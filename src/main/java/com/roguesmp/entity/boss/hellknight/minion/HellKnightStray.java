package com.roguesmp.entity.boss.hellknight.minion;

import com.roguesmp.entity.BaseEntity;
import com.roguesmp.entity.spell.SpellManager;
import org.bukkit.entity.LivingEntity;

import java.util.List;

public class HellKnightStray extends HellKnightMinion {
    public HellKnightStray(BaseEntity base, LivingEntity entity) {
        super(base, entity);
    }

    @Override
    protected void onInitialized() {
        spellCasting.startSpell(new SpellManager(List.of(new HellFireBeam(entity, 25, 40, 30))), List.of(new TpAwaySpell(this, 100, 15)), 30);
    }
}
