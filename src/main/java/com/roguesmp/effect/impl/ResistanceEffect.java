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

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class ResistanceEffect extends SmpEffect {

    public static final String ID = "resistance";
    public static final Set<DamageType> DEFAULT_DAMAGE_TYPE = EnumSet.of(
            DamageType.MELEE,
            DamageType.MELEE_ABILITY,
            DamageType.PROJECTILE,
            DamageType.PROJECTILE_ABILITY,
            DamageType.BLAST,
            DamageType.MAGIC);

    public static final Codec<ResistanceEffect> CODEC = Codec.composite(
            SmpEffect.BASE_CODEC.forGetter(SmpEffect::getBaseProperties),
            Codec.DOUBLE.fieldOf("value").forGetter(ResistanceEffect::getMagnitude),
            Codec.listOf(Codec.enumOf(DamageType.class))
                    .optionalFieldOf("allowed_damage_types", List.copyOf(DEFAULT_DAMAGE_TYPE))
                    .forGetter(effect -> List.copyOf(effect.allowedDamageType)),
            (base, value, damageTypesList) -> new ResistanceEffect(
                    base,
                    value,
                    damageTypesList.isEmpty() ? EnumSet.noneOf(DamageType.class) : EnumSet.copyOf(damageTypesList)
            )
    );

    private final Set<DamageType> allowedDamageType;
    private final double value;

    /**
     * @param value Value used for percentage (0.4, 0.5, etc...)
     */
    public ResistanceEffect(int duration, double value, DeathBehavior deathBehavior, Set<DamageType> allowedDamageType) {
        super(ID, duration, deathBehavior);
        this.value = value;
        this.allowedDamageType = EnumSet.copyOf(allowedDamageType);
    }

    /**
     * @param value Value used for percentage (0.4, 0.5, etc...)
     */
    public ResistanceEffect(int duration, double value, DeathBehavior deathBehavior) {
        this(duration, value, deathBehavior, DEFAULT_DAMAGE_TYPE);
    }

    public ResistanceEffect(BaseProperties base, double value, Set<DamageType> allowedDamageType) {
        super(ID, base);
        this.value = value;
        this.allowedDamageType = EnumSet.copyOf(allowedDamageType);
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
    public @Nullable Component getDisplayComponent() {
        return Component.text(Utils.formatDecimal(value * 100) + "% miễn thương", NamedTextColor.GREEN);
    }

    @Override
    public void onHurt(DamageEvent event) {
        if (allowedDamageType.contains(event.getDamageType())) {
            event.addDamageModifier(1 - value, DamageOperation.MORE_FINAL);
        }

    }
}
