package com.roguesmp.quest;

import com.google.gson.JsonObject;
import com.roguesmp.player.SmpPlayer;
import net.kyori.adventure.text.Component;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.List;

/**
 * An objective that player must do/satisfy to finish the quest. Used for quest definition and event handling. A {@link Quest} can have multiple objectives of the same subclass.
 */
public interface QuestObjective {

    /**
     * Get objective display with progress data. Can be used to display player's current objective progress for example
     */
    List<Component> getDisplay(SmpPlayer smpPlayer, ObjectiveProgress progress);

    /**
     * Get objective display. Can be used to display an objective info for example
     */
    List<Component> getDisplay(SmpPlayer smpPlayer);

    boolean isCompleted(ObjectiveProgress progress);

    /**
     * Used to serialize player objective progress data
     */
    JsonObject serializeProgress(ObjectiveProgress progress);

    /**
     * Used to create progress data from player quest data
     */
    ObjectiveProgress deserializeProgress(JsonObject data);

    /**
     * Create new progress instance for this objective. Used for assigning a new quest.
     */
    ObjectiveProgress createNewProgress();

    /**
     * Run when this objective is completed
     */
    default void onCompleted(ObjectiveProgress progress) {

    }

    default void onKillEntity(EntityDeathEvent event, ObjectiveProgress progress) {

    }

}
