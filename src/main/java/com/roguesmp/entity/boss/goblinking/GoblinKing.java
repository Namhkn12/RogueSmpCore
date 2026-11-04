package com.roguesmp.entity.boss.goblinking;

import com.roguesmp.entity.BaseEntity;
import com.roguesmp.entity.SmpEntity;
import com.roguesmp.entity.component.EntityComponentKeys;
import com.roguesmp.entity.component.impl.PhaseComponent;
import com.roguesmp.entity.component.impl.SpellComponent;
import com.roguesmp.entity.spell.Spell;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;

import com.roguesmp.entity.boss.goblinking.ComboDirector.ComboSpell;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GoblinKing extends SmpEntity {

    public static final String ID = "goblin_king";

    private static final int PHASE_TWO_PERCENT = 66;
    private static final int PHASE_THREE_PERCENT = 33;
    private static final int DETECTION_RANGE = 32;
    private static final int SPELL_START_DELAY = 40;
    private static final int PHASE_SWAP_DELAY = 40;

    private final ComboDirector director;
    private final GoblinCombos combos;
    private final SpellComponent spells;

    public GoblinKing(BaseEntity base, LivingEntity entity) {
        super(base, entity);
        director = new ComboDirector(entity);
        combos = new GoblinCombos(director);
        spells = new SpellComponent(withDirector(combos.phaseOne()), DETECTION_RANGE, SPELL_START_DELAY, false);

        setComponent(EntityComponentKeys.SPELLS, spells);
        setComponent(EntityComponentKeys.PHASE, new PhaseComponent(Map.of(
                PHASE_TWO_PERCENT, boss -> enterPhase(combos.phaseTwo()),
                PHASE_THREE_PERCENT, boss -> enterPhase(combos.phaseThree())
        ), true));
    }

    private List<Spell> withDirector(List<ComboSpell> pool) {
        List<Spell> list = new ArrayList<>(pool);
        list.add(director);
        return list;
    }

    private void enterPhase(List<ComboSpell> pool) {
        spells.setSpells(withDirector(pool), PHASE_SWAP_DELAY);
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_RAVAGER_ROAR, 2.5f, 0.6f);
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_PIGLIN_BRUTE_ANGRY, 2f, 0.5f);
        entity.getWorld().spawnParticle(Particle.FLAME, entity.getLocation().add(0, 1, 0), 60, 0.8, 1, 0.8, 0.05);
    }
}
