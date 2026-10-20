package com.roguesmp.effect.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.effect.SmpEffect;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;


public class SpeedEffect extends SmpEffect {
    public static final String EFFECT_ID = "speed";

    public static final Codec<SpeedEffect> CODEC = Codec.composite(
            SmpEffect.BASE_CODEC.forGetter(SmpEffect::getBaseProperties),
            Codec.DOUBLE.fieldOf("value").forGetter(SpeedEffect::getMagnitude),
            SpeedEffect::new
    );

    private final double value;

    public SpeedEffect(int duration, double value, DeathBehavior behavior) {
        super(EFFECT_ID, duration, behavior);
        this.value = value;
    }

    public SpeedEffect(int duration, double value) {
        super(EFFECT_ID, duration);
        this.value = value;
    }

    public SpeedEffect(BaseProperties baseProperties, double value) {
        super(EFFECT_ID, baseProperties);
        this.value = value;
    }

    @Override
    public double getMagnitude() {
        return value;
    }

    @Override
    protected boolean isPersistentByDefault() {
        return true;
    }

    @Override
    public @Nullable Component getDisplayComponent() {
        if (value <= 0) {
            return Component.text(Utils.formatDecimal(value * 100) + "% tốc chạy", NamedTextColor.RED);
        }
        return Component.text(Utils.formatDecimal(value * 100) + "% tốc chạy", NamedTextColor.GREEN);
    }

    @Override
    public void onGainEffect(Entity entity) {
        if (entity instanceof LivingEntity le) {
            AttributeInstance speedInstance = le.getAttribute(Attribute.MOVEMENT_SPEED);
            if (speedInstance != null) {
                NamespacedKey key = sourceKey();
                speedInstance.removeModifier(key);
                speedInstance.addTransientModifier(new AttributeModifier(key, value, AttributeModifier.Operation.ADD_SCALAR));
            }
        }
    }

    @Override
    public void onLoseEffect(Entity entity) {
        if (entity instanceof LivingEntity le) {
            AttributeInstance speedInstance = le.getAttribute(Attribute.MOVEMENT_SPEED);
            if (speedInstance != null) {
                speedInstance.removeModifier(sourceKey());
            }
        }
    }
}
