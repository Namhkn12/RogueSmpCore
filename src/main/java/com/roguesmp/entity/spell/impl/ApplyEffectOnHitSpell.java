package com.roguesmp.entity.spell.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.entity.spell.SpellParams;
import com.roguesmp.entity.spell.common.HitCondition;
import com.roguesmp.entity.spell.common.OnHitSpell;
import com.roguesmp.entity.spell.common.PotionEffectSpec;
import com.roguesmp.event.DamageEvent;
import org.bukkit.entity.LivingEntity;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ApplyEffectOnHitSpell extends OnHitSpell {

    public static final String TYPE_KEY = "apply_effect_on_hit_spell";

    public record Params(HitCondition condition, List<PotionEffectSpec> effects, boolean firstHitOnly) implements SpellParams {
        public static final Codec<Params> CODEC = Codec.composite(
                HitCondition.MAP_CODEC.forGetter(Params::condition),
                PotionEffectSpec.LIST_CODEC.fieldOf("effects").forGetter(Params::effects),
                Codec.BOOLEAN.optionalFieldOf("firstHitOnly", false).forGetter(Params::firstHitOnly),
                Params::new
        );

        @Override
        public String getTypeId() {
            return TYPE_KEY;
        }
    }

    private final List<PotionEffectSpec> effects;
    private final boolean firstHitOnly;
    private final Set<UUID> alreadyHit = new HashSet<>();

    public ApplyEffectOnHitSpell(LivingEntity owner, Params params) {
        super(owner, params.condition());
        this.effects = params.effects();
        this.firstHitOnly = params.firstHitOnly();
    }

    @Override
    protected void onHit(LivingEntity victim, DamageEvent event) {
        if (firstHitOnly && !alreadyHit.add(victim.getUniqueId())) return;
        PotionEffectSpec.applyAll(victim, effects);
    }

    public static ApplyEffectOnHitSpell create(Params params, LivingEntity owner) {
        return new ApplyEffectOnHitSpell(owner, params);
    }
}
