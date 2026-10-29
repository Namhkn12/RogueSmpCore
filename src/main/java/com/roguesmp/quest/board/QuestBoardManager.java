package com.roguesmp.quest.board;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.event.AdventureRankChangeEvent;
import com.roguesmp.event.QuestCompletedEvent;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.quest.Quest;
import com.roguesmp.quest.QuestManager;
import com.roguesmp.quest.QuestProgress;
import com.roguesmp.quest.data.PlayerQuestData;
import com.roguesmp.quest.data.QuestDataManager.RankEntry;
import com.roguesmp.quest.holder.PlayerHolder;
import com.roguesmp.registry.Registries;
import com.roguesmp.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.CompletableFuture;
import java.util.UUID;
import java.util.Map;

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
    private final AdventureLeaderboard leaderboard;
    private final Executor mainThread;
    private final Map<UUID, AdventureStatus> statuses = new ConcurrentHashMap<>();
    private final Map<UUID, RankEntry> rankings = new ConcurrentHashMap<>();

    /**
     * @param position 1-based position in the daily ranking, or -1 if unranked
     */
    public record AdventureStatus(int level, AdventureRank rank, int position) {
        public static final AdventureStatus NONE = new AdventureStatus(1, AdventureRank.NONE, -1);
    }

    private QuestBoardManager(RogueSmpCore plugin, QuestManager questManager) {
        this.questManager = questManager;
        this.settings = QuestBoardSettings.load(plugin.getDataFolder().toPath().resolve("quest_board.json"));
        this.leaderboard = new AdventureLeaderboard(plugin, questManager.getQuestDataManager(), settings);
        this.mainThread = Bukkit.getScheduler().getMainThreadExecutor(plugin);
    }

    public AdventureLeaderboard getLeaderboard() {
        return leaderboard;
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

    public int countClaimableRewards(PlayerQuestData data) {
        int level = getLevel(data);
        return (int) settings.level().rewards().headMap(level, true).keySet().stream().filter(reward -> canClaimReward(data, reward)).count();
    }

    public int getLevel(PlayerQuestData data) {
        return settings.level().levelOf(data.getExp());
    }

    public boolean isRewardClaimed(PlayerQuestData data, int level) {
        return claimedRewards(data).contains(level);
    }

    public boolean canClaimReward(PlayerQuestData data, int level) {
        return level <= getLevel(data) && !settings.level().rewardsAt(level).isEmpty() && !isRewardClaimed(data, level);
    }

    public boolean claimReward(Player player, int level) {
        PlayerQuestData data = questManager.getPlayerQuestData(player.getUniqueId());
        SmpPlayer smpPlayer = PlayerManager.getInstance().getSmpPlayer(player);
        if (data == null || smpPlayer == null || !canClaimReward(data, level)) return false;
        data.markRewardClaimed(level);
        questManager.getQuestDataManager().saveAsync(data);
        settings.level().rewardsAt(level).forEach(reward -> reward.giveReward(smpPlayer));
        return true;
    }

    private Set<Integer> claimedRewards(PlayerQuestData data) {
        if (data.getClaimedRewards() == null) {
            int level = getLevel(data);
            data.setClaimedRewards(settings.level().rewards().headMap(level, true).keySet());
        }
        return data.getClaimedRewards();
    }

    public AdventureRank getRank(PlayerQuestData data) {
        int level = getLevel(data);
        TopRankSettings topRank = settings.topRank();
        if (topRank != null && level >= topRank.minLevel() && leaderboard.holdsTopRank(rankings.get(data.getUuid()))) return topRank.rank();
        return settings.level().rankAt(level);
    }

    public AdventureStatus getStatus(UUID uuid) {
        return statuses.getOrDefault(uuid, AdventureStatus.NONE);
    }

    public void syncStatus(Player player) {
        PlayerQuestData data = questManager.getPlayerQuestData(player.getUniqueId());
        if (data == null) return;
        RankEntry ranking = rankings.get(player.getUniqueId());
        AdventureStatus next = new AdventureStatus(getLevel(data), getRank(data), ranking == null ? -1 : ranking.position());
        AdventureStatus previous = statuses.put(player.getUniqueId(), next);
        if (previous != null && !previous.rank().equals(next.rank())) {
            Bukkit.getPluginManager().callEvent(new AdventureRankChangeEvent(player, previous.rank(), next.rank()));
        }
    }

    /**
     * Pre-login thread: loads the player's daily ranking ahead of {@link #syncStatus} on join.
     */
    @Blocking
    public void loadRanking(UUID uuid) {
        RankEntry entry = leaderboard.rankingOf(uuid).join();
        if (entry != null) rankings.put(uuid, entry);
        else rankings.remove(uuid);
    }

    public void clearStatus(UUID uuid) {
        statuses.remove(uuid);
        rankings.remove(uuid);
    }

    /**
     * Re-ranks, then reloads the ranking of every online player and syncs their status. Completes on the main thread.
     */
    public CompletableFuture<Void> refreshTopRank() {
        return leaderboard.refresh().thenCompose(ignored -> CompletableFuture.allOf(Bukkit.getOnlinePlayers().stream()
                .map(player -> leaderboard.rankingOf(player.getUniqueId()).thenAcceptAsync(entry -> {
                    if (!player.isOnline()) return;
                    if (entry != null) rankings.put(player.getUniqueId(), entry);
                    else rankings.remove(player.getUniqueId());
                    syncStatus(player);
                }, mainThread))
                .toArray(CompletableFuture[]::new)));
    }

    /**
     * Admin override: no level rewards are granted, already reached levels just become claimable.
     */
    public void setExp(Player player, long exp) {
        PlayerQuestData data = questManager.getPlayerQuestData(player.getUniqueId());
        if (data == null) return;
        claimedRewards(data);
        data.setExp(exp);
        questManager.getQuestDataManager().saveAsync(data);
        syncStatus(player);
    }

    public int getAcceptLimit(PlayerQuestData data) {
        return settings.level().acceptLimit(getLevel(data));
    }

    public void giveExp(Player player, long amount) {
        PlayerQuestData data = questManager.getPlayerQuestData(player.getUniqueId());
        if (data == null || amount <= 0) return;
        claimedRewards(data);
        int before = getLevel(data);
        data.addExp(amount);
        questManager.getQuestDataManager().saveAsync(data);
        int after = getLevel(data);
        player.sendMessage(Utils.fromString("<aqua>+" + amount + " kinh nghiệm Mạo hiểm"));
        syncStatus(player);
        if (after <= before) return;
        player.sendMessage(Utils.fromString("<gold><bold>LÊN CẤP!</bold></gold> <yellow>Cấp Mạo hiểm <gold>" + after + "</gold>. Có thể nhận <gold>" + settings.level().acceptLimit(after) + "</gold> nhiệm vụ cùng lúc."));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
        boolean hasReward = settings.level().rewards().subMap(before, false, after, true).values().stream().anyMatch(rewards -> !rewards.isEmpty());
        if (hasReward) player.sendMessage(Utils.fromString("<yellow>Có phần thưởng cấp mới! Dùng <gold>/quest level</gold> để nhận."));
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
        INSTANCE.leaderboard.refreshIfEmpty();
        Bukkit.getScheduler().runTaskTimer(plugin, INSTANCE::refreshOnline, REFRESH_INTERVAL_TICKS, REFRESH_INTERVAL_TICKS);
    }

    public static QuestBoardManager getInstance() {
        if (INSTANCE == null) throw new IllegalStateException("QuestBoardManager is null!");
        return INSTANCE;
    }
}
