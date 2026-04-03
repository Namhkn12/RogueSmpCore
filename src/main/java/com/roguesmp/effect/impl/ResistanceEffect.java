package com.roguesmp.effect.impl;

import com.roguesmp.constant.DamageOperation;
import com.roguesmp.constant.DamageType;
import com.roguesmp.effect.SmpEffect;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Set;

public class ResistanceEffect extends SmpEffect {

    public static final String ID = "resistance_effect";
    public static final Set<DamageType> DEFAULT_DAMAGE_TYPE = EnumSet.of(
            DamageType.MELEE,
            DamageType.MELEE_ABILITY,
            DamageType.PROJECTILE,
            DamageType.PROJECTILE_ABILITY,
            DamageType.BLAST,
            DamageType.MAGIC);

    private final Set<DamageType> allowedDamageType;
    private final double value;

    /**
     * @param value Value used for percentage (0.4, 0.5, etc...)
     */
    public ResistanceEffect(int duration, double value, DeathBehavior deathBehavior, Set<DamageType> allowedDamageType) {
        super(duration, ID, deathBehavior);
        this.value = value;
        this.allowedDamageType = allowedDamageType;
    }

    /**
     * @param value Value used for percentage (0.4, 0.5, etc...)
     */
    public ResistanceEffect(int duration, double value, DeathBehavior deathBehavior) {
        this(duration, value, deathBehavior, DEFAULT_DAMAGE_TYPE);
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
        if (allowedDamageType.contains(event.getDamageType())) {
            event.addDamageModifier(1 - value, DamageOperation.MORE_FINAL);
        }

    }
}
