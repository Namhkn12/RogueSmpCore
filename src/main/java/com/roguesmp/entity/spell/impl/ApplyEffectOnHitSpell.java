package com.roguesmp.entity.spell.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.entity.spell.SpellParams;
import com.roguesmp.entity.spell.common.HitCondition;
import com.roguesmp.entity.spell.common.OnHitSpell;
import com.roguesmp.entity.spell.common.PotionEffectSpec;
import com.roguesmp.event.DamageEvent;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ApplyEffectOnHitSpell extends OnHitSpell {

    private static final List<PotionEffectSpec> DEFAULT_EFFECTS = List.of(new PotionEffectSpec(PotionEffectType.POISON, 60, 0));

    public record Params(HitCondition condition, List<PotionEffectSpec> effects, boolean firstHitOnly) implements SpellParams {
        public static final Codec<Params> CODEC = Codec.composite(
                HitCondition.MAP_CODEC.forGetter(Params::condition),
                PotionEffectSpec.LIST_CODEC.optionalFieldOf("effects", DEFAULT_EFFECTS).forGetter(Params::effects),
                Codec.BOOLEAN.optionalFieldOf("firstHitOnly", false).forGetter(Params::firstHitOnly),
                Params::new
        );
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
}
