package com.roguesmp.entity.boss.blackveinhorror;

import com.roguesmp.entity.BaseEntity;
import com.roguesmp.entity.SmpEntity;
import com.roguesmp.entity.component.EntityComponentKeys;
import com.roguesmp.entity.component.impl.SpellComponent;
import org.bukkit.entity.LivingEntity;

import java.util.List;

public class BlackveinHorror extends SmpEntity {

    public static final String ID = "blackvein_horror";

    private static final int DETECTION_RANGE = 24;
    private static final int SPELL_START_DELAY = 40;

    public BlackveinHorror(BaseEntity base, LivingEntity entity) {
        super(base, entity);
        setComponent(EntityComponentKeys.SPELLS, new SpellComponent(
                List.of(new OrequakeSpell(entity), new CorruptedGroundSpell(entity)),
                DETECTION_RANGE,
                SPELL_START_DELAY,
                false
        ));
    }
}
