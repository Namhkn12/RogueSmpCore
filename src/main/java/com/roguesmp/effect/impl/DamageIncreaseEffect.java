package com.roguesmp.effect.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.constant.DamageOperation;
import com.roguesmp.effect.SmpEffect;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.Nullable;

public class DamageIncreaseEffect extends SmpEffect {

    public static final Codec<DamageIncreaseEffect> CODEC = Codec.composite(
            SmpEffect.BASE_CODEC.forGetter(SmpEffect::getBaseProperties),
            Codec.DOUBLE.fieldOf("increase_value").forGetter(DamageIncreaseEffect::getMagnitude),
            DamageIncreaseEffect::new
    );

    public static final String ID = "damage_increase";

    private final double increaseValue;

    public DamageIncreaseEffect(int duration, double increaseValue) {
        super(ID, duration);
        this.increaseValue = increaseValue;
    }

    public DamageIncreaseEffect(BaseProperties base, double increaseValue) {
        super(ID, base);
        this.increaseValue = increaseValue;
    }

    @Override
    public double getMagnitude() {
        return increaseValue;
    }

    @Override
    public boolean isPersistent() {
        return false;
    }

    @Override
    public @Nullable Component getDisplayComponent() {
        if (increaseValue <= 0) return Component.text(Utils.formatDecimal(increaseValue * 100) + "% sát thương", NamedTextColor.RED);
        return Component.text(Utils.formatDecimal(increaseValue * 100) + "% sát thương", NamedTextColor.GREEN);
    }

    @Override
    public void onDamageEntity(DamageEvent event) {
        event.addDamageModifier(increaseValue, DamageOperation.INCREASE_BASE);
    }
}
