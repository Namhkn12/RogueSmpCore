package com.roguesmp.entity.ai.goal;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Items;

import java.util.EnumSet;

/**
 * Vanilla {@code RangedBowAttackGoal} with a configurable draw time. The arrow is always fired at the power a full
 * vanilla draw would give, so a shorter {@code pullTime} only makes the mob shoot faster.
 */
public class BowAttackGoal<T extends Monster & RangedAttackMob> extends Goal {

    private static final int FULL_DRAW_TICKS = 20;
    private static final int STRAFE_SWITCH_TICKS = 20;
    private static final int LOSE_SIGHT_TICKS = -60;

    private final T mob;
    private final double speedModifier;
    private final int attackInterval;
    private final int pullTime;
    private final float attackRadiusSqr;
    private int attackTime = -1;
    private int seeTime;
    private boolean strafingClockwise;
    private boolean strafingBackwards;
    private int strafingTime = -1;

    public BowAttackGoal(T mob, double speedModifier, int attackInterval, int pullTime, float attackRadius) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.attackInterval = attackInterval;
        this.pullTime = Math.max(1, pullTime);
        this.attackRadiusSqr = attackRadius * attackRadius;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return mob.getTarget() != null && isHoldingBow();
    }

    @Override
    public boolean canContinueToUse() {
        return (canUse() || !mob.getNavigation().isDone()) && isHoldingBow();
    }

    private boolean isHoldingBow() {
        return mob.isHolding(Items.BOW);
    }

    @Override
    public void start() {
        super.start();
        mob.setAggressive(true);
    }

    @Override
    public void stop() {
        super.stop();
        mob.setAggressive(false);
        seeTime = 0;
        attackTime = -1;
        mob.stopUsingItem();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = mob.getTarget();
        if (target == null) return;

        double distanceSqr = mob.distanceToSqr(target.getX(), target.getY(), target.getZ());
        boolean canSee = mob.getSensing().hasLineOfSight(target);
        if (canSee != seeTime > 0) seeTime = 0;
        seeTime += canSee ? 1 : -1;

        if (distanceSqr <= attackRadiusSqr && seeTime >= 20) {
            mob.getNavigation().stop();
            strafingTime++;
        } else {
            mob.getNavigation().moveTo(target, speedModifier);
            strafingTime = -1;
        }

        if (strafingTime >= STRAFE_SWITCH_TICKS) {
            if (mob.getRandom().nextFloat() < 0.3) strafingClockwise = !strafingClockwise;
            if (mob.getRandom().nextFloat() < 0.3) strafingBackwards = !strafingBackwards;
            strafingTime = 0;
        }

        if (strafingTime > -1) {
            if (distanceSqr > attackRadiusSqr * 0.75F) strafingBackwards = false;
            else if (distanceSqr < attackRadiusSqr * 0.25F) strafingBackwards = true;

            mob.getMoveControl().strafe(strafingBackwards ? -0.5F : 0.5F, strafingClockwise ? 0.5F : -0.5F);
            if (mob.getControlledVehicle() instanceof Mob vehicle) vehicle.lookAt(target, 30.0F, 30.0F);
            mob.lookAt(target, 30.0F, 30.0F);
        } else {
            mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
        }

        if (mob.isUsingItem()) {
            if (!canSee && seeTime < LOSE_SIGHT_TICKS) {
                mob.stopUsingItem();
            } else if (canSee && mob.getTicksUsingItem() >= pullTime) {
                mob.stopUsingItem();
                mob.performRangedAttack(target, BowItem.getPowerForTime(Math.max(FULL_DRAW_TICKS, mob.getTicksUsingItem())));
                attackTime = attackInterval;
            }
        } else if (--attackTime <= 0 && seeTime >= LOSE_SIGHT_TICKS) {
            mob.startUsingItem(ProjectileUtil.getWeaponHoldingHand(mob, Items.BOW));
        }
    }
}
