package com.roguesmp.quest;

import com.roguesmp.quest.data.GroupQuestData;
import com.roguesmp.quest.data.PlayerQuestData;
import com.roguesmp.quest.data.QuestDataManager;
import com.roguesmp.quest.data.QuestLog;
import com.roguesmp.quest.holder.GroupHolder;
import com.roguesmp.quest.holder.PlayerHolder;
import com.roguesmp.quest.holder.QuestHolder;
import com.roguesmp.quest.objective.ObjectiveProgress;
import com.roguesmp.quest.objective.QuestObjective;
import com.roguesmp.gate.requirement.Requirement;
import com.roguesmp.RogueSmpCore;
import com.roguesmp.event.QuestCompletedEvent;
import com.roguesmp.event.QuestProgressEvent;
import com.roguesmp.event.QuestStartedEvent;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.inbox.InboxEntry;
import com.roguesmp.player.inbox.InboxManager;
import com.roguesmp.player.inbox.InboxSource;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiPredicate;

public class QuestManager {

    private static QuestManager INSTANCE;

    private final RogueSmpCore plugin;
    private final QuestDataManager questDataManager;

    public QuestManager(RogueSmpCore plugin) {
        this.plugin = plugin;
        this.questDataManager = new QuestDataManager(plugin);
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

    /**
     * Hands the player's items in to given quest, taking only what its unfinished objectives still need.
     *
     * @return whether any item was taken
     */
    public boolean submitItems(Player player, Quest quest) {
        PlayerQuestData data = getPlayerQuestData(player.getUniqueId());
        SmpPlayer smpPlayer = PlayerManager.getInstance().getSmpPlayer(player);
        QuestProgress progress = data == null ? null : data.getQuestProgress(quest.getId());
        if (progress == null || smpPlayer == null) return false;
        return advance(data, progress, (objective, objectiveProgress) -> objective.onSubmitItems(smpPlayer, objectiveProgress), player, System.currentTimeMillis());
    }

    // Quest conditions only apply to progress from a player's own action.
    private void handleProgress(QuestLog log, BiPredicate<QuestObjective, ObjectiveProgress> objectiveConsumer, @Nullable Player player) {
        long now = System.currentTimeMillis();
        for (QuestProgress questProgress : List.copyOf(log.getQuestProgresses().values())) {
            advance(log, questProgress, objectiveConsumer, player, now);
        }
    }

    private boolean advance(QuestLog log, QuestProgress questProgress, BiPredicate<QuestObjective, ObjectiveProgress> objectiveConsumer, @Nullable Player player, long now) {
        if (questProgress.isCompleted() || questProgress.isExpired(now)) return false;

        Quest quest = questProgress.getQuest();
        if (player != null && !quest.canProgress(log.getHolder(), player)) return false;

        boolean changed = false;
        for (Map.Entry<String, QuestObjective> entry : quest.getObjectives().entrySet()) {
            String objectiveId = entry.getKey();
            if (questProgress.isObjectiveCompleted(objectiveId)) continue;

            if (!questProgress.updateObjectiveProgress(objectiveId, objectiveProgress -> objectiveConsumer.test(entry.getValue(), objectiveProgress))) continue;
            changed = true;
            log.setDirty(true);
            new QuestProgressEvent(log.getHolder(), questProgress, objectiveId).callEvent();

            if (questProgress.isCompleted()) completeQuest(log, questProgress);
        }
        return changed;
    }

    /**
     * Completes a quest whose objectives were already all done when it loaded but never went through completion,
     * for example after its goal range was lowered.
     */
    public void completeLoaded(QuestHolder holder, QuestProgress progress) {
        QuestLog log = getQuestLog(holder);
        if (log != null && progress.isCompleted() && log.getQuestProgress(progress.getQuest().getId()) == progress) completeQuest(log, progress);
    }

    private void completeQuest(QuestLog log, QuestProgress progress) {
        Quest quest = progress.getQuest();
        log.getHolder().audience().forEach(member -> sendQuestDoneNotification(member, quest));
        if (log instanceof GroupQuestData group) {
            InboxSource source = new InboxSource("Nhiệm vụ nhóm", displayName(quest), quest.getIcon());
            progress.getEligible().forEach(member -> giveOrDeliver(member, source, quest));
            questDataManager.saveGroupAsync(group);
        } else {
            String origin = quest.getBoard() != null ? "Bảng nhiệm vụ" : "Nhiệm vụ";
            giveOrDeliver(log.getHolder().id(), new InboxSource(origin, displayName(quest), quest.getIcon()), quest);
        }
        new QuestCompletedEvent(log.getHolder(), progress).callEvent();
        if (log instanceof PlayerQuestData data) questDataManager.saveAsync(data);
    }

    private static String displayName(Quest quest) {
        return quest.getName() != null ? quest.getName() : quest.getId();
    }

    /**
     * Gives the rewards right away for an auto-claimed quest when the player is online, otherwise puts them in the inbox.
     */
    private void giveOrDeliver(UUID playerId, InboxSource source, Quest quest) {
        Player player = Bukkit.getPlayer(playerId);
        SmpPlayer smpPlayer = player == null ? null : PlayerManager.getInstance().getSmpPlayer(player);
        if (quest.isAutoClaimRewards() && smpPlayer != null) {
            quest.getRewards().forEach(reward -> reward.giveReward(smpPlayer));
            return;
        }
        InboxManager.getInstance().send(playerId, InboxEntry.of(source, quest.getRewards()));
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
     * Restarts a holder's quest from zero, for seasonal resets or re-taking a repeatable quest. Rewards already in
     * inboxes are kept.
     *
     * @return false if the holder has no such quest
     */
    public boolean resetQuestProgress(QuestHolder holder, Quest quest) {
        QuestLog log = getQuestLog(holder);
        QuestProgress progress = log == null ? null : log.getQuestProgress(quest.getId());
        if (progress == null) return false;

        progress.resetProgresses();
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

         for (Requirement requirement : quest.getRequirements()) {
             if (!requirement.test(smpPlayer)) return false;
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
    }

    public static QuestManager getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("QuestManager is null!");
        }
        return INSTANCE;
    }
}
