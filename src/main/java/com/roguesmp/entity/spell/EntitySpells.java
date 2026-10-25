package com.roguesmp.entity.spell;

import com.roguesmp.codec.Codec;
import com.roguesmp.codec.DataResult;
import com.roguesmp.codec.JsonOps;
import com.roguesmp.codec.MapCodec;
import com.roguesmp.entity.spell.impl.*;
import com.roguesmp.registry.Registries;
import org.bukkit.entity.LivingEntity;

import java.util.function.BiFunction;
import java.util.function.Function;

public class EntitySpells {

    public static final SpellType<SelfDestructSpell.Params> SELF_DESTRUCT =
            register("self_destruct_spell", SelfDestructSpell.Params.CODEC, (owner, params) -> new SelfDestructSpell(params.particleCount()));
    public static final SpellType<NoParams> SLOW_AURA =
            registerSimple("slow_aura_spell", SlowAuraSpell::new);
    public static final SpellType<NoParams> DUMMY_ENTITY =
            registerSimple("dummy_entity_spell", owner -> new DummyEntitySpell());
    public static final SpellType<NoParams> FIRE_ASPECT =
            registerSimple("fire_aspect_spell", owner -> new FireAspectSpell());
    public static final SpellType<NoParams> ICE_ASPECT =
            registerSimple("ice_aspect_spell", IceAspectSpell::new);
    public static final SpellType<NoParams> BLIND =
            registerSimple("blind_spell", BlindSpell::new);
    public static final SpellType<NoParams> SELF_HEAL =
            registerSimple("self_heal_spell", SelfHealSpell::new);
    public static final SpellType<NoParams> SHADOW_STEP =
            registerSimple("shadow_step_spell", ShadowStepSpell::new);
    public static final SpellType<NoParams> DEATH_GRIP =
            registerSimple("death_grip_spell", DeathGripSpell::new);
    public static final SpellType<NoParams> FIRE_RESISTANCE =
            registerSimple("fire_resistance_spell", FireRestanceSpell::new);
    public static final SpellType<ApplyEffectOnHitSpell.Params> APPLY_EFFECT_ON_HIT =
            register("apply_effect_on_hit_spell", ApplyEffectOnHitSpell.Params.CODEC, ApplyEffectOnHitSpell::new);
    public static final SpellType<BleedOnHitSpell.Params> BLEED_ON_HIT =
            register(BleedOnHitSpell.TYPE_KEY, BleedOnHitSpell.Params.CODEC, BleedOnHitSpell::new);
    public static final SpellType<ArmorShredOnHitSpell.Params> ARMOR_SHRED_ON_HIT =
            register("armor_shred_on_hit_spell", ArmorShredOnHitSpell.Params.CODEC, ArmorShredOnHitSpell::new);
    public static final SpellType<HungerDrainOnHitSpell.Params> HUNGER_DRAIN_ON_HIT =
            register("hunger_drain_on_hit_spell", HungerDrainOnHitSpell.Params.CODEC, HungerDrainOnHitSpell::new);
    public static final SpellType<LifestealOnHitSpell.Params> LIFESTEAL_ON_HIT =
            register("lifesteal_on_hit_spell", LifestealOnHitSpell.Params.CODEC, LifestealOnHitSpell::new);
    public static final SpellType<KnockbackOnHitSpell.Params> KNOCKBACK_ON_HIT =
            register("knockback_on_hit_spell", KnockbackOnHitSpell.Params.CODEC, KnockbackOnHitSpell::new);
    public static final SpellType<SuffocateOnHitSpell.Params> SUFFOCATE_ON_HIT =
            register(SuffocateOnHitSpell.TYPE_KEY, SuffocateOnHitSpell.Params.CODEC, SuffocateOnHitSpell::new);
    public static final SpellType<ThornsSpell.Params> THORNS =
            register(ThornsSpell.TYPE_KEY, ThornsSpell.Params.CODEC, ThornsSpell::new);
    public static final SpellType<DeathCloudSpell.Params> DEATH_CLOUD =
            register("death_cloud_spell", DeathCloudSpell.Params.CODEC, DeathCloudSpell::new);
    public static final SpellType<ToxicZoneSpell.Params> TOXIC_ZONE =
            register("toxic_zone_spell", ToxicZoneSpell.Params.CODEC, ToxicZoneSpell::new);
    public static final SpellType<SpeedBurstSpell.Params> SPEED_BURST =
            register("speed_burst_spell", SpeedBurstSpell.Params.CODEC, SpeedBurstSpell::new);
    public static final SpellType<ProximityDetonateSpell.Params> PROXIMITY_DETONATE =
            register(ProximityDetonateSpell.TYPE_KEY, ProximityDetonateSpell.Params.CODEC, ProximityDetonateSpell::new);
    public static final SpellType<ChargeSpell.Params> CHARGE =
            register(ChargeSpell.TYPE_KEY, ChargeSpell.Params.CODEC, ChargeSpell::new);
    public static final SpellType<ExtendedReachSpell.Params> EXTENDED_REACH =
            register("extended_reach_spell", ExtendedReachSpell.Params.CODEC, ExtendedReachSpell::new);
    public static final SpellType<EffectAuraSpell.Params> EFFECT_AURA =
            register("effect_aura_spell", EffectAuraSpell.Params.CODEC, EffectAuraSpell::new);
    public static final SpellType<HomingProjectileSpell.Params> HOMING_PROJECTILE =
            register("homing_projectile_spell", HomingProjectileSpell.Params.CODEC, HomingProjectileSpell::new);
    public static final SpellType<ThrowTntSpell.Params> THROW_TNT =
            register(ThrowTntSpell.TYPE_KEY, ThrowTntSpell.Params.CODEC, ThrowTntSpell::new);
    public static final SpellType<UndermineSpell.Params> UNDERMINE =
            register("undermine_spell", UndermineSpell.Params.CODEC, UndermineSpell::new);
    public static final SpellType<HealAlliesSpell.Params> HEAL_ALLIES =
            register("heal_allies_spell", HealAlliesSpell.Params.CODEC, HealAlliesSpell::new);
//    public static final SpellType<PrimordialSlimeAltarSpell.Params> PRIMORDIAL_SLIME_ALTAR =
//            register(PrimordialSlimeAltarSpell.TYPE_KEY, PrimordialSlimeAltarSpell.Params.CODEC, PrimordialSlimeAltarSpell::new);

    public static void loadClass() {

    }

    private static <P extends SpellParams> SpellType<P> register(String id, Codec<P> paramsCodec, BiFunction<LivingEntity, P, Spell> factory) {
        DataResult<P> defaults = paramsCodec.decode(JsonOps.INSTANCE.emptyMap(), JsonOps.INSTANCE);
        if (!defaults.isSuccess()) {
            throw new IllegalStateException("Spell '" + id + "' has no default params: " + defaults.error());
        }
        return Registries.ENTITY_SPELL.register(id, new SpellType<>(id, paramsCodec, factory, defaults.result()));
    }

    private static SpellType<NoParams> registerSimple(String id, Function<LivingEntity, Spell> factory) {
        Codec<NoParams> codec = MapCodec.unit(() -> NoParams.INSTANCE).codec();
        return Registries.ENTITY_SPELL.register(id, new SpellType<>(id, codec, (owner, ignored) -> factory.apply(owner), NoParams.INSTANCE));
    }
}
