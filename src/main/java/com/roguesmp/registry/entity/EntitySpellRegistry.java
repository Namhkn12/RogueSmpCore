package com.roguesmp.registry.entity;

import com.roguesmp.entity.boss.primordialslime.PrimordialSlimeAltarSpell;
import com.roguesmp.entity.spell.Spell;
import com.roguesmp.entity.spell.impl.*;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class EntitySpellRegistry {

    private static final Map<String, Spell.SpellParamReader> spellFactories = new HashMap<>();

    static {
        register("self_destruct_spell", SelfDestructSpell::readParam);
        register("slow_aura_spell", SlowAuraSpell::readParam);
        register("dummy_entity_spell", DummyEntitySpell::factory);
        register("fire_aspect_spell", FireAspectSpell::readParam);
        register("ice_aspect_spell", IceAspectSpell::readParam);
        register("blind_spell", BlindSpell::readParam);
        register("self_heal_spell", SelfHealSpell::readParam);
        register("shadow_step_spell", ShadowStepSpell::readParam);
        register("death_grip_spell", DeathGripSpell::readParam);
        register("fire_resistance_spell", FireRestanceSpell::readParam);

        register("primordial_slime_altar_spell", PrimordialSlimeAltarSpell::readParam);
    }

    public static @Nullable Spell createSpell(String id, @Nullable Map<String, Object> param, LivingEntity owner) {
        Spell.SpellParamReader paramReader = spellFactories.get(id);
        if (paramReader != null) return paramReader.fromParams(param, owner);
        return null;
    }

    private static void register(String id, Spell.SpellParamReader reader) {
        spellFactories.put(id, reader);
    }
}
