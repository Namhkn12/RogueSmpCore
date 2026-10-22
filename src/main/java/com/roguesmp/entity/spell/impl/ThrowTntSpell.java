package com.roguesmp.entity.spell.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.entity.spell.ActiveSpell;
import com.roguesmp.entity.spell.SpellContext;
import com.roguesmp.entity.spell.SpellParams;
import com.roguesmp.entity.spell.common.ExplosionSpec;
import com.roguesmp.entity.spell.common.SpellTargets;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;

import java.util.ArrayList;
import java.util.List;

/**
 * Lobs a primed TNT at a target standing {@code minRange}..{@code maxRange} blocks away. The vanilla
 * TNT is yield-0 and purely visual - the blast is an {@link ExplosionSpec} scaled off the thrower's
 * {@code attack_damage}.
 */
public class ThrowTntSpell extends ActiveSpell {

    public static final String TYPE_KEY = "throw_tnt_spell";

    private static final double TNT_GRAVITY = 0.04;
    private static final double TNT_DRAG = 0.98;

    public record Params(double minRange, double maxRange, int fuseTicks, ExplosionSpec explosion, int cooldownTicks) implements SpellParams {
        public static final Codec<Params> CODEC = Codec.composite(
                Codec.DOUBLE.optionalFieldOf("minRange", 5.0).forGetter(Params::minRange),
                Codec.DOUBLE.optionalFieldOf("maxRange", 7.0).forGetter(Params::maxRange),
                Codec.INT.optionalFieldOf("fuseTicks", 30).forGetter(Params::fuseTicks),
                ExplosionSpec.MAP_CODEC.forGetter(Params::explosion),
                Codec.INT.optionalFieldOf("cooldownTicks", 80).forGetter(Params::cooldownTicks),
                Params::new
        );
    }

    private final LivingEntity owner;
    private final Params params;
    private final List<ThrownTnt> inFlight = new ArrayList<>();

    private record ThrownTnt(TNTPrimed tnt, long detonateAt) {
    }

    public ThrowTntSpell(LivingEntity owner, Params params) {
        super(params.cooldownTicks());
        this.owner = owner;
        this.params = params;
    }

    @Override
    public boolean canCast(SpellContext context) {
        Player target = SpellTargets.playerTarget(owner);
        if (target == null || !owner.hasLineOfSight(target)) return false;
        double distance = target.getLocation().distance(owner.getLocation());
        return distance >= params.minRange() && distance <= params.maxRange();
    }

    @Override
    public void onCast(SpellContext context) {
        Player target = SpellTargets.playerTarget(owner);
        if (target == null) return;

        Location from = owner.getEyeLocation();
        int flightTicks = Math.max(10, params.fuseTicks() - 5);
        TNTPrimed tnt = owner.getWorld().spawn(from, TNTPrimed.class, primed -> {
            primed.setSource(owner);
            primed.setYield(0);
            primed.setFuseTicks(params.fuseTicks() + 10);
            primed.setVelocity(SpellTargets.ballisticVelocity(from, target.getLocation(), flightTicks, TNT_GRAVITY, TNT_DRAG));
        });
        owner.swingMainHand();
        owner.getWorld().playSound(from, Sound.ENTITY_TNT_PRIMED, 1f, 1f);

        inFlight.add(new ThrownTnt(tnt, context.tick() + params.fuseTicks()));
    }

    @Override
    public void tick(SpellContext context, int interval) {
        inFlight.removeIf(thrown -> {
            if (!thrown.tnt().isValid()) return true;
            if (context.tick() < thrown.detonateAt()) return false;
            Location location = thrown.tnt().getLocation();
            thrown.tnt().remove();
            params.explosion().detonate(owner, location, TYPE_KEY);
            return true;
        });
    }

    @Override
    public void onRemoved() {
        inFlight.forEach(thrown -> thrown.tnt().remove());
        inFlight.clear();
    }
}
