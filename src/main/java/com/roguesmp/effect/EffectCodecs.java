package com.roguesmp.effect;

import com.roguesmp.codec.Codec;
import com.roguesmp.effect.impl.*;
import com.roguesmp.registry.Registries;

/**
 * Every {@link SmpEffect} codec. Each constant registers itself into
 * {@link Registries#EFFECT_CODEC} as it's initialized - call {@link #loadClass()} to force that to
 * happen.
 */
public class EffectCodecs {

    public static final Codec<SpeedEffect> SPEED = register(SpeedEffect.EFFECT_ID, SpeedEffect.CODEC);
    public static final Codec<DamageIncreaseEffect> DAMAGE_INCREASE = register(DamageIncreaseEffect.ID, DamageIncreaseEffect.CODEC);
    public static final Codec<ResistanceEffect> RESISTANCE = register(ResistanceEffect.ID, ResistanceEffect.CODEC);
    public static final Codec<BleedingEffect> BLEEDING = register(BleedingEffect.ID, BleedingEffect.CODEC);
    public static final Codec<PotentPoisonEffect> POTENT_POISON = register(PotentPoisonEffect.ID, PotentPoisonEffect.CODEC);
    public static final Codec<VulnerabilityEffect> VULNERABILITY = register(VulnerabilityEffect.ID, VulnerabilityEffect.CODEC);
    public static final Codec<SilenceEffect> SILENCE = register(SilenceEffect.ID, SilenceEffect.CODEC);
    public static final Codec<EmpoweredStrikeEffect> EMPOWERED_STRIKE = register(EmpoweredStrikeEffect.ID, EmpoweredStrikeEffect.CODEC);
    public static final Codec<StealthEffect> STEALTH = register(StealthEffect.KEY, StealthEffect.CODEC);
    public static final Codec<RegenerationEffect> REGENERATION = register(RegenerationEffect.ID, RegenerationEffect.CODEC);
    public static final Codec<AbsorptionEffect> ABSORPTION = register(AbsorptionEffect.ID, AbsorptionEffect.CODEC);
    public static final Codec<FireResistanceEffect> FIRE_RESISTANCE = register(FireResistanceEffect.ID, FireResistanceEffect.CODEC);
    public static final Codec<DamageOverTimeEffect> DAMAGE_OVER_TIME = register(DamageOverTimeEffect.ID, DamageOverTimeEffect.CODEC);
    public static final Codec<VanillaPotionEffect> VANILLA_POTION = register(VanillaPotionEffect.ID, VanillaPotionEffect.CODEC);
    public static final Codec<BrewedPotionEffect> BREWED_POTION = register(BrewedPotionEffect.ID, BrewedPotionEffect.CODEC);
    public static final Codec<CurseOfDeathEffect> CURSE_OF_DEATH = register(CurseOfDeathEffect.ID, CurseOfDeathEffect.CODEC);

    public static void loadClass() {

    }

    private static <T extends SmpEffect> Codec<T> register(String id, Codec<T> codec) {
        return Registries.EFFECT_CODEC.register(id, codec);
    }
}
