package com.roguesmp.entity.ai;

import com.roguesmp.codec.Codec;
import com.roguesmp.registry.Registries;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import org.jetbrains.annotations.Nullable;

public interface AiGoal {

    Codec<AiGoal> CODEC = Codec.dispatch(
            AiGoal::getTypeId,
            id -> Registries.AI_GOAL_CODEC.getOrThrow(id)
    );

    String getTypeId();

    int priority();

    default boolean isTarget() {
        return false;
    }

    /**
     * @return the goal, or null when {@code mob} can't run it (e.g. a ranged goal on a mob that can't shoot)
     */
    @Nullable Goal create(Mob mob);
}
