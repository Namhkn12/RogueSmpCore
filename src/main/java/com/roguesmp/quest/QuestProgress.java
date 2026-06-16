package com.roguesmp.quest;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;

/**
 * Represent player progress for a given {@link Quest}
 */
public class QuestProgress {
    private final Quest quest;
    private boolean completed;
    private boolean rewardClaimed;
    private final Map<String, ObjectiveProgress> progress; // String is the id for each Objective, defined in Quest json data.

    private final Set<String> incompleteObjectives; // Checklist for incomplete objectives so we don't have to loop through all objectives every time

    /**
     * Create a new progress instance for the given quest.
     */
    public QuestProgress(Quest quest) {
        this.quest = quest;
        this.completed = false;
        this.rewardClaimed = false;
        this.progress = new HashMap<>();
        this.incompleteObjectives = new HashSet<>();
        quest.getObjectives().forEach((trackingKey, objective) -> {
            this.progress.put(trackingKey, objective.createNewProgress());
        });

        quest.getObjectives().forEach((key, objective) -> {
            this.progress.put(key, objective.createNewProgress());
            this.incompleteObjectives.add(key);
        });
    }

    public QuestProgress(Quest quest, boolean completed, boolean rewardClaimed, Map<String, ObjectiveProgress> progress) {
        this.quest = quest;
        this.completed = completed;
        this.rewardClaimed = rewardClaimed;
        this.progress = progress;
        this.incompleteObjectives = new HashSet<>();

        if (!completed) {
            // Scan ONCE on login to see what's still left to do
            quest.getObjectives().forEach((key, objective) -> {
                ObjectiveProgress objProgress = progress.get(key);
                if (objProgress == null || !objective.isCompleted(objProgress)) {
                    this.incompleteObjectives.add(key);
                }
            });
        }
    }

    /**
     * Return true if the quest is completed
     */
    public boolean updateObjectiveProgress(String objectiveId, java.util.function.Consumer<ObjectiveProgress> updater) {
        ObjectiveProgress objProgress = this.progress.get(objectiveId);
        if (objProgress == null) return false;

        updater.accept(objProgress);

        QuestObjective objective = quest.getObjective(objectiveId);
        if (objective != null && objective.isCompleted(objProgress)) {
            objective.onCompleted(objProgress);
            this.incompleteObjectives.remove(objectiveId);
        } else {
            this.incompleteObjectives.add(objectiveId);
        }

        this.completed = this.incompleteObjectives.isEmpty();
        return this.completed;
    }

    public boolean isObjectiveCompleted(String objectiveId) {
        return !this.incompleteObjectives.contains(objectiveId);
    }

    /**
     * <b> !! USE WITH CAUTION !! </b>
     */
    public void resetProgresses() {
        this.progress.clear();
        this.incompleteObjectives.clear();
        this.completed = false;
        this.rewardClaimed = false;
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

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public boolean isRewardClaimed() {
        return rewardClaimed;
    }

    public void setRewardClaimed(boolean rewardClaimed) {
        this.rewardClaimed = rewardClaimed;
    }

    public JsonObject serialize() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("completed", completed);
        jsonObject.addProperty("rewardClaimed", rewardClaimed);
        JsonObject progressJson = new JsonObject();
        quest.getObjectives().forEach((s, questObjective) -> {
            ObjectiveProgress objectiveProgress = this.progress.get(s);
            if (objectiveProgress != null) {
                JsonObject progressNode = questObjective.serializeProgress(objectiveProgress);
                progressJson.add(s, progressNode);
            }
        });
        jsonObject.add("progress", progressJson);
        return jsonObject;
    }
}
