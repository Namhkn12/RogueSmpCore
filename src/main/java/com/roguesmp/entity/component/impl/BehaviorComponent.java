package com.roguesmp.entity.component.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.entity.component.EntityComponent;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

/**
 * Vanilla behavior flags, and the "activity radius" other systems
 * (passive spell ticking, {@code EntityManager}'s distance-based tick throttling) use to decide
 * whether this entity is worth paying attention to right now.
 */
public record BehaviorComponent(boolean noAi, boolean invulnerable, boolean persistent, int detectionRange) implements EntityComponent {

    private static final int DEFAULT_DETECTION_RANGE = 20;

    public static final Codec<BehaviorComponent> CODEC = Codec.composite(
            Codec.BOOLEAN.optionalFieldOf("noAi", false).forGetter(BehaviorComponent::noAi),
            Codec.BOOLEAN.optionalFieldOf("invulnerable", false).forGetter(BehaviorComponent::invulnerable),
            Codec.BOOLEAN.optionalFieldOf("persistent", false).forGetter(BehaviorComponent::persistent),
            Codec.INT.optionalFieldOf("detectionRange", DEFAULT_DETECTION_RANGE).forGetter(BehaviorComponent::detectionRange),
            BehaviorComponent::new
    );

    @Override
    public @NotNull EntityComponent copy() {
        return this;
    }

    @Override
    public void apply(LivingEntity entity) {
        entity.setInvulnerable(invulnerable);
        entity.setAI(!noAi);
        entity.setPersistent(persistent);
        entity.setRemoveWhenFarAway(!persistent);
    }

    /** {@link #detectionRange}, normalized to {@value DEFAULT_DETECTION_RANGE} if unset/invalid. */
    public int getEffectiveDetectionRange() {
        return detectionRange <= 0 ? DEFAULT_DETECTION_RANGE : detectionRange;
    }
}
