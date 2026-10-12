package com.roguesmp.entity.spell.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.constant.DamageType;
import com.roguesmp.entity.spell.Spell;
import com.roguesmp.entity.spell.SpellParams;
import com.roguesmp.entity.spell.common.CloudSpec;
import com.roguesmp.event.DamageEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.ProjectileHitEvent;

/**
 * Leaves a lingering {@link CloudSpec} under the owner when it's hit in melee, and/or wherever the
 * owner's own projectiles land.
 */
public class ToxicZoneSpell extends Spell {

    public static final String TYPE_KEY = "toxic_zone_spell";

    public record Params(CloudSpec cloud, boolean onMeleeHurt, boolean onProjectileHit, int hurtCooldownTicks) implements SpellParams {
        public static final Codec<Params> CODEC = Codec.composite(
                CloudSpec.CODEC.fieldOf("cloud").forGetter(Params::cloud),
                Codec.BOOLEAN.optionalFieldOf("onMeleeHurt", true).forGetter(Params::onMeleeHurt),
                Codec.BOOLEAN.optionalFieldOf("onProjectileHit", true).forGetter(Params::onProjectileHit),
                Codec.INT.optionalFieldOf("hurtCooldownTicks", 60).forGetter(Params::hurtCooldownTicks),
                Params::new
        );

        @Override
        public String getTypeId() {
            return TYPE_KEY;
        }
    }

    private final LivingEntity owner;
    private final Params params;
    private int nextHurtTriggerTick;

    public ToxicZoneSpell(LivingEntity owner, Params params) {
        this.owner = owner;
        this.params = params;
    }

    @Override
    public void onHurt(DamageEvent event) {
        if (!params.onMeleeHurt() || event.isCancelled() || !DamageType.isMeleeDamage(event.getDamageType())) return;
        int currentTick = Bukkit.getCurrentTick();
        if (currentTick < nextHurtTriggerTick) return;

        nextHurtTriggerTick = currentTick + params.hurtCooldownTicks();
        params.cloud().spawn(owner.getLocation(), owner);
    }

    @Override
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!params.onProjectileHit()) return;
        params.cloud().spawn(event.getEntity().getLocation(), owner);
    }

    @Override
    public void run(int interval) {
    }

    @Override
    public int cooldownTicks() {
        return 0;
    }

    public static ToxicZoneSpell create(Params params, LivingEntity owner) {
        return new ToxicZoneSpell(owner, params);
    }
}
