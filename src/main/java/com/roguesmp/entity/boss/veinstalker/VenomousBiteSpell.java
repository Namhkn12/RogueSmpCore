package com.roguesmp.entity.boss.veinstalker;

import com.roguesmp.effect.EffectManager;
import com.roguesmp.effect.SmpEffect;
import com.roguesmp.effect.impl.PotentPoisonEffect;
import com.roguesmp.entity.spell.common.HitCondition;
import com.roguesmp.entity.spell.common.HitTrigger;
import com.roguesmp.entity.spell.common.OnHitSpell;
import com.roguesmp.entity.spell.common.PotionEffectSpec;
import com.roguesmp.event.DamageEvent;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

public class VenomousBiteSpell extends OnHitSpell {

    private static final String SOURCE = "vein_stalker_venom";
    private static final double POISON_DAMAGE_PER_STACK = 1.0;
    private static final List<PotionEffectSpec> HUNGER = List.of(new PotionEffectSpec(PotionEffectType.HUNGER, 160, 1));

    public VenomousBiteSpell(LivingEntity owner) {
        super(owner, new HitCondition(1.0, HitTrigger.MELEE, true));
    }

    @Override
    protected void onHit(LivingEntity victim, DamageEvent event) {
        SmpEffect existing = EffectManager.getInstance().getActiveEffects(victim).get(SOURCE);
        if (existing instanceof PotentPoisonEffect poison) {
            poison.addStack();
        } else {
            PotentPoisonEffect poison = new PotentPoisonEffect(PotentPoisonEffect.DURATION_TICKS, POISON_DAMAGE_PER_STACK, 1);
            poison.setApplier(owner);
            EffectManager.getInstance().addEffect(victim, SOURCE, poison);
        }
        PotionEffectSpec.applyAll(victim, HUNGER);
    }
}
