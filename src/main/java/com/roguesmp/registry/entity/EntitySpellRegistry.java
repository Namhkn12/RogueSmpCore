package com.roguesmp.registry.entity;

import com.roguesmp.entity.boss.primordialslime.PrimordialSlimeAltarSpell;
import com.roguesmp.entity.spell.Spell;
import com.roguesmp.entity.spell.impl.DummyEntitySpell;
import com.roguesmp.entity.spell.impl.SelfDestructSpell;
import com.roguesmp.entity.spell.impl.SlowAuraSpell;
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
