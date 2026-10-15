package com.roguesmp.entity.ai;

import com.roguesmp.codec.Codec;
import com.roguesmp.codec.DataResult;
import com.roguesmp.entity.ai.goal.BowAttackGoal;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraft.world.entity.ai.goal.RangedCrossbowAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;

import java.util.Locale;

/**
 * Built-in AI setups. {@link #before} runs ahead of an {@code ai} component's own removals and goals so those can still
 * override it, {@link #after} runs once they are in place.
 */
public enum AiPreset {
    HOSTILE("Chỉ nhắm vào người chơi, bỏ mọi mục tiêu khác. Tự thêm đánh cận chiến nếu mob chưa có cách tấn công") {
        @Override
        public void before(Mob mob) {
            mob.targetSelector.removeAllGoals(goal -> true);
            mob.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(mob, Player.class, true));
        }

        @Override
        public void after(Mob mob) {
            if (mob instanceof PathfinderMob pathfinder && mob.getGoalSelector().getAvailableGoals().stream().noneMatch(wrapped -> isAttack(wrapped.getGoal()))) {
                mob.getGoalSelector().addGoal(2, new MeleeAttackGoal(pathfinder, 1.0, false));
            }
        }
    };

    public static final Codec<AiPreset> CODEC = Codec.STRING.comapFlatMap(AiPreset::parse, AiPreset::id);

    private final String description;

    AiPreset(String description) {
        this.description = description;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String description() {
        return description;
    }

    public void before(Mob mob) {
    }

    public void after(Mob mob) {
    }

    private static boolean isAttack(Goal goal) {
        return goal instanceof MeleeAttackGoal || goal instanceof RangedAttackGoal || goal instanceof RangedBowAttackGoal<?>
                || goal instanceof RangedCrossbowAttackGoal<?> || goal instanceof BowAttackGoal<?>;
    }

    private static DataResult<AiPreset> parse(String id) {
        try {
            return DataResult.success(valueOf(id.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return DataResult.error("Unknown AI preset: " + id);
        }
    }
}
