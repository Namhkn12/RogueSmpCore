package com.roguesmp.entity.component.impl;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.codec.Codec;
import com.roguesmp.entity.ai.AiGoal;
import com.roguesmp.entity.ai.AiPreset;
import com.roguesmp.entity.ai.RemovableGoal;
import com.roguesmp.entity.component.EntityComponent;
import net.minecraft.world.entity.ai.goal.Goal;
import org.bukkit.craftbukkit.entity.CraftMob;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record AiComponent(List<AiPreset> presets, List<RemovableGoal> remove, List<AiGoal> goals) implements EntityComponent {

    public static final Codec<AiComponent> CODEC = Codec.composite(
            Codec.listOf(AiPreset.CODEC).optionalFieldOf("presets", List.of()).forGetter(AiComponent::presets),
            Codec.listOf(RemovableGoal.CODEC).optionalFieldOf("remove", List.of()).forGetter(AiComponent::remove),
            Codec.listOf(AiGoal.CODEC).optionalFieldOf("goals", List.of()).forGetter(AiComponent::goals),
            AiComponent::new
    );

    @Override
    public @NotNull EntityComponent copy() {
        return this;
    }

    @Override
    public int applyOrder() {
        return 1;
    }

    @Override
    public void apply(LivingEntity entity) {
        if (!(entity instanceof Mob mob)) return;

        net.minecraft.world.entity.Mob nmsMob = ((CraftMob) mob).getHandle();
        presets.forEach(preset -> preset.before(nmsMob));
        remove.forEach(goal -> goal.removeFrom(nmsMob));

        for (AiGoal aiGoal : goals) {
            Goal goal = aiGoal.create(nmsMob);
            if (goal == null) {
                RogueSmpCore.LOGGER.warn("AI goal '{}' is not supported by {}", aiGoal.getTypeId(), entity.getType());
                continue;
            }
            (aiGoal.isTarget() ? nmsMob.targetSelector : nmsMob.getGoalSelector()).addGoal(aiGoal.priority(), goal);
        }
        presets.forEach(preset -> preset.after(nmsMob));
    }
}
