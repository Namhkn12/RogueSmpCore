package com.roguesmp.effect.impl;

import com.roguesmp.constant.DamageOperation;
import com.roguesmp.effect.SmpEffect;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.Nullable;

public class ResistanceEffect extends SmpEffect {

    public static final String ID = "resistance_effect";

    private final double value;

    /**
     * @param value Value used for percentage (0.4, 0.5, etc...)
     */
    public ResistanceEffect(int duration, double value, DeathBehavior deathBehavior) {
        super(duration, ID, deathBehavior);
        this.value = value;
    }

    @Override
    public double getMagnitude() {
        return value;
    }

    @Override
    public boolean isPersistent() {
        return true;
    }

    @Override
    public @Nullable Component getDisplay() {
        return Component.text(Utils.formatDecimal(value * 100) + "% miễn thương", NamedTextColor.GREEN);
    }

    @Override
    public void onDamage(DamageEvent event) {
        event.addDamageModifier(1 - value, DamageOperation.MORE_FINAL);
    }
}
