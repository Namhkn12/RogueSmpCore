package com.roguesmp.entity.spell.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.constant.DamageType;
import com.roguesmp.entity.spell.Spell;
import com.roguesmp.entity.spell.SpellContext;
import com.roguesmp.entity.spell.SpellParams;
import com.roguesmp.entity.spell.common.SpellTargets;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.utils.DamageUtils;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

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
    }

    private final LivingEntity owner;
    private final Params params;
    private int uses;
    private int cooldownRemaining;
    private @Nullable Player chargeTarget;
    private int dashTicks;

    public ChargeSpell(LivingEntity owner, Params params) {
        this.owner = owner;
        this.params = params;
    }

    @Override
    public void tick(SpellContext context, int interval) {
        if (chargeTarget != null) {
            dash(interval);
            return;
        }

        cooldownRemaining -= interval;
        if (cooldownRemaining > 0) return;
        if (params.maxUses() > 0 && uses >= params.maxUses()) return;

        Player target = context.target();
        if (target == null || !owner.hasLineOfSight(target)) return;
        double distance = target.getLocation().distance(owner.getLocation());
        if (distance < params.minRange() || distance > params.triggerRange()) return;

        uses++;
        cooldownRemaining = params.cooldownTicks();
        chargeTarget = target;
        dashTicks = 0;
        owner.getWorld().playSound(owner.getLocation(), Sound.ENTITY_HOGLIN_ANGRY, 1.5f, 0.8f);
    }

    private void dash(int interval) {
        Player target = chargeTarget;
        dashTicks += interval;
        if (!target.isValid() || dashTicks > params.maxDashTicks()) {
            chargeTarget = null;
            return;
        }

        Vector direction = SpellTargets.horizontalDirection(owner.getLocation(), target.getLocation());
        owner.setVelocity(direction.clone().multiply(params.dashSpeed()).setY(owner.getVelocity().getY()));
        owner.getWorld().spawnParticle(Particle.CLOUD, owner.getLocation(), 3, 0.3, 0.1, 0.3, 0.02);

        if (owner.getBoundingBox().expand(0.4).overlaps(target.getBoundingBox())) {
            impact(target, direction);
            chargeTarget = null;
        }
    }

    @Override
    public void onRemoved() {
        chargeTarget = null;
    }

    private void impact(Player target, Vector direction) {
        DamageEvent.Metadata metadata = DamageEvent.Metadata.spell(DamageType.MELEE, TYPE_KEY);
        metadata.setDoKnockback(false);
        DamageUtils.damage(target, owner, params.contactDamage(), metadata);
        target.setVelocity(direction.multiply(params.launchHorizontal()).setY(params.launchVertical()));
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_RAVAGER_ATTACK, 1.5f, 1f);
    }
}
