package com.roguesmp.quest;

/**
 * This marker interface is responsible for holding player's quest progress data
 */
public interface ObjectiveProgress {
    /**
     * Method to check if this progress is newly created (we don't want to save an empty progress)
     * @return true if the player has made no progress towards this objective yet.
     */
    boolean isDefault();
}
