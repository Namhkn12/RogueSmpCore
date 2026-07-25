package com.roguesmp.quest;

import com.roguesmp.codec.Codec;
import com.roguesmp.registry.Registries;

/**
 * This interface is responsible for holding player's quest progress data
 */
public interface ObjectiveProgress {

    Codec<ObjectiveProgress> CODEC = Codec.dispatch(
            ObjectiveProgress::getTypeId,
            Registries.OBJECTIVE_PROGRESS_CODEC::getOrThrow
    );

    /**
     * Method to check if this progress is newly created (we don't want to save an empty progress)
     * @return true if the player has made no progress towards this objective yet.
     */
    boolean isDefault();

    String getTypeId();
}
