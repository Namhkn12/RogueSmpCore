package com.roguesmp.entity.spell.common;

import com.roguesmp.effect.EffectManager;
import com.roguesmp.effect.impl.VanillaPotionEffect;
import com.roguesmp.codec.Codec;
import com.roguesmp.codec.DataResult;
import net.kyori.adventure.key.Key;
import org.bukkit.Registry;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

/**
 * A vanilla potion effect declared in spell JSON, e.g. {@code {"type": "poison", "duration": 100, "amplifier": 1}}.
 */
public record PotionEffectSpec(PotionEffectType type, int duration, int amplifier) {

    private static final Codec<PotionEffectType> TYPE_CODEC = Codec.STRING.comapFlatMap(
            name -> {
                PotionEffectType type = Registry.MOB_EFFECT.get(Key.key(name.toLowerCase()));
                return type != null ? DataResult.success(type) : DataResult.error("Unknown potion effect: " + name);
            },
            type -> type.getKey().asString()
    );

    public static final Codec<PotionEffectSpec> CODEC = Codec.composite(
            TYPE_CODEC.fieldOf("type").forGetter(PotionEffectSpec::type),
            Codec.INT.fieldOf("duration").forGetter(PotionEffectSpec::duration),
            Codec.INT.optionalFieldOf("amplifier", 0).forGetter(PotionEffectSpec::amplifier),
            PotionEffectSpec::new
    );

    public static final Codec<List<PotionEffectSpec>> LIST_CODEC = Codec.listOf(CODEC);

    public PotionEffect toPotionEffect() {
        return new PotionEffect(type, duration, amplifier);
    }

    public static void applyAll(LivingEntity target, List<PotionEffectSpec> effects) {
        effects.forEach(effect -> EffectManager.getInstance().addEffect(target, VanillaPotionEffect.sourceOf(effect.type()),
                new VanillaPotionEffect(effect.duration(), effect.type(), effect.amplifier())));
    }
}
