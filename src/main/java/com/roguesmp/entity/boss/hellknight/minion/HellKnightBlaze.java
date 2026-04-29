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
    public void initialize() {
        if (initialized) return; // Safety check

        // This triggers the logic inside BaseEntity to call startSpell()
        base.processEntity(this.entity);

        startSpell(new SpellManager(List.of(new InfernalBarrageSpell(this, 6, 20, 3, 30))), Collections.emptyList(), 40, null);

        this.initialized = true;
    }
}
