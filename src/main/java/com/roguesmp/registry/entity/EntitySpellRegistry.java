package com.roguesmp.registry.entity;

import com.roguesmp.codec.Codec;
import com.roguesmp.entity.boss.primordialslime.PrimordialSlimeAltarSpell;
import com.roguesmp.entity.spell.Spell;
import com.roguesmp.entity.spell.SpellFactory;
import com.roguesmp.entity.spell.SpellParams;
import com.roguesmp.entity.spell.impl.*;
import com.roguesmp.registry.Registries;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;

public class EntitySpellRegistry {

    public static void bootstrap() {
        register(SelfDestructSpell.TYPE_KEY, SelfDestructSpell.Params.CODEC, SelfDestructSpell::create);
        register(SlowAuraSpell.TYPE_KEY, SlowAuraSpell.Params.CODEC, SlowAuraSpell::create);
        register(DummyEntitySpell.TYPE_KEY, DummyEntitySpell.Params.CODEC, DummyEntitySpell::create);
        register(FireAspectSpell.TYPE_KEY, FireAspectSpell.Params.CODEC, FireAspectSpell::create);
        register(IceAspectSpell.TYPE_KEY, IceAspectSpell.Params.CODEC, IceAspectSpell::create);
        register(BlindSpell.TYPE_KEY, BlindSpell.Params.CODEC, BlindSpell::create);
        register(SelfHealSpell.TYPE_KEY, SelfHealSpell.Params.CODEC, SelfHealSpell::create);
        register(ShadowStepSpell.TYPE_KEY, ShadowStepSpell.Params.CODEC, ShadowStepSpell::create);
        register(DeathGripSpell.TYPE_KEY, DeathGripSpell.Params.CODEC, DeathGripSpell::create);
        register(FireRestanceSpell.TYPE_KEY, FireRestanceSpell.Params.CODEC, FireRestanceSpell::create);

        register(PrimordialSlimeAltarSpell.TYPE_KEY, PrimordialSlimeAltarSpell.Params.CODEC, PrimordialSlimeAltarSpell::create);
    }

    private static <P extends SpellParams> void register(String id, Codec<P> paramsCodec, BiFunction<P, LivingEntity, Spell> factory) {
        Registries.ENTITY_SPELL.register(id, new SpellFactory<>(paramsCodec, factory));
    }

    public static @Nullable Spell createSpell(SpellParams params, LivingEntity owner) {
        SpellFactory<?> factory = Registries.ENTITY_SPELL.get(params.getTypeId());
        return factory != null ? factory.createSpell(params, owner) : null;
    }
}
