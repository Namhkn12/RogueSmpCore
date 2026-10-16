package com.roguesmp.entity.spell.impl;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.codec.Codec;
import com.roguesmp.constant.DamageType;
import com.roguesmp.entity.spell.Spell;
import com.roguesmp.entity.spell.SpellParams;
import com.roguesmp.entity.spell.common.SpellTargets;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.utils.DamageUtils;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * Dashes at the target once it's within [{@code minRange}, {@code triggerRange}], and on contact
 * deals {@code contactDamage} and launches it. {@code maxUses} <= 0 means unlimited.
 */
public class ChargeSpell extends Spell {

    public static final String TYPE_KEY = "charge_spell";

    public record Params(double triggerRange, double minRange, double dashSpeed, int maxDashTicks,
                         double contactDamage, double launchHorizontal, double launchVertical,
                         int maxUses, int cooldownTicks) implements SpellParams {
        public static final Codec<Params> CODEC = Codec.composite(
                Codec.DOUBLE.optionalFieldOf("triggerRange", 12.0).forGetter(Params::triggerRange),
                Codec.DOUBLE.optionalFieldOf("minRange", 3.0).forGetter(Params::minRange),
                Codec.DOUBLE.optionalFieldOf("dashSpeed", 0.9).forGetter(Params::dashSpeed),
                Codec.INT.optionalFieldOf("maxDashTicks", 30).forGetter(Params::maxDashTicks),
                Codec.DOUBLE.optionalFieldOf("contactDamage", 6.0).forGetter(Params::contactDamage),
                Codec.DOUBLE.optionalFieldOf("launchHorizontal", 0.6).forGetter(Params::launchHorizontal),
                Codec.DOUBLE.optionalFieldOf("launchVertical", 1.2).forGetter(Params::launchVertical),
                Codec.INT.optionalFieldOf("maxUses", 1).forGetter(Params::maxUses),
                Codec.INT.optionalFieldOf("cooldownTicks", 200).forGetter(Params::cooldownTicks),
                Params::new
        );

        @Override
        public String getTypeId() {
            return TYPE_KEY;
        }
    }

    private final LivingEntity owner;
    private final Params params;
    private int uses;
    private int cooldownRemaining;
    private boolean charging;

    public ChargeSpell(LivingEntity owner, Params params) {
        this.owner = owner;
        this.params = params;
    }

    @Override
    public void run(int interval) {
        cooldownRemaining -= interval;
        if (charging || cooldownRemaining > 0) return;
        if (params.maxUses() > 0 && uses >= params.maxUses()) return;

        Player target = SpellTargets.playerTarget(owner);
        if (target == null || !owner.hasLineOfSight(target)) return;
        double distance = target.getLocation().distance(owner.getLocation());
        if (distance < params.minRange() || distance > params.triggerRange()) return;

        uses++;
        cooldownRemaining = params.cooldownTicks();
        startCharge(target);
    }

    private void startCharge(Player target) {
        charging = true;
        owner.getWorld().playSound(owner.getLocation(), Sound.ENTITY_HOGLIN_ANGRY, 1.5f, 0.8f);

        BukkitRunnable runnable = new BukkitRunnable() {
            private int ticks;

            @Override
            public void run() {
                if (!owner.isValid() || !target.isValid() || ticks++ > params.maxDashTicks()) {
                    finish();
                    return;
                }

                Vector direction = SpellTargets.horizontalDirection(owner.getLocation(), target.getLocation());
                owner.setVelocity(direction.clone().multiply(params.dashSpeed()).setY(owner.getVelocity().getY()));
                owner.getWorld().spawnParticle(Particle.CLOUD, owner.getLocation(), 3, 0.3, 0.1, 0.3, 0.02);

                if (owner.getBoundingBox().expand(0.4).overlaps(target.getBoundingBox())) {
                    impact(target, direction);
                    finish();
                }
            }

            private void finish() {
                charging = false;
                cancel();
                activeRunnables.remove(this);
            }
        };
        activeRunnables.add(runnable);
        runnable.runTaskTimer(RogueSmpCore.getInstance(), 0, 1);
    }

    private void impact(Player target, Vector direction) {
        DamageEvent.Metadata metadata = DamageEvent.Metadata.spell(DamageType.MELEE, TYPE_KEY);
        metadata.setDoKnockback(false);
        DamageUtils.damage(target, owner, params.contactDamage(), metadata);
        target.setVelocity(direction.multiply(params.launchHorizontal()).setY(params.launchVertical()));
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_RAVAGER_ATTACK, 1.5f, 1f);
    }

    @Override
    public void cancel() {
        super.cancel();
        charging = false;
    }

    @Override
    public int cooldownTicks() {
        return 0;
    }

    public static ChargeSpell create(Params params, LivingEntity owner) {
        return new ChargeSpell(owner, params);
    }
}
