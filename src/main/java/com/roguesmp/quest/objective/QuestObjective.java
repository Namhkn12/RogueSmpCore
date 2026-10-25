package com.roguesmp.quest.objective;

import com.roguesmp.quest.Quest;
import com.roguesmp.codec.Codec;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.registry.Registries;
import net.kyori.adventure.text.Component;
import org.bukkit.event.entity.EntityDeathEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * An objective that player must do/satisfy to finish the quest. Used for quest definition and event handling. A {@link Quest} can have multiple objectives of the same subclass.
 */
public interface QuestObjective {

    Codec<QuestObjective> CODEC = Codec.dispatch(
            QuestObjective::getTypeId,
            Registries.QUEST_OBJECTIVE_CODEC::getOrThrow
    );

    /**
     * Get objective display with progress data. Can be used to display player's current objective progress for example
     */
    List<Component> getDisplay(@Nullable SmpPlayer smpPlayer, ObjectiveProgress progress);

    /**
     * Display that needs no viewer, for progress shared by a group such as a scoreboard line.
     */
    default List<Component> getDisplay(ObjectiveProgress progress) {
        return getDisplay(null, progress);
    }

    /**
     * Get objective display. Can be used to display an objective info for example
     */
    List<Component> getDisplay(SmpPlayer smpPlayer);

    boolean isCompleted(ObjectiveProgress progress);

    String getTypeId();

    /**
     * Create new progress instance for this objective. Used for assigning a new quest.
     */
    ObjectiveProgress createNewProgress();

    /**
     * Sets the progress's in-memory goal. The same seed always gives the same goal, so it is never saved.
     */
    default void resolveGoal(ObjectiveProgress progress, long seed) {

    }

    /**
     * Run when this objective is completed
     */
    default void onCompleted(ObjectiveProgress progress) {

    }

    /**
     * @return whether the progress changed
     */
    default boolean onKillEntity(EntityDeathEvent event, ObjectiveProgress progress) {
        return false;
    }

    /**
     * @return whether the progress changed
     */
    default boolean onSignal(String signal, int amount, ObjectiveProgress progress) {
        return false;
    }

    /**
     * Called when the player hands in items to this objective's quest; takes what it needs from their inventory.
     *
     * @return whether the progress changed
     */
    default boolean onSubmitItems(SmpPlayer smpPlayer, ObjectiveProgress progress) {
        return false;
    }

}
