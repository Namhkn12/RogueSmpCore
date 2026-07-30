package com.roguesmp.entity.spell;

import com.roguesmp.codec.Codec;
import com.roguesmp.entity.boss.primordialslime.PrimordialSlimeAltarSpell;
import com.roguesmp.entity.spell.impl.*;
import com.roguesmp.registry.Registries;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;

/**
 * Entity spell factories, keyed by {@link SpellParams} type id. Each constant registers itself
 * into {@link Registries#ENTITY_SPELL} as it's initialized - call {@link #loadClass()} to force
 * that to happen.
 */
public class EntitySpells {

    public static final SpellFactory<SelfDestructSpell.Params> SELF_DESTRUCT =
            register(SelfDestructSpell.TYPE_KEY, SelfDestructSpell.Params.CODEC, SelfDestructSpell::create);
    public static final SpellFactory<SlowAuraSpell.Params> SLOW_AURA =
            register(SlowAuraSpell.TYPE_KEY, SlowAuraSpell.Params.CODEC, SlowAuraSpell::create);
    public static final SpellFactory<DummyEntitySpell.Params> DUMMY_ENTITY =
            register(DummyEntitySpell.TYPE_KEY, DummyEntitySpell.Params.CODEC, DummyEntitySpell::create);
    public static final SpellFactory<FireAspectSpell.Params> FIRE_ASPECT =
            register(FireAspectSpell.TYPE_KEY, FireAspectSpell.Params.CODEC, FireAspectSpell::create);
    public static final SpellFactory<IceAspectSpell.Params> ICE_ASPECT =
            register(IceAspectSpell.TYPE_KEY, IceAspectSpell.Params.CODEC, IceAspectSpell::create);
    public static final SpellFactory<BlindSpell.Params> BLIND =
            register(BlindSpell.TYPE_KEY, BlindSpell.Params.CODEC, BlindSpell::create);
    public static final SpellFactory<SelfHealSpell.Params> SELF_HEAL =
            register(SelfHealSpell.TYPE_KEY, SelfHealSpell.Params.CODEC, SelfHealSpell::create);
    public static final SpellFactory<ShadowStepSpell.Params> SHADOW_STEP =
            register(ShadowStepSpell.TYPE_KEY, ShadowStepSpell.Params.CODEC, ShadowStepSpell::create);
    public static final SpellFactory<DeathGripSpell.Params> DEATH_GRIP =
            register(DeathGripSpell.TYPE_KEY, DeathGripSpell.Params.CODEC, DeathGripSpell::create);
    public static final SpellFactory<FireRestanceSpell.Params> FIRE_RESISTANCE =
            register(FireRestanceSpell.TYPE_KEY, FireRestanceSpell.Params.CODEC, FireRestanceSpell::create);
    public static final SpellFactory<PrimordialSlimeAltarSpell.Params> PRIMORDIAL_SLIME_ALTAR =
            register(PrimordialSlimeAltarSpell.TYPE_KEY, PrimordialSlimeAltarSpell.Params.CODEC, PrimordialSlimeAltarSpell::create);

    public static void loadClass() {

    }

    /**
     * The one unavoidable unsafe cast: {@code params}'s concrete type is only known at runtime
     * (looked up by string id), so it can't be proven to match the {@code SpellFactory<P>} found
     * for that id without a cast. {@link SpellFactory#createSpell} itself stays fully type-safe.
     */
    @SuppressWarnings("unchecked")
    public static @Nullable Spell createSpell(SpellParams params, LivingEntity owner) {
        SpellFactory<SpellParams> factory = (SpellFactory<SpellParams>) Registries.ENTITY_SPELL.get(params.getTypeId());
        return factory != null ? factory.createSpell(params, owner) : null;
    }

    private static <P extends SpellParams> SpellFactory<P> register(String id, Codec<P> paramsCodec, BiFunction<P, LivingEntity, Spell> factory) {
        SpellFactory<P> spellFactory = new SpellFactory<>(paramsCodec, factory);
        Registries.ENTITY_SPELL.register(id, spellFactory);
        return spellFactory;
    }
}
