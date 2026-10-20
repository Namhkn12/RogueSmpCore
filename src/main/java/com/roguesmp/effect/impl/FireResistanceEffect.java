package com.roguesmp.effect.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.constant.DamageOperation;
import com.roguesmp.constant.DamageType;
import com.roguesmp.effect.SmpEffect;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.Nullable;

public class FireResistanceEffect extends SmpEffect {

    public static final String ID = "fire_resistance";

    public static final Codec<FireResistanceEffect> CODEC = Codec.composite(
            SmpEffect.BASE_CODEC.forGetter(SmpEffect::getBaseProperties),
            Codec.DOUBLE.fieldOf("value").forGetter(FireResistanceEffect::getMagnitude),
            FireResistanceEffect::new
    );

    private final double value;

    public FireResistanceEffect(int duration, double value) {
        super(ID, duration);
        this.value = value;
    }

    public FireResistanceEffect(BaseProperties base, double value) {
        super(ID, base);
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
        return Component.text(Utils.formatDecimal(value * 100) + "% kháng lửa", NamedTextColor.GOLD);
    }

    @Override
    public void onHurt(DamageEvent event) {
        if (event.getDamageType() == DamageType.FIRE) event.addDamageModifier(1 - Math.min(1, value), DamageOperation.MORE_FINAL);
    }
}
