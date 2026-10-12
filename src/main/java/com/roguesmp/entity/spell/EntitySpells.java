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

    public static final SpellType<ApplyEffectOnHitSpell.Params> APPLY_EFFECT_ON_HIT =
            register(ApplyEffectOnHitSpell.TYPE_KEY, ApplyEffectOnHitSpell.Params.CODEC, ApplyEffectOnHitSpell::create, SpellUsage.PASSIVE);
    public static final SpellType<BleedOnHitSpell.Params> BLEED_ON_HIT =
            register(BleedOnHitSpell.TYPE_KEY, BleedOnHitSpell.Params.CODEC, BleedOnHitSpell::create, SpellUsage.PASSIVE);
    public static final SpellType<ArmorShredOnHitSpell.Params> ARMOR_SHRED_ON_HIT =
            register(ArmorShredOnHitSpell.TYPE_KEY, ArmorShredOnHitSpell.Params.CODEC, ArmorShredOnHitSpell::create, SpellUsage.PASSIVE);
    public static final SpellType<HungerDrainOnHitSpell.Params> HUNGER_DRAIN_ON_HIT =
            register(HungerDrainOnHitSpell.TYPE_KEY, HungerDrainOnHitSpell.Params.CODEC, HungerDrainOnHitSpell::create, SpellUsage.PASSIVE);
    public static final SpellType<LifestealOnHitSpell.Params> LIFESTEAL_ON_HIT =
            register(LifestealOnHitSpell.TYPE_KEY, LifestealOnHitSpell.Params.CODEC, LifestealOnHitSpell::create, SpellUsage.PASSIVE);
    public static final SpellType<KnockbackOnHitSpell.Params> KNOCKBACK_ON_HIT =
            register(KnockbackOnHitSpell.TYPE_KEY, KnockbackOnHitSpell.Params.CODEC, KnockbackOnHitSpell::create, SpellUsage.PASSIVE);
    public static final SpellType<SuffocateOnHitSpell.Params> SUFFOCATE_ON_HIT =
            register(SuffocateOnHitSpell.TYPE_KEY, SuffocateOnHitSpell.Params.CODEC, SuffocateOnHitSpell::create, SpellUsage.PASSIVE);
    public static final SpellType<ThornsSpell.Params> THORNS =
            register(ThornsSpell.TYPE_KEY, ThornsSpell.Params.CODEC, ThornsSpell::create, SpellUsage.PASSIVE);
    public static final SpellType<DeathCloudSpell.Params> DEATH_CLOUD =
            register(DeathCloudSpell.TYPE_KEY, DeathCloudSpell.Params.CODEC, DeathCloudSpell::create, SpellUsage.PASSIVE);
    public static final SpellType<ToxicZoneSpell.Params> TOXIC_ZONE =
            register(ToxicZoneSpell.TYPE_KEY, ToxicZoneSpell.Params.CODEC, ToxicZoneSpell::create, SpellUsage.PASSIVE);
    public static final SpellType<SpeedBurstSpell.Params> SPEED_BURST =
            register(SpeedBurstSpell.TYPE_KEY, SpeedBurstSpell.Params.CODEC, SpeedBurstSpell::create, SpellUsage.PASSIVE);
    public static final SpellType<ProximityDetonateSpell.Params> PROXIMITY_DETONATE =
            register(ProximityDetonateSpell.TYPE_KEY, ProximityDetonateSpell.Params.CODEC, ProximityDetonateSpell::create, SpellUsage.PASSIVE);
    public static final SpellType<ChargeSpell.Params> CHARGE =
            register(ChargeSpell.TYPE_KEY, ChargeSpell.Params.CODEC, ChargeSpell::create, SpellUsage.PASSIVE);
    public static final SpellType<ExtendedReachSpell.Params> EXTENDED_REACH =
            register(ExtendedReachSpell.TYPE_KEY, ExtendedReachSpell.Params.CODEC, ExtendedReachSpell::create, SpellUsage.PASSIVE);
    public static final SpellType<EffectAuraSpell.Params> EFFECT_AURA =
            register(EffectAuraSpell.TYPE_KEY, EffectAuraSpell.Params.CODEC, EffectAuraSpell::create, SpellUsage.PASSIVE);
    public static final SpellType<HomingProjectileSpell.Params> HOMING_PROJECTILE =
            register(HomingProjectileSpell.TYPE_KEY, HomingProjectileSpell.Params.CODEC, HomingProjectileSpell::create, SpellUsage.PASSIVE);
    public static final SpellType<ThrowTntSpell.Params> THROW_TNT =
            register(ThrowTntSpell.TYPE_KEY, ThrowTntSpell.Params.CODEC, ThrowTntSpell::create, SpellUsage.ACTIVE);
    public static final SpellType<UndermineSpell.Params> UNDERMINE =
            register(UndermineSpell.TYPE_KEY, UndermineSpell.Params.CODEC, UndermineSpell::create, SpellUsage.ACTIVE);
    public static final SpellType<HealAlliesSpell.Params> HEAL_ALLIES =
            register(HealAlliesSpell.TYPE_KEY, HealAlliesSpell.Params.CODEC, HealAlliesSpell::create, SpellUsage.ACTIVE);
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
