package com.roguesmp.registry;

import com.roguesmp.entity.spell.Spell;
import com.roguesmp.entity.spell.impl.SelfDestructSpell;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class EntitySpellRegistry {

    private static final Map<String, Supplier<? extends Spell>> noDataSpell = new HashMap<>();
    private static final Map<String, Spell.SpellParamReader> dataSpell = new HashMap<>();

    static {
        registerWithParam("self_destruct", SelfDestructSpell::readParam, () -> new SelfDestructSpell(SelfDestructSpell.DEFAULT_COUNT));
    }

    public static @Nullable Spell getSpellParam(String id, Map<String, Object> param) {
        Spell.SpellParamReader paramReader = dataSpell.get(id);
        if (paramReader != null) return paramReader.fromParams(param);
        return null;
    }

    public static @Nullable Spell getSpellNoParam(String id) {
        Supplier<? extends Spell> supplier = noDataSpell.get(id);
        if (supplier != null) return supplier.get();
        return null;
    }

    private static void registerDefault(String id, Supplier<? extends Spell> supplier) {
        noDataSpell.put(id, supplier);
    }

    private static void registerWithParam(String id, Spell.SpellParamReader reader, Supplier<? extends Spell> defaultSupplier) {
        dataSpell.put(id, reader);
        noDataSpell.put(id, defaultSupplier);
    }
}
