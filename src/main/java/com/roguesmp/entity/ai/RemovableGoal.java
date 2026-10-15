package com.roguesmp.entity.ai;

import com.roguesmp.codec.Codec;
import com.roguesmp.codec.DataResult;
import com.roguesmp.entity.ai.goal.BowAttackGoal;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraft.world.entity.ai.goal.RangedCrossbowAttackGoal;
import net.minecraft.world.entity.ai.goal.target.DefendVillageTargetGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;

import java.util.Locale;
import java.util.function.Predicate;

public enum RemovableGoal {
    ALL_GOALS(Scope.GOALS, goal -> true),
    ALL_TARGETS(Scope.TARGETS, goal -> true),
    MELEE_ATTACK(Scope.GOALS, goal -> goal instanceof MeleeAttackGoal),
    RANGED_ATTACK(Scope.GOALS, goal -> goal instanceof RangedAttackGoal || goal instanceof RangedBowAttackGoal<?> || goal instanceof RangedCrossbowAttackGoal<?> || goal instanceof BowAttackGoal<?>),
    LEAP_AT_TARGET(Scope.GOALS, goal -> goal instanceof LeapAtTargetGoal),
    AVOID_ENTITY(Scope.GOALS, goal -> goal instanceof AvoidEntityGoal<?>),
    PANIC(Scope.GOALS, goal -> goal instanceof PanicGoal),
    RANDOM_STROLL(Scope.GOALS, goal -> goal instanceof RandomStrollGoal),
    LOOK_AT_PLAYER(Scope.GOALS, goal -> goal instanceof LookAtPlayerGoal),
    RANDOM_LOOK_AROUND(Scope.GOALS, goal -> goal instanceof RandomLookAroundGoal),
    FLOAT(Scope.GOALS, goal -> goal instanceof FloatGoal),
    NEAREST_ATTACKABLE_TARGET(Scope.TARGETS, goal -> goal instanceof NearestAttackableTargetGoal<?>),
    HURT_BY_TARGET(Scope.TARGETS, goal -> goal instanceof HurtByTargetGoal),
    DEFEND_VILLAGE(Scope.TARGETS, goal -> goal instanceof DefendVillageTargetGoal);

    public static final Codec<RemovableGoal> CODEC = Codec.STRING.comapFlatMap(RemovableGoal::parse, RemovableGoal::id);

    private enum Scope {GOALS, TARGETS}

    private final Scope scope;
    private final Predicate<Goal> matcher;

    RemovableGoal(Scope scope, Predicate<Goal> matcher) {
        this.scope = scope;
        this.matcher = matcher;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public void removeFrom(Mob mob) {
        (scope == Scope.GOALS ? mob.getGoalSelector() : mob.targetSelector).removeAllGoals(matcher);
    }

    private static DataResult<RemovableGoal> parse(String id) {
        try {
            return DataResult.success(valueOf(id.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return DataResult.error("Unknown removable goal: " + id);
        }
    }
}
