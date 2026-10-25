package com.roguesmp.entity.boss.cursedforeman;

import com.roguesmp.entity.BaseEntity;
import com.roguesmp.entity.SmpEntity;
import com.roguesmp.entity.component.EntityComponentKeys;
import com.roguesmp.entity.component.impl.PhaseComponent;
import com.roguesmp.entity.component.impl.SpellComponent;
import org.bukkit.entity.LivingEntity;

import java.util.List;
import java.util.Map;

public class CursedForeman extends SmpEntity {

    public static final String ID = "cursed_foreman";

    private static final int FRENZY_THRESHOLD_PERCENT = 40;
    private static final int DETECTION_RANGE = 24;
    private static final int SPELL_START_DELAY = 40;

    public CursedForeman(BaseEntity base, LivingEntity entity) {
        super(base, entity);
        FrenzySpell frenzy = new FrenzySpell(entity);

        setComponent(EntityComponentKeys.SPELLS, new SpellComponent(
                List.of(new PickaxeSlamSpell(entity), new WorkOrderSpell(entity), frenzy),
                DETECTION_RANGE,
                SPELL_START_DELAY,
                false
        ));
        setComponent(EntityComponentKeys.PHASE, new PhaseComponent(Map.of(FRENZY_THRESHOLD_PERCENT, boss -> frenzy.activate()), false));
    }
}
