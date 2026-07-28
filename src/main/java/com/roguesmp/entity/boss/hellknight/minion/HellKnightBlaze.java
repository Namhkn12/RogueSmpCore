package com.roguesmp.entity.boss.hellknight.minion;

import com.roguesmp.entity.BaseEntity;
import com.roguesmp.entity.spell.SpellManager;
import org.bukkit.entity.LivingEntity;

import java.util.Collections;
import java.util.List;

public class HellKnightBlaze extends HellKnightMinion {
    public HellKnightBlaze(BaseEntity base, LivingEntity entity) {
        super(base, entity);
    }

    @Override
    protected void onInitialized() {
        spellCasting.startSpell(new SpellManager(List.of(new InfernalBarrageSpell(this, 6, 20, 3, 30))), Collections.emptyList(), 40);
    }
}
