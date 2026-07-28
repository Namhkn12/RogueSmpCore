package com.roguesmp.entity.boss.hellknight.minion;

import com.roguesmp.effect.EffectManager;
import com.roguesmp.effect.impl.SpeedEffect;
import com.roguesmp.entity.BaseEntity;
import com.roguesmp.entity.spell.SpellManager;
import com.roguesmp.event.DamageEvent;
import org.bukkit.entity.LivingEntity;

import java.util.List;

public class HellKnightEvoker extends HellKnightMinion {

    public HellKnightEvoker(BaseEntity base, LivingEntity entity) {
        super(base, entity);
    }

    @Override
    protected void onInitialized() {
        spellCasting.startSpell(SpellManager.EMPTY, List.of(new SummonHordeSpell(entity, 6, "hell_knight_hordes")), 30);
    }

    @Override
    public void onDamage(DamageEvent event) {
        EffectManager.getInstance().addEffect(event.getVictim(), "hk_evoker_slow", new SpeedEffect(30, -0.2, "hk_evoker_slow"));
    }
}
