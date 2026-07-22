package com.roguesmp.quest;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;

/**
 * Represent player progress for a given {@link Quest}
 */
public class QuestProgress {
    private final Quest quest;
    private boolean rewardClaimed;
    private long acceptTimestamp;
    private long completedTimestamp;
    private final Map<String, ObjectiveProgress> progress; // String is the id for each Objective, defined in Quest json data.

    private final Set<String> incompleteObjectives; // Checklist for incomplete objectives so we don't have to loop through all objectives every time

    /**
     * Create a new progress instance for the given quest.
     */
    public QuestProgress(Quest quest) {
        this.quest = quest;
        this.rewardClaimed = false;
        this.acceptTimestamp = System.currentTimeMillis();
        this.completedTimestamp = -1;
        this.progress = new HashMap<>();
        this.incompleteObjectives = new HashSet<>();
        //Init progress data
        quest.getObjectives().forEach((key, objective) -> {
            this.progress.put(key, objective.createNewProgress());
            this.incompleteObjectives.add(key);
        });
    }

    public QuestProgress(Quest quest, boolean rewardClaimed, long acceptTimestamp, long completedTimestamp, Map<String, ObjectiveProgress> progress) {
        this.quest = quest;
        this.rewardClaimed = rewardClaimed;
        this.acceptTimestamp = acceptTimestamp;
        this.completedTimestamp = completedTimestamp;
        //Init progress data, we can then add old data in to prevent null ObjectiveProgress (because we don't save new/empty progress)
        this.progress = new HashMap<>();
        quest.getObjectives().forEach((key, objective) -> {
            this.progress.put(key, objective.createNewProgress());
        });
        this.progress.putAll(progress);

        this.incompleteObjectives = new HashSet<>();

        // Scan ONCE on load/deserialization to check what objectives remain incomplete
        quest.getObjectives().forEach((key, objective) -> {
            ObjectiveProgress objProgress = progress.get(key);
            if (objProgress == null || !objective.isCompleted(objProgress)) {
                this.incompleteObjectives.add(key);
            }
        });

        // Ensure completedTimestamp is set if loaded in completed state
        if (isCompleted() && this.completedTimestamp == -1) {
            this.completedTimestamp = System.currentTimeMillis();
        }
    }

    /**
     * Updates an objective's progress and returns whether the entire quest is now complete.
     */
    public boolean updateObjectiveProgress(String objectiveId, java.util.function.Consumer<ObjectiveProgress> updater) {
        ObjectiveProgress objProgress = this.progress.get(objectiveId);
        if (objProgress == null) return isCompleted();

        updater.accept(objProgress);

        QuestObjective objective = quest.getObjective(objectiveId);
        if (objective != null && objective.isCompleted(objProgress)) {
            objective.onCompleted(objProgress);
            this.incompleteObjectives.remove(objectiveId);
        } else {
            this.incompleteObjectives.add(objectiveId);
        }

        if (this.incompleteObjectives.isEmpty() && this.completedTimestamp == -1) {
            complete();
        }

        return isCompleted();
    }

    public boolean isObjectiveCompleted(String objectiveId) {
        return !this.incompleteObjectives.contains(objectiveId);
    }

    /**
     * <b> !! USE WITH CAUTION !! </b> <br>
     * Reset all progress, including timestamp and reward states.
     */
    public void resetProgresses() {
        this.progress.clear();
        this.incompleteObjectives.clear();
        this.completedTimestamp = -1;
        this.acceptTimestamp = System.currentTimeMillis();
        this.rewardClaimed = false;

        quest.getObjectives().forEach((key, objective) -> {
            this.progress.put(key, objective.createNewProgress());
            this.incompleteObjectives.add(key);
        });
    }

    /**
     * Sets the completed timestamp if not already set.
     *
     * @return Timestamp (currentTimeMillis) when the quest progress was completed
     */
    public long complete() {
        if (this.completedTimestamp == -1) {
            this.completedTimestamp = System.currentTimeMillis();
        }
        return this.completedTimestamp;
    }

    /**
     * Evaluates if the quest is completed based on remaining incomplete objectives.
     */
    public boolean isCompleted() {
        return this.incompleteObjectives.isEmpty();
    }

    public @Unmodifiable Map<String, ObjectiveProgress> getProgressMap() {
        return Collections.unmodifiableMap(progress);
    }

    public @Unmodifiable Set<String> getIncompleteObjectives() {
        return Collections.unmodifiableSet(incompleteObjectives);
    }

    public Quest getQuest() {
        return quest;
    }

    public boolean isRewardClaimed() {
        return rewardClaimed;
    }

    public long getAcceptTimestamp() {
        return acceptTimestamp;
    }

    public void setAcceptTimestamp(long acceptTimestamp) {
        this.acceptTimestamp = acceptTimestamp;
    }

    public long getCompletedTimestamp() {
        return completedTimestamp;
    }

    public void setRewardClaimed(boolean rewardClaimed) {
        this.rewardClaimed = rewardClaimed;
    }

    public JsonObject serialize() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("rewardClaimed", rewardClaimed);
        jsonObject.addProperty("acceptTimestamp", acceptTimestamp);
        jsonObject.addProperty("completedTimestamp", completedTimestamp);

        JsonObject progressJson = new JsonObject();
        quest.getObjectives().forEach((s, questObjective) -> {
            ObjectiveProgress objectiveProgress = this.progress.get(s);
            if (objectiveProgress != null && !objectiveProgress.isDefault()) {
                JsonObject progressNode = questObjective.serializeProgress(objectiveProgress);
                progressJson.add(s, progressNode);
            }
        });
        jsonObject.add("progress", progressJson);
        return jsonObject;
    }
}
