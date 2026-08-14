package com.roguesmp.entity.spell;

import com.roguesmp.codec.Codec;
import com.roguesmp.entity.spell.impl.*;
import com.roguesmp.registry.Registries;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;

/**
 * Entity spell types, keyed by {@link SpellParams} type id. Each constant registers itself
 * into {@link Registries#ENTITY_SPELL} as it's initialized - call {@link #loadClass()} to force
 * that to happen.
 */
public class EntitySpells {

    public static final SpellType<SelfDestructSpell.Params> SELF_DESTRUCT =
            register(SelfDestructSpell.TYPE_KEY, SelfDestructSpell.Params.CODEC, SelfDestructSpell::create, SpellUsage.PASSIVE);
    public static final SpellType<SlowAuraSpell.Params> SLOW_AURA =
            register(SlowAuraSpell.TYPE_KEY, SlowAuraSpell.Params.CODEC, SlowAuraSpell::create, SpellUsage.PASSIVE);
    public static final SpellType<DummyEntitySpell.Params> DUMMY_ENTITY =
            register(DummyEntitySpell.TYPE_KEY, DummyEntitySpell.Params.CODEC, DummyEntitySpell::create, SpellUsage.EITHER);
    public static final SpellType<FireAspectSpell.Params> FIRE_ASPECT =
            register(FireAspectSpell.TYPE_KEY, FireAspectSpell.Params.CODEC, FireAspectSpell::create, SpellUsage.PASSIVE);
    public static final SpellType<IceAspectSpell.Params> ICE_ASPECT =
            register(IceAspectSpell.TYPE_KEY, IceAspectSpell.Params.CODEC, IceAspectSpell::create, SpellUsage.PASSIVE);
    public static final SpellType<BlindSpell.Params> BLIND =
            register(BlindSpell.TYPE_KEY, BlindSpell.Params.CODEC, BlindSpell::create, SpellUsage.PASSIVE);
    public static final SpellType<SelfHealSpell.Params> SELF_HEAL =
            register(SelfHealSpell.TYPE_KEY, SelfHealSpell.Params.CODEC, SelfHealSpell::create, SpellUsage.ACTIVE);
    public static final SpellType<ShadowStepSpell.Params> SHADOW_STEP =
            register(ShadowStepSpell.TYPE_KEY, ShadowStepSpell.Params.CODEC, ShadowStepSpell::create, SpellUsage.ACTIVE);
    public static final SpellType<DeathGripSpell.Params> DEATH_GRIP =
            register(DeathGripSpell.TYPE_KEY, DeathGripSpell.Params.CODEC, DeathGripSpell::create, SpellUsage.ACTIVE);
    public static final SpellType<FireRestanceSpell.Params> FIRE_RESISTANCE =
            register(FireRestanceSpell.TYPE_KEY, FireRestanceSpell.Params.CODEC, FireRestanceSpell::create, SpellUsage.PASSIVE);
//    public static final SpellType<PrimordialSlimeAltarSpell.Params> PRIMORDIAL_SLIME_ALTAR =
//            register(PrimordialSlimeAltarSpell.TYPE_KEY, PrimordialSlimeAltarSpell.Params.CODEC, PrimordialSlimeAltarSpell::create, SpellUsage.PASSIVE);

    public static void loadClass() {

    }

    /**
     * The one unavoidable unsafe cast: {@code params}'s concrete type is only known at runtime
     * (looked up by string id), so it can't be proven to match the {@code SpellType<P>} found
     * for that id without a cast. {@link SpellType#createSpell} itself stays fully type-safe.
     */
    @SuppressWarnings("unchecked")
    public static @Nullable Spell createSpell(SpellParams params, LivingEntity owner) {
        SpellType<SpellParams> type = (SpellType<SpellParams>) Registries.ENTITY_SPELL.get(params.getTypeId());
        return type != null ? type.createSpell(params, owner) : null;
    }

    private static <P extends SpellParams> SpellType<P> register(String id, Codec<P> paramsCodec, BiFunction<P, LivingEntity, Spell> factory, SpellUsage usage) {
        SpellType<P> spellType = new SpellType<>(paramsCodec, factory, usage);
        Registries.ENTITY_SPELL.register(id, spellType);
        return spellType;
    }
}
