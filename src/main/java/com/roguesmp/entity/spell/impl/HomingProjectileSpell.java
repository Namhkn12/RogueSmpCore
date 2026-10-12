package com.roguesmp.entity.spell.impl;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.codec.Codec;
import com.roguesmp.entity.spell.Spell;
import com.roguesmp.entity.spell.SpellParams;
import com.roguesmp.entity.spell.common.SpellTargets;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * Steers every projectile the owner launches towards its current target. {@code turnStrength}
 * (0..1) is how much of the velocity is re-aimed per tick - 1 is a perfect lock-on.
 */
public class HomingProjectileSpell extends Spell {

    public static final String TYPE_KEY = "homing_projectile_spell";

    public record Params(double turnStrength, int maxTrackingTicks) implements SpellParams {
        public static final Codec<Params> CODEC = Codec.composite(
                Codec.DOUBLE.optionalFieldOf("turnStrength", 0.25).forGetter(Params::turnStrength),
                Codec.INT.optionalFieldOf("maxTrackingTicks", 40).forGetter(Params::maxTrackingTicks),
                Params::new
        );

        @Override
        public String getTypeId() {
            return TYPE_KEY;
        }
    }

    private final LivingEntity owner;
    private final Params params;

    public HomingProjectileSpell(LivingEntity owner, Params params) {
        this.owner = owner;
        this.params = params;
    }

    @Override
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        Player target = SpellTargets.playerTarget(owner);
        if (target == null) return;
        Projectile projectile = event.getEntity();

        BukkitRunnable runnable = new BukkitRunnable() {
            private int ticks;

            @Override
            public void run() {
                boolean landed = projectile instanceof AbstractArrow arrow && arrow.isInBlock();
                if (!projectile.isValid() || landed || !target.isValid() || ticks++ > params.maxTrackingTicks()) {
                    cancel();
                    activeRunnables.remove(this);
                    return;
                }

                Vector velocity = projectile.getVelocity();
                double speed = velocity.length();
                Vector desired = SpellTargets.centerOf(target).toVector()
                        .subtract(projectile.getLocation().toVector())
                        .normalize()
                        .multiply(speed);
                Vector steered = velocity.add(desired.subtract(velocity).multiply(params.turnStrength()));
                projectile.setVelocity(steered.normalize().multiply(speed));
            }
        };
        activeRunnables.add(runnable);
        runnable.runTaskTimer(RogueSmpCore.getInstance(), 1, 1);
    }

    @Override
    public void run(int interval) {
    }

    @Override
    public int cooldownTicks() {
        return 0;
    }

    public static HomingProjectileSpell create(Params params, LivingEntity owner) {
        return new HomingProjectileSpell(owner, params);
    }
}
