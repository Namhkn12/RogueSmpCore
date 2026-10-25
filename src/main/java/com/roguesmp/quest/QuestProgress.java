package com.roguesmp.quest;

import com.roguesmp.quest.objective.ObjectiveProgress;
import com.roguesmp.quest.objective.QuestObjective;
import com.roguesmp.codec.Codec;
import com.roguesmp.player.SmpPlayer;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;
import java.util.function.Predicate;

/**
 * Represent player progress for a given {@link Quest}
 */
public class QuestProgress {
    private static final Codec<Set<UUID>> UUID_SET = Codec.listOf(Codec.UUID).xmap(HashSet::new, ArrayList::new);


    public static final Codec<QuestProgress> CODEC = Codec.composite(
            Quest.REFERENCE_CODEC.fieldOf("quest").forGetter(QuestProgress::getQuest),
            Codec.LONG.fieldOf("acceptTimestamp").forGetter(QuestProgress::getAcceptTimestamp),
            Codec.LONG.optionalFieldOf("completedTimestamp", -1L).forGetter(QuestProgress::getCompletedTimestamp),
            Codec.LONG.optionalFieldOf("cycle", 0L).forGetter(QuestProgress::getCycle),
            Codec.lenientUnboundedMap(ObjectiveProgress.CODEC).optionalFieldOf("progress", new HashMap<>()).forGetter(QuestProgress::getSavableProgressMap),
            UUID_SET.optionalFieldOf("eligible", Set.of()).forGetter(QuestProgress::getEligible),
            QuestProgress::new
    );

    private final Quest quest;
    private long acceptTimestamp;
    private long completedTimestamp;
    private long cycle;
    private final Map<String, ObjectiveProgress> progress; // String is the id for each Objective, defined in Quest json data.

    private final Set<String> incompleteObjectives; // Checklist for incomplete objectives so we don't have to loop through all objectives every time
    private final Set<UUID> eligible = new HashSet<>();

    /**
     * Create a new progress instance for the given quest.
     */
    public QuestProgress(Quest quest) {
        this.quest = quest;
        this.acceptTimestamp = System.currentTimeMillis();
        this.completedTimestamp = -1;
        this.cycle = quest.cycleAt(acceptTimestamp);
        this.progress = new HashMap<>();
        this.incompleteObjectives = new HashSet<>();
        quest.getObjectives().forEach((key, objective) -> {
            this.progress.put(key, objective.createNewProgress());
            this.incompleteObjectives.add(key);
        });
        resolveGoals();
    }

    public QuestProgress(Quest quest, long acceptTimestamp, long completedTimestamp, long cycle, Map<String, ObjectiveProgress> progress, Set<UUID> eligible) {
        this.quest = quest;
        this.acceptTimestamp = acceptTimestamp;
        this.completedTimestamp = completedTimestamp;
        this.cycle = cycle;
        this.eligible.addAll(eligible);
        //Init progress data, we can then add old data in to prevent null ObjectiveProgress (because we don't save new/empty progress)
        this.progress = new HashMap<>();
        quest.getObjectives().forEach((key, objective) -> {
            this.progress.put(key, objective.createNewProgress());
        });
        this.progress.putAll(progress);
        resolveGoals();

        this.incompleteObjectives = new HashSet<>();

        // Scan ONCE on load/deserialization to check what objectives remain incomplete
        quest.getObjectives().forEach((key, objective) -> {
            if (!objective.isCompleted(this.progress.get(key))) {
                this.incompleteObjectives.add(key);
            }
        });

        // Ensure completedTimestamp is set if loaded in completed state
        if (isCompleted() && this.completedTimestamp == -1) {
            this.completedTimestamp = System.currentTimeMillis();
        }
    }

    /**
     * Updates an objective's progress.
     *
     * @param updater returns whether it changed the progress
     * @return whether the progress changed
     */
    public boolean updateObjectiveProgress(String objectiveId, Predicate<ObjectiveProgress> updater) {
        ObjectiveProgress objProgress = this.progress.get(objectiveId);
        if (objProgress == null || !updater.test(objProgress)) return false;

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

        return true;
    }

    public boolean isObjectiveCompleted(String objectiveId) {
        return !this.incompleteObjectives.contains(objectiveId);
    }

    public List<Component> getDisplay() {
        return getDisplay(null);
    }

    public List<Component> getDisplay(@Nullable SmpPlayer viewer) {
        List<Component> lines = new ArrayList<>();
        quest.getObjectives().forEach((objectiveId, objective) -> {
            ObjectiveProgress objectiveProgress = progress.get(objectiveId);
            if (objectiveProgress != null) lines.addAll(objective.getDisplay(viewer, objectiveProgress));
        });
        return lines;
    }

    public void forceComplete() {
        this.incompleteObjectives.clear();
        complete();
    }

    public void resetProgresses() {
        this.progress.clear();
        this.incompleteObjectives.clear();
        this.completedTimestamp = -1;
        this.acceptTimestamp = System.currentTimeMillis();
        this.cycle = quest.cycleAt(acceptTimestamp);
        this.eligible.clear();

        quest.getObjectives().forEach((key, objective) -> {
            this.progress.put(key, objective.createNewProgress());
            this.incompleteObjectives.add(key);
        });
        resolveGoals();
    }

    private void resolveGoals() {
        progress.forEach((objectiveId, objectiveProgress) -> {
            QuestObjective objective = quest.getObjective(objectiveId);
            if (objective != null) objective.resolveGoal(objectiveProgress, Objects.hash(quest.getId(), objectiveId, cycle));
        });
    }

    /**
     * Group quests only: lets the player claim the rewards once the quest completes. Fixed once completed.
     *
     * @return whether eligibility changed
     */
    public boolean addEligible(UUID playerId) {
        return !isCompleted() && eligible.add(playerId);
    }

    /**
     * @return whether eligibility changed
     */
    public boolean removeEligible(UUID playerId) {
        return !isCompleted() && eligible.remove(playerId);
    }

    public @Unmodifiable Set<UUID> getEligible() {
        return Collections.unmodifiableSet(eligible);
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

    public @Unmodifiable Map<String, ObjectiveProgress> getSavableProgressMap() {
        Map<String, ObjectiveProgress> result = new HashMap<>();
        progress.forEach((id, p) -> { if (!p.isDefault()) result.put(id, p); });
        return result;
    }

    public Quest getQuest() {
        return quest;
    }

    public long getAcceptTimestamp() {
        return acceptTimestamp;
    }

    public void setAcceptTimestamp(long acceptTimestamp) {
        this.acceptTimestamp = acceptTimestamp;
    }

    public long getCycle() {
        return cycle;
    }

    /**
     * Board quests only: true once the reset cycle the quest was accepted in has ended.
     */
    public boolean isExpired(long time) {
        return quest.getBoard() != null && quest.cycleAt(time) != cycle;
    }

    public long getCompletedTimestamp() {
        return completedTimestamp;
    }
}
