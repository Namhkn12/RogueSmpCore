package com.roguesmp.quest.board;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.event.QuestCompletedEvent;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.inbox.InboxEntry;
import com.roguesmp.player.inbox.InboxManager;
import com.roguesmp.player.inbox.InboxSource;
import com.roguesmp.quest.reward.QuestReward;
import com.roguesmp.quest.Quest;
import com.roguesmp.quest.QuestManager;
import com.roguesmp.quest.QuestProgress;
import com.roguesmp.quest.data.PlayerQuestData;
import com.roguesmp.quest.holder.PlayerHolder;
import com.roguesmp.registry.Registries;
import com.roguesmp.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class QuestBoardManager {

    private static final long REFRESH_INTERVAL_TICKS = 20L * 60;
    private static QuestBoardManager INSTANCE;

    public enum AcceptResult {
        ACCEPTED("<green>Đã nhận nhiệm vụ!"),
        NOT_ON_BOARD("<red>Nhiệm vụ này không còn trên bảng."),
        ALREADY_ACCEPTED("<red>Bạn đã nhận nhiệm vụ này rồi."),
        LIMIT_REACHED("<red>Bạn đã nhận tối đa số nhiệm vụ cho phép."),
        REQUIREMENTS_NOT_MET("<red>Bạn chưa đủ điều kiện để nhận nhiệm vụ này."),
        UNAVAILABLE("<red>Không thể tải dữ liệu nhiệm vụ của bạn lúc này.");

        private final String message;

        AcceptResult(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    private final QuestManager questManager;
    private final QuestBoardSettings settings;

    private QuestBoardManager(RogueSmpCore plugin, QuestManager questManager) {
        this.questManager = questManager;
        this.settings = QuestBoardSettings.load(plugin.getDataFolder().toPath().resolve("quest_board.json"));
    }

    public QuestBoardSettings getSettings() {
        return settings;
    }

    public List<Quest> getBoardQuests() {
        return Registries.QUEST.getAll().values().stream().filter(quest -> quest.getBoard() != null).toList();
    }

    public void refresh(Player player) {
        PlayerQuestData data = questManager.getPlayerQuestData(player.getUniqueId());
        if (data != null && refresh(data, player)) {
            player.sendMessage(Utils.fromString("<green><bold>[!]</bold> Bảng nhiệm vụ đã được làm mới!"));
        }
    }

    /**
     * Drops entries whose cycle ended, with their unfinished progress, then fills the free slots.
     *
     * @return whether any entry was dropped for a reset
     */
    private boolean refresh(PlayerQuestData data, Player player) {
        PlayerBoard board = data.getBoard();
        long now = System.currentTimeMillis();
        boolean reset = false;
        List<QuestProgress> doneButNotCompleted = new ArrayList<>();
        for (BoardEntry entry : List.copyOf(board.getEntries())) {
            Quest quest = Registries.QUEST.get(entry.questId());
            if (quest != null && quest.getBoard() != null && quest.cycleAt(now) == entry.cycle()) {
                QuestProgress progress = data.getQuestProgress(entry.questId());
                if (progress != null && progress.isCompleted()) doneButNotCompleted.add(progress);
                continue;
            }
            board.removeEntry(entry);
            QuestProgress progress = data.getQuestProgress(entry.questId());
            if (progress != null && !progress.isCompleted()) data.removeQuestProgress(entry.questId());
            data.setDirty(true);
            reset = true;
        }
        for (QuestProgress progress : List.copyOf(data.getQuestProgresses().values())) {
            String questId = progress.getQuest().getId();
            if (progress.getQuest().getBoard() == null || progress.isCompleted() || board.getEntry(questId) != null) continue;
            data.removeQuestProgress(questId);
        }
        if (board.removeCompletedIf(completed -> {
            Quest quest = Registries.QUEST.get(completed.getKey());
            return quest == null || quest.cycleAt(now) != completed.getValue();
        })) data.setDirty(true);
        fill(data, player, now);
        doneButNotCompleted.forEach(progress -> questManager.completeLoaded(data.getHolder(), progress));
        return reset;
    }

    private void fill(PlayerQuestData data, @Nullable Player player, long now) {
        PlayerBoard board = data.getBoard();
        SmpPlayer smpPlayer = player == null ? null : PlayerManager.getInstance().getSmpPlayer(player);
        List<Quest> candidates = new ArrayList<>();
        for (Quest quest : getBoardQuests()) {
            long cycle = quest.cycleAt(now);
            if (board.getEntry(quest.getId()) != null || board.isCompleted(quest.getId(), cycle)) continue;
            if (data.getQuestProgress(quest.getId()) != null) continue;
            if (smpPlayer != null && !quest.getRequirements().stream().allMatch(requirement -> requirement.test(smpPlayer))) continue;
            candidates.add(quest);
        }

        while (board.getEntries().size() < settings.size() && !candidates.isEmpty()) {
            Quest picked = pickWeighted(candidates);
            candidates.remove(picked);
            board.addEntry(new BoardEntry(picked.getId(), picked.cycleAt(now)));
            data.setDirty(true);
        }
    }

    private static Quest pickWeighted(List<Quest> candidates) {
        int total = candidates.stream().mapToInt(QuestBoardManager::weight).sum();
        int roll = Utils.RANDOM.nextInt(total);
        for (Quest quest : candidates) {
            roll -= weight(quest);
            if (roll < 0) return quest;
        }
        return candidates.getLast();
    }

    private static int weight(Quest quest) {
        return Math.max(1, quest.getBoard().weight());
    }

    public int getLevel(PlayerQuestData data) {
        return settings.level().levelOf(data.getExp());
    }

    public int getAcceptLimit(PlayerQuestData data) {
        return settings.level().acceptLimit(getLevel(data));
    }

    public void giveExp(Player player, long amount) {
        PlayerQuestData data = questManager.getPlayerQuestData(player.getUniqueId());
        if (data == null || amount <= 0) return;
        int before = getLevel(data);
        data.addExp(amount);
        questManager.getQuestDataManager().saveAsync(data);
        int after = getLevel(data);
        player.sendMessage(Utils.fromString("<aqua>+" + amount + " kinh nghiệm Mạo hiểm"));
        if (after <= before) return;
        player.sendMessage(Utils.fromString("<gold><bold>LÊN CẤP!</bold></gold> <yellow>Cấp Mạo hiểm <gold>" + after + "</gold>. Có thể nhận <gold>" + settings.level().acceptLimit(after) + "</gold> nhiệm vụ cùng lúc."));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
        for (int level = before + 1; level <= after; level++) {
            List<QuestReward> rewards = settings.level().rewardsAt(level);
            if (rewards.isEmpty()) continue;
            InboxSource source = new InboxSource("Cấp Mạo hiểm", "Phần thưởng cấp " + level, Material.NETHER_STAR.key());
            InboxManager.getInstance().send(player.getUniqueId(), InboxEntry.of(source, rewards));
            player.sendMessage(Utils.fromString("<yellow>Phần thưởng cấp <gold>" + level + "</gold> đã được gửi vào hộp thư."));
        }
    }

    public long getAcceptedCount(PlayerQuestData data) {
        return data.getBoard().getEntries().stream().filter(entry -> data.getQuestProgress(entry.questId()) != null).count();
    }

    public AcceptResult accept(Player player, Quest quest) {
        PlayerQuestData data = questManager.getPlayerQuestData(player.getUniqueId());
        SmpPlayer smpPlayer = PlayerManager.getInstance().getSmpPlayer(player);
        if (data == null || smpPlayer == null) return AcceptResult.UNAVAILABLE;
        BoardEntry entry = data.getBoard().getEntry(quest.getId());
        if (entry == null || quest.cycleAt(System.currentTimeMillis()) != entry.cycle()) return AcceptResult.NOT_ON_BOARD;
        if (data.getQuestProgress(quest.getId()) != null) return AcceptResult.ALREADY_ACCEPTED;
        if (getAcceptedCount(data) >= getAcceptLimit(data)) return AcceptResult.LIMIT_REACHED;
        if (!quest.getRequirements().stream().allMatch(requirement -> requirement.test(smpPlayer))) return AcceptResult.REQUIREMENTS_NOT_MET;
        return questManager.startQuest(data.getHolder(), quest) ? AcceptResult.ACCEPTED : AcceptResult.UNAVAILABLE;
    }

    /**
     * Drops the player's progress on a board quest; the quest stays on the board to be accepted again.
     */
    public boolean abandon(Player player, Quest quest) {
        PlayerQuestData data = questManager.getPlayerQuestData(player.getUniqueId());
        if (data == null || data.getBoard().getEntry(quest.getId()) == null) return false;
        QuestProgress progress = data.getQuestProgress(quest.getId());
        if (progress == null || progress.isCompleted()) return false;
        data.removeQuestProgress(quest.getId());
        return true;
    }

    public void onQuestCompleted(QuestCompletedEvent event) {
        if (!(event.getHolder() instanceof PlayerHolder holder)) return;
        PlayerQuestData data = questManager.getPlayerQuestData(holder.id());
        String questId = event.getProgress().getQuest().getId();
        BoardEntry entry = data == null ? null : data.getBoard().getEntry(questId);
        if (entry == null) return;

        data.getBoard().removeEntry(entry);
        data.getBoard().markCompleted(questId, entry.cycle());
        data.removeQuestProgress(questId);
        fill(data, Bukkit.getPlayer(holder.id()), System.currentTimeMillis());
    }

    private void refreshOnline() {
        Bukkit.getOnlinePlayers().forEach(this::refresh);
    }

    public static void init(RogueSmpCore plugin, QuestManager questManager) {
        INSTANCE = new QuestBoardManager(plugin, questManager);
        Bukkit.getScheduler().runTaskTimer(plugin, INSTANCE::refreshOnline, REFRESH_INTERVAL_TICKS, REFRESH_INTERVAL_TICKS);
    }

    public static QuestBoardManager getInstance() {
        if (INSTANCE == null) throw new IllegalStateException("QuestBoardManager is null!");
        return INSTANCE;
    }
}
