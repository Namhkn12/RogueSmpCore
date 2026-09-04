package com.roguesmp.quest.daily;

import com.roguesmp.registry.Registries;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.quest.*;
import com.roguesmp.tag.SmpTag;
import com.roguesmp.utils.DateUtils;
import com.roguesmp.utils.Utils;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class DailyQuestManager {

    public static final int QUESTS_PER_TIER = 3;

    private static final String EASY_TAG_ID = "daily_easy_quest";
    private static final String MEDIUM_TAG_ID = "daily_medium_quest";
    private static final String HARD_TAG_ID = "daily_hard_quest";

    private static final int EASY_DAILY_LIMIT = 3;
    private static final int MEDIUM_DAILY_LIMIT = 3;
    private static final int HARD_DAILY_LIMIT = 3;

    private final QuestManager questManager;

    public DailyQuestManager(QuestManager questManager) {
        this.questManager = questManager;
    }

    public void onPlayerJoin(Player player) {
        PlayerQuestData data = questManager.getPlayerQuestData(player.getUniqueId());
        if (data == null) return;

        checkAndApplyDailyReset(data);
        populateAllTagSlots(data);
    }

    public boolean populateAllTagSlots(PlayerQuestData data) {
        boolean filledAny = false;
        for (SmpTag<Quest> tag : getDailyTags()) {
            if (ensureTagSlotsFilled(data, tag)) {
                filledAny = true;
            }
        }
        return filledAny;
    }

    public boolean checkAndApplyDailyReset(PlayerQuestData data) {
        long lastReset = data.getLastDailyCompletionResetTimestamp();
        long todayResetTimestamp = DateUtils.getTodayStartOfDayTimestamp();

        if (lastReset < todayResetTimestamp) {
            List<String> dailyQuestIdsToRemove = new ArrayList<>();
            for (QuestProgress progress : data.getQuestProgresses().values()) {
                if (isDailyQuest(progress.getQuest())) {
                    dailyQuestIdsToRemove.add(progress.getQuest().getId());
                }
            }
            dailyQuestIdsToRemove.forEach(data::removeQuestProgress);

            data.clearDailyCompletions();
            data.setLastDailyCompletionResetTimestamp(System.currentTimeMillis());
            return true;
        }
        return false;
    }

    private boolean ensureTagSlotsFilled(PlayerQuestData data, SmpTag<Quest> tag) {
        int completedToday = getCompletedCountToday(data, tag);
        int maxLimit = getDailyLimitForTag(tag);

        if (completedToday >= maxLimit) return false;

        List<QuestProgress> activeQuests = getActiveDailyQuestsForTag(data, tag);

        int remainingDailyQuota = maxLimit - completedToday;
        int targetCapacity = Math.min(QUESTS_PER_TIER, remainingDailyQuota);
        int slotsToFill = targetCapacity - activeQuests.size();

        if (slotsToFill <= 0) return false;

        boolean assignedAny = false;
        for (int i = 0; i < slotsToFill; i++) {
            if (assignNextQuestFromTag(data, tag, null)) {
                assignedAny = true;
            }
        }
        return assignedAny;
    }

    public boolean claimAndReplenish(Player player, Quest quest) {
        SmpPlayer smpPlayer = PlayerManager.getInstance().getSmpPlayer(player);
        if (smpPlayer == null) return false;

        PlayerQuestData data = questManager.getPlayerQuestData(player.getUniqueId());
        if (data == null) return false;

        QuestProgress progress = data.getQuestProgress(quest.getId());
        if (progress == null || !progress.isCompleted() || progress.isRewardClaimed()) {
            return false;
        }

        for (QuestReward reward : quest.getRewards()) {
            reward.giveReward(smpPlayer);
        }

        SmpTag<Quest> tag = getDailyTagForQuest(quest);
        if (tag != null) {
            data.incrementDailyCompletionForTag(tag.getId());
            data.removeQuestProgress(quest.getId());

            int completedToday = getCompletedCountToday(data, tag);
            int maxLimit = getDailyLimitForTag(tag);

            if (completedToday < maxLimit) {
                assignNextQuestFromTag(data, tag, quest);
            }
        } else {
            progress.setRewardClaimed(true);
        }

        Utils.runAsync(() -> questManager.getQuestDataManager().saveData(data));
        return true;
    }

    private boolean assignNextQuestFromTag(PlayerQuestData data, SmpTag<Quest> tag, Quest previousQuest) {
        List<Quest> allTagQuests = tag.getElements().stream().toList();
        if (allTagQuests.isEmpty()) return false;

        List<Quest> candidates = allTagQuests.stream()
                .filter(q -> !data.getQuestProgresses().containsKey(q.getId()))
                .filter(q -> previousQuest == null || !q.getId().equals(previousQuest.getId()))
                .toList();

        if (candidates.isEmpty()) return false;

        Quest selectedQuest = candidates.get(Utils.RANDOM.nextInt(candidates.size()));
        QuestProgress newProgress = new QuestProgress(selectedQuest);
        data.setQuestProgress(selectedQuest.getId(), newProgress);
        return true;
    }

    public List<QuestProgress> getActiveDailyQuestsForTag(PlayerQuestData data, SmpTag<Quest> tag) {
        Set<String> tagQuestIds = tag.getElements().stream()
                .map(Quest::getId)
                .collect(Collectors.toSet());

        return data.getQuestProgresses().values().stream()
                .filter(qp -> tagQuestIds.contains(qp.getQuest().getId()))
                .toList();
    }

    public int getCompletedCountToday(PlayerQuestData data, SmpTag<Quest> tag) {
        return data.getDailyCompletionsForTag(tag.getId());
    }

    public int getDailyLimitForTag(SmpTag<Quest> tag) {
        if (tag.getId().equals(EASY_TAG_ID)) return EASY_DAILY_LIMIT;
        if (tag.getId().equals(MEDIUM_TAG_ID)) return MEDIUM_DAILY_LIMIT;
        if (tag.getId().equals(HARD_TAG_ID)) return HARD_DAILY_LIMIT;
        return 3;
    }

    public SmpTag<Quest> getDailyTagForQuest(Quest quest) {
        for (SmpTag<Quest> tag : getDailyTags()) {
            if (tag.contains(quest)) return tag;
        }
        return null;
    }

    public boolean isDailyQuest(Quest quest) {
        return getDailyTagForQuest(quest) != null;
    }

    /**
     * Resolves the 3 daily quest tiers from {@link Registries#QUEST}. A tier is skipped (not
     * included) until an admin drops a matching {@code quests/tags/<id>.json} file — see
     * {@link com.roguesmp.registry.Registry#loadTagsFrom}.
     */
    public List<SmpTag<Quest>> getDailyTags() {
        List<SmpTag<Quest>> tags = new ArrayList<>();
        for (String id : List.of(EASY_TAG_ID, MEDIUM_TAG_ID, HARD_TAG_ID)) {
            SmpTag<Quest> tag = Registries.QUEST.getTag(id);
            if (tag != null) tags.add(tag);
        }
        return tags;
    }
}
