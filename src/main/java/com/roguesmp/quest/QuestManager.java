package com.roguesmp.quest;

import com.roguesmp.quest.data.GroupQuestData;
import com.roguesmp.quest.data.PendingQuestReward;
import com.roguesmp.quest.data.PlayerQuestData;
import com.roguesmp.quest.data.QuestDataManager;
import com.roguesmp.quest.data.QuestLog;
import com.roguesmp.quest.holder.GroupHolder;
import com.roguesmp.quest.holder.PlayerHolder;
import com.roguesmp.quest.holder.QuestHolder;
import com.roguesmp.quest.objective.ObjectiveProgress;
import com.roguesmp.quest.objective.QuestObjective;
import com.roguesmp.quest.requirement.QuestRequirement;
import com.roguesmp.RogueSmpCore;
import com.roguesmp.event.QuestCompletedEvent;
import com.roguesmp.event.QuestProgressEvent;
import com.roguesmp.event.QuestStartedEvent;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.quest.daily.DailyQuestManager;
import com.roguesmp.registry.Registries;
import com.roguesmp.registry.Registry;
import com.roguesmp.gui.quest.RewardInboxGui;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiPredicate;

public class QuestManager {

    private static QuestManager INSTANCE;
    private static final long REWARD_EXPIRY_INTERVAL_TICKS = 20L * 60 * 60;

    private final RogueSmpCore plugin;
    private final QuestDataManager questDataManager;
    private final Registry<Quest> questRegistry;
    private final DailyQuestManager dailyQuestManager;

    public QuestManager(RogueSmpCore plugin) {
        this.plugin = plugin;
        this.questDataManager = new QuestDataManager(plugin);
        this.questRegistry = Registries.QUEST;
        this.dailyQuestManager = new DailyQuestManager(this);
    }

    public @Nullable PlayerQuestData getPlayerQuestData(UUID uuid) {
        return questDataManager.getCachedData(uuid);
    }

    /**
     * Assigns a specific quest to a player if they don't already have progress on it.
     */
    public boolean assignQuest(Player player, Quest quest) {
        return startQuest(new PlayerHolder(player.getUniqueId()), quest);
    }

    public @Nullable QuestLog getQuestLog(QuestHolder holder) {
        if (holder instanceof GroupHolder group) return questDataManager.getCachedGroupData(group.id());
        return questDataManager.getCachedData(holder.id());
    }

    /**
     * @return the quests that were started, skipping those the holder already has
     */
    public List<Quest> startQuests(QuestHolder holder, Collection<Quest> quests) {
        return quests.stream().filter(quest -> startQuest(holder, quest)).toList();
    }

    /**
     * Starts a quest for any holder: a single player, or a group whose members all advance and see it. A group must
     * be bound first, so wait for {@link #bindGroup}.
     *
     * @return false if the holder has no loaded quest log or already has progress on this quest
     */
    public boolean startQuest(QuestHolder holder, Quest quest) {
        QuestLog log = getQuestLog(holder);
        if (log == null) {
            if (holder instanceof GroupHolder) RogueSmpCore.LOGGER.warn("Cannot start quest {} on group {}: it is not bound yet", quest.getId(), holder.id());
            return false;
        }
        if (log.getQuestProgresses().containsKey(quest.getId())) return false;
        QuestProgress progress = new QuestProgress(quest);
        log.setQuestProgress(quest.getId(), progress);
        new QuestStartedEvent(holder, progress).callEvent();
        return true;
    }

    /**
     * Loads or creates the group's quest log and attaches its members. A group must be bound before quests start on it.
     */
    public CompletableFuture<GroupQuestData> bindGroup(GroupHolder holder) {
        return questDataManager.bindGroup(holder);
    }

    /**
     * Ends a group: unfinished quests are dropped, unclaimed rewards move to each eligible member.
     */
    public void endGroup(GroupHolder holder) {
        questDataManager.endGroup(holder.id());
    }

    /**
     * Startup only: ends every saved group of {@code owner} that is not live any more.
     *
     * @return how many groups were ended
     */
    public int sweepGroups(String owner, Set<UUID> liveGroupIds) {
        return questDataManager.sweepGroups(owner, liveGroupIds);
    }

    /**
     * Lets the player claim the rewards of every active quest of the group once it completes. Group rewards go only to
     * eligible players, added only through here by the group's owner. Completed quests are archived.
     */
    public void addEligible(GroupHolder holder, UUID playerId) {
        GroupQuestData group = questDataManager.getCachedGroupData(holder.id());
        if (group == null) return;
        group.getQuestProgresses().keySet().forEach(questId -> addEligible(group, questId, playerId));
    }

    public void addEligible(GroupHolder holder, String questId, UUID playerId) {
        GroupQuestData group = questDataManager.getCachedGroupData(holder.id());
        if (group != null) addEligible(group, questId, playerId);
    }

    private void addEligible(GroupQuestData group, String questId, UUID playerId) {
        QuestProgress progress = group.getQuestProgress(questId);
        if (progress != null && progress.addEligible(playerId)) group.setDirty(true);
    }

    public void removeEligible(GroupHolder holder, UUID playerId) {
        GroupQuestData group = questDataManager.getCachedGroupData(holder.id());
        if (group == null) return;
        group.getQuestProgresses().keySet().forEach(questId -> removeEligible(holder, questId, playerId));
    }

    public void removeEligible(GroupHolder holder, String questId, UUID playerId) {
        GroupQuestData group = questDataManager.getCachedGroupData(holder.id());
        QuestProgress progress = group == null ? null : group.getQuestProgress(questId);
        if (progress != null && progress.removeEligible(playerId)) group.setDirty(true);
    }

    /**
     * Advances the player's own quests and those of every group they can progress.
     *
     * @param objectiveConsumer returns whether it changed the objective's progress
     */
    public void handleProgress(Player player, BiPredicate<QuestObjective, ObjectiveProgress> objectiveConsumer) {
        PlayerQuestData playerQuestData = getPlayerQuestData(player.getUniqueId());
        if (playerQuestData != null) handleProgress(playerQuestData, objectiveConsumer, player);
        for (GroupQuestData group : List.copyOf(questDataManager.getCachedGroupQuestData())) {
            if (group.getHolder().canProgress(player)) handleProgress(group, objectiveConsumer, player);
        }
    }

    /**
     * Advances one holder's quests directly, for progress that belongs to the holder rather than to a player's action.
     *
     * @param objectiveConsumer returns whether it changed the objective's progress
     */
    public void handleProgress(QuestHolder holder, BiPredicate<QuestObjective, ObjectiveProgress> objectiveConsumer) {
        QuestLog log = getQuestLog(holder);
        if (log != null) handleProgress(log, objectiveConsumer, null);
    }

    public void signal(QuestHolder holder, String signal, int amount) {
        handleProgress(holder, (objective, progress) -> objective.onSignal(signal, amount, progress));
    }

    public void signal(Player player, String signal, int amount) {
        handleProgress(player, (objective, progress) -> objective.onSignal(signal, amount, progress));
    }

    // Quest conditions only apply to progress from a player's own action.
    private void handleProgress(QuestLog log, BiPredicate<QuestObjective, ObjectiveProgress> objectiveConsumer, @Nullable Player player) {
        for (QuestProgress questProgress : List.copyOf(log.getQuestProgresses().values())) {
            if (questProgress.isCompleted()) continue;

            Quest quest = questProgress.getQuest();
            if (player != null && !quest.canProgress(log.getHolder(), player)) continue;

            quest.getObjectives().forEach((objectiveId, objective) -> {
                if (questProgress.isObjectiveCompleted(objectiveId)) return;

                if (!questProgress.updateObjectiveProgress(objectiveId, objectiveProgress -> objectiveConsumer.test(objective, objectiveProgress))) return;
                log.setDirty(true);
                new QuestProgressEvent(log.getHolder(), questProgress, objectiveId).callEvent();

                if (questProgress.isCompleted()) completeQuest(log, questProgress);
            });
        }
    }

    private void completeQuest(QuestLog log, QuestProgress progress) {
        Quest quest = progress.getQuest();
        UUID source = log.getHolder().id();
        log.getHolder().audience().forEach(member -> sendQuestDoneNotification(member, quest));
        if (log instanceof GroupQuestData group) {
            progress.getEligible().forEach(member -> giveOrDeliver(member, source, progress));
            questDataManager.saveGroupAsync(group);
        } else {
            giveOrDeliver(source, source, progress);
        }
        new QuestCompletedEvent(log.getHolder(), progress).callEvent();
    }

    /**
     * Gives the rewards right away for an auto-claimed quest when the player is online, otherwise puts them in the inbox.
     */
    private void giveOrDeliver(UUID playerId, UUID source, QuestProgress progress) {
        Quest quest = progress.getQuest();
        Player player = Bukkit.getPlayer(playerId);
        PlayerQuestData data = getPlayerQuestData(playerId);
        if (quest.isAutoClaimRewards() && player != null && data != null && giveRewards(player, quest)) {
            if (source.equals(playerId)) dailyQuestManager.onRewardClaimed(data, quest);
            saveQuestDataImmediately(data);
            return;
        }
        questDataManager.deliver(playerId, PendingQuestReward.of(source, progress));
        if (player != null) sendClaimPrompt(player, source, quest);
    }

    /**
     * Claims a reward from the player's inbox.
     *
     * @param source the holder that completed the quest: the player themselves or a group
     */
    public boolean claimReward(Player player, UUID source, String questId) {
        PlayerQuestData data = getPlayerQuestData(player.getUniqueId());
        Quest quest = questRegistry.get(questId);
        if (data == null || quest == null || !data.hasPendingQuestReward(source, questId)) {
            player.sendMessage(Utils.fromString("<red>Không có phần thưởng nào để nhận!"));
            return false;
        }
        if (!giveRewards(player, quest)) return false;
        data.removePendingQuestReward(source, questId);
        if (source.equals(player.getUniqueId())) dailyQuestManager.onRewardClaimed(data, quest);
        saveQuestDataImmediately(data);
        return true;
    }

    public boolean hasClaimableReward(Player player, String questId) {
        PlayerQuestData data = getPlayerQuestData(player.getUniqueId());
        return data != null && data.hasPendingQuestReward(player.getUniqueId(), questId);
    }

    public void notifyPendingQuestRewards(Player player) {
        int pending = getPendingRewards(player).size();
        if (pending == 0) return;
        Component openInbox = Utils.fromString("<gold>[Mở hộp thư]</gold>").clickEvent(ClickEvent.callback(audience -> {
            if (!(audience instanceof Player viewer)) return;
            PlayerQuestData data = getPlayerQuestData(viewer.getUniqueId());
            if (data != null) new RewardInboxGui(viewer, data).showInventory(viewer);
        }));
        player.sendMessage(Utils.fromString("<yellow>Bạn có <gold>" + pending + "</gold> phần thưởng nhiệm vụ chưa nhận. ").append(openInbox));
    }

    public List<PendingQuestReward> getPendingRewards(Player player) {
        PlayerQuestData data = getPlayerQuestData(player.getUniqueId());
        return data == null ? List.of() : data.getPendingQuestRewards();
    }

    private boolean giveRewards(Player member, Quest quest) {
        SmpPlayer smpPlayer = PlayerManager.getInstance().getSmpPlayer(member);
        if (smpPlayer == null) return false;
        quest.getRewards().forEach(reward -> reward.giveReward(smpPlayer));
        return true;
    }

    public void sendClaimPrompt(Player member, UUID source, Quest quest) {
        String name = quest.getName() != null ? quest.getName() : quest.getId();
        String label = source.equals(member.getUniqueId()) ? "Phần thưởng: " : "Phần thưởng nhóm: ";
        Component claim = Utils.fromString("<green>[Nhận thưởng]</green>").clickEvent(ClickEvent.callback(audience -> {
            if (audience instanceof Player viewer && claimReward(viewer, source, quest.getId())) {
                viewer.sendMessage(Utils.fromString("<green>Đã nhận thưởng: </green>" + name));
            }
        }));
        member.sendMessage(Utils.fromString("<gold>" + label + "</gold>" + name + " ").append(claim));
    }

    public void sendQuestDoneNotification(Player player, Quest quest) {
        Component questName;
        if (quest.getName() != null) {
            questName = Utils.fromString(quest.getName());
        } else questName = Component.text("NO_NAME");
        player.sendMessage(Component.text("Bạn đã hoàn thành nhiệm vụ ", NamedTextColor.GREEN).append(questName));
    }

    /**
     * Instantly completes a holder's quest, through the normal completion with its rewards. Useful for administrative
     * debugging or skip tokens.
     *
     * @return false if the holder has no such unfinished quest
     */
    public boolean forceCompleteQuest(QuestHolder holder, Quest quest) {
        QuestLog log = getQuestLog(holder);
        QuestProgress progress = log == null ? null : log.getQuestProgress(quest.getId());
        if (progress == null || progress.isCompleted()) return false;

        progress.forceComplete();
        log.setDirty(true);
        completeQuest(log, progress);
        return true;
    }

    /**
     * Persists a player's quest data right away instead of waiting for the periodic
     * autosave/quit save - used whenever a quest transitions to a completed state, so a
     * crash shortly after completion doesn't lose it.
     */
    private void saveQuestDataImmediately(PlayerQuestData data) {
        questDataManager.saveAsync(data);
    }

    /**
     * Restarts a holder's quest from zero, for seasonal resets or re-taking a repeatable quest. A player's unclaimed
     * reward from the earlier completion is dropped; group rewards already in members' inboxes are kept.
     *
     * @return false if the holder has no such quest
     */
    public boolean resetQuestProgress(QuestHolder holder, Quest quest) {
        QuestLog log = getQuestLog(holder);
        QuestProgress progress = log == null ? null : log.getQuestProgress(quest.getId());
        if (progress == null) return false;

        progress.resetProgresses();
        if (log instanceof PlayerQuestData data) data.removePendingQuestReward(holder.id(), quest.getId());
        log.setDirty(true);
        return true;
    }

    /**
     * Evaluates whether a player satisfies all requirement
     * to unlock or initiate a specific quest.
     *
     * @return true if the player meets all conditions
     */
    public boolean canAcceptQuest(Player player, Quest quest) {
        SmpPlayer smpPlayer = PlayerManager.getInstance().getSmpPlayer(player);
        if (smpPlayer == null) return false;

        PlayerQuestData data = getPlayerQuestData(player.getUniqueId());
        if (data == null) return false;

        // 1. Is it already active or completed in their file?
        if (data.getQuestProgresses().containsKey(quest.getId())) {
            return false;
        }

         for (QuestRequirement requirement : quest.getRequirements()) {
             if (!requirement.canMeetRequirement(smpPlayer)) return false;
         }

        return true;
    }

    /**
     * Retrieves all active, uncompleted quest tracking sessions for a player.
     */
    public @NotNull List<QuestProgress> getActiveQuests(Player player) {
        PlayerQuestData data = getPlayerQuestData(player.getUniqueId());
        if (data == null) return Collections.emptyList();

        return data.getQuestProgresses().values().stream()
                .filter(progress -> !progress.isCompleted())
                .toList();
    }

    /**
     * Retrieves all completed quest profiles for a player.
     */
    public @NotNull List<QuestProgress> getCompletedQuests(Player player) {
        PlayerQuestData data = getPlayerQuestData(player.getUniqueId());
        if (data == null) return Collections.emptyList();

        return data.getQuestProgresses().values().stream()
                .filter(QuestProgress::isCompleted)
                .toList();
    }

    public QuestDataManager getQuestDataManager() {
        return questDataManager;
    }

    public DailyQuestManager getDailyQuestManager() {
        return dailyQuestManager;
    }

    public void onKillEntity(EntityDeathEvent event) {
        if (!(event.getDamageSource().getCausingEntity() instanceof Player player)) return;

        handleProgress(player, (questObjective, objectiveProgress) -> questObjective.onKillEntity(event, objectiveProgress));
    }

    public void onDisable() {
        questDataManager.saveAllData();
        questDataManager.close();
    }

    public static void init(RogueSmpCore plugin) {
        INSTANCE = new QuestManager(plugin);
        Bukkit.getScheduler().runTaskTimer(plugin, () -> INSTANCE.questDataManager.expireRewards(), REWARD_EXPIRY_INTERVAL_TICKS, REWARD_EXPIRY_INTERVAL_TICKS);
    }

    public static QuestManager getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("QuestManager is null!");
        }
        return INSTANCE;
    }
}
