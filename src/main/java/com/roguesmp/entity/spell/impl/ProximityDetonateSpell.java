package com.roguesmp.entity.spell.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.entity.spell.Spell;
import com.roguesmp.entity.spell.SpellContext;
import com.roguesmp.entity.spell.SpellParams;
import com.roguesmp.entity.spell.common.ExplosionSpec;
import com.roguesmp.entity.spell.common.PotionEffectSpec;
import com.roguesmp.entity.spell.common.SpellTargets;
import com.roguesmp.utils.PlayerUtils;
import org.bukkit.Location;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Explodes ({@link ExplosionSpec}, scaled off the owner's {@code attack_damage}) once the target is
 * within {@code triggerRange}, applies {@code effects} to players inside the blast, then kills the
 * owner so death spells/loot still run. A creeper owner has its vanilla fuse disabled so this spell
 * fully owns the detonation.
 */
public class ProximityDetonateSpell extends Spell {

    public static final String TYPE_KEY = "proximity_detonate_spell";

    public record Params(double triggerRange, ExplosionSpec explosion, List<PotionEffectSpec> effects) implements SpellParams {
        public static final Codec<Params> CODEC = Codec.composite(
                Codec.DOUBLE.optionalFieldOf("triggerRange", 2.0).forGetter(Params::triggerRange),
                ExplosionSpec.MAP_CODEC.forGetter(Params::explosion),
                PotionEffectSpec.LIST_CODEC.optionalFieldOf("effects", List.of()).forGetter(Params::effects),
                Params::new
        );
    }

    private final LivingEntity owner;
    private final Params params;
    private boolean detonated;

    public ProximityDetonateSpell(LivingEntity owner, Params params) {
        this.owner = owner;
        this.params = params;
        if (owner instanceof Creeper creeper) {
            creeper.setMaxFuseTicks(Integer.MAX_VALUE);
            creeper.setExplosionRadius(0);
        }
    }

    @Override
    public void tick(SpellContext context, int interval) {
        if (detonated || !owner.isValid()) return;

        Player target = SpellTargets.playerTarget(owner);
        if (target == null || target.getLocation().distanceSquared(owner.getLocation()) > params.triggerRange() * params.triggerRange()) return;

        detonate();
    }

    private void detonate() {
        detonated = true;
        Location center = SpellTargets.centerOf(owner);
        params.explosion().detonate(owner, center, TYPE_KEY);
        PlayerUtils.playersInRange(center, params.explosion().radius(), false)
                .forEach(player -> PotionEffectSpec.applyAll(player, params.effects()));
        owner.setHealth(0);
    }
}
