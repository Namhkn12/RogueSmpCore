package com.roguesmp.effect.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.codec.DataResult;
import com.roguesmp.effect.EffectManager;
import com.roguesmp.effect.SmpEffect;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Registry;
import org.bukkit.entity.Entity;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionEffectTypeCategory;
import org.jetbrains.annotations.Nullable;

/**
 * A wrapper of vanilla potion effect - {@link EffectManager} applies the strongest
 * active request per type to the entity, so sources never overwrite or strip each other.
 */
public class VanillaPotionEffect extends SmpEffect {

    public static final String ID = "vanilla_potion";

    public static final Codec<PotionEffectType> TYPE_CODEC = Codec.KEY.comapFlatMap(
            key -> {
                PotionEffectType type = Registry.MOB_EFFECT.get(key);
                return type != null ? DataResult.success(type) : DataResult.error("Unknown potion effect '" + key.asString() + "'");
            },
            PotionEffectType::key
    );

    public static final Codec<VanillaPotionEffect> CODEC = Codec.composite(
            SmpEffect.BASE_CODEC.forGetter(SmpEffect::getBaseProperties),
            TYPE_CODEC.fieldOf("type").forGetter(VanillaPotionEffect::getType),
            Codec.INT.optionalFieldOf("amplifier", 0).forGetter(VanillaPotionEffect::getAmplifier),
            VanillaPotionEffect::new
    );

    private final PotionEffectType type;
    private final int amplifier;

    public VanillaPotionEffect(int duration, PotionEffectType type, int amplifier) {
        super(ID, duration);
        this.type = type;
        this.amplifier = amplifier;
    }

    public VanillaPotionEffect(BaseProperties base, PotionEffectType type, int amplifier) {
        super(ID, base);
        this.type = type;
        this.amplifier = amplifier;
    }

    public static String sourceOf(PotionEffectType type) {
        return "vanilla/" + type.key().value();
    }

    public PotionEffectType getType() {
        return type;
    }

    public int getAmplifier() {
        return amplifier;
    }

    @Override
    public double getMagnitude() {
        return amplifier;
    }

    @Override
    protected boolean isPersistentByDefault() {
        return true;
    }

    @Override
    public @Nullable Component getDisplayComponent() {
        NamedTextColor color = type.getCategory() == PotionEffectTypeCategory.HARMFUL ? NamedTextColor.RED : NamedTextColor.AQUA;
        Component name = Component.translatable(type.translationKey(), color);
        return amplifier <= 0 ? name : name.append(Utils.text(" " + Utils.toRoman(amplifier + 1), color));
    }

    @Override
    public void onGainEffect(Entity entity) {
        EffectManager.getInstance().requestVanilla(entity, getSource(), type, amplifier);
    }

    @Override
    public void onLoseEffect(Entity entity) {
        EffectManager.getInstance().releaseVanilla(entity, getSource(), type);
    }
}
