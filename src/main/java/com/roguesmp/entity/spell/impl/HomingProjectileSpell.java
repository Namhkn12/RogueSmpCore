package com.roguesmp.entity.spell.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.entity.spell.Spell;
import com.roguesmp.entity.spell.SpellContext;
import com.roguesmp.entity.spell.SpellParams;
import com.roguesmp.entity.spell.common.SpellTargets;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

/**
 * Steers every projectile the owner launches towards its current target. {@code turnStrength}
 * (0..1) is how much of the velocity is re-aimed per tick - 1 is a perfect lock-on.
 */
public class HomingProjectileSpell extends Spell {

    public record Params(double turnStrength, int maxTrackingTicks) implements SpellParams {
        public static final Codec<Params> CODEC = Codec.composite(
                Codec.DOUBLE.optionalFieldOf("turnStrength", 0.25).forGetter(Params::turnStrength),
                Codec.INT.optionalFieldOf("maxTrackingTicks", 40).forGetter(Params::maxTrackingTicks),
                Params::new
        );
    }

    private final LivingEntity owner;
    private final Params params;
    private final List<Tracked> tracked = new ArrayList<>();

    private static final class Tracked {
        private final Projectile projectile;
        private final Player target;
        private int ticks;

        private Tracked(Projectile projectile, Player target) {
            this.projectile = projectile;
            this.target = target;
        }
    }

    public HomingProjectileSpell(LivingEntity owner, Params params) {
        this.owner = owner;
        this.params = params;
    }

    @Override
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        Player target = SpellTargets.playerTarget(owner);
        if (target != null) tracked.add(new Tracked(event.getEntity(), target));
    }

    @Override
    public void tick(SpellContext context, int interval) {
        tracked.removeIf(entry -> {
            entry.ticks += interval;
            Projectile projectile = entry.projectile;
            boolean landed = projectile instanceof AbstractArrow arrow && arrow.isInBlock();
            if (!projectile.isValid() || landed || !entry.target.isValid() || entry.ticks > params.maxTrackingTicks()) return true;

            Vector velocity = projectile.getVelocity();
            double speed = velocity.length();
            Vector desired = SpellTargets.centerOf(entry.target).toVector()
                    .subtract(projectile.getLocation().toVector())
                    .normalize()
                    .multiply(speed);
            Vector steered = velocity.add(desired.subtract(velocity).multiply(params.turnStrength()));
            projectile.setVelocity(steered.normalize().multiply(speed));
            return false;
        });
    }

    @Override
    public void onRemoved() {
        tracked.clear();
    }
}
