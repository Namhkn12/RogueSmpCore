package com.roguesmp.entity.spell.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.entity.spell.Spell;
import com.roguesmp.entity.spell.SpellParams;
import com.roguesmp.entity.spell.common.SpellTargets;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 * Lets a melee mob land attacks from up to {@code reach} blocks away (vanilla melee reach is
 * roughly 1.5-2 blocks for a zombie-sized mob).
 */
public class ExtendedReachSpell extends Spell {

    public static final String TYPE_KEY = "extended_reach_spell";

    public record Params(double reach, int attackCooldownTicks) implements SpellParams {
        public static final Codec<Params> CODEC = Codec.composite(
                Codec.DOUBLE.optionalFieldOf("reach", 2.5).forGetter(Params::reach),
                Codec.INT.optionalFieldOf("attackCooldownTicks", 20).forGetter(Params::attackCooldownTicks),
                Params::new
        );

        @Override
        public String getTypeId() {
            return TYPE_KEY;
        }
    }

    private final LivingEntity owner;
    private final Params params;
    private int cooldownRemaining;

    public ExtendedReachSpell(LivingEntity owner, Params params) {
        this.owner = owner;
        this.params = params;
    }

    @Override
    public void run(int interval) {
        cooldownRemaining -= interval;
        if (cooldownRemaining > 0) return;

        Player target = SpellTargets.playerTarget(owner);
        if (target == null || !owner.hasLineOfSight(target)) return;
        if (SpellTargets.horizontalDistance(owner.getLocation(), target.getLocation()) > params.reach()) return;
        if (Math.abs(owner.getY() - target.getY()) > params.reach()) return;

        cooldownRemaining = params.attackCooldownTicks();
        owner.swingMainHand();
        owner.attack(target);
    }

    @Override
    public int cooldownTicks() {
        return 0;
    }

    public static ExtendedReachSpell create(Params params, LivingEntity owner) {
        return new ExtendedReachSpell(owner, params);
    }
}
