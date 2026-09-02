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

/**
 * One-shot buff: boosts the next {@link DamageType#MELEE} hit this entity lands by a percentage,
 * then consumes itself (expires on the following tick) regardless of remaining duration - a
 * non-melee hit (ability/projectile/etc.) passes through without consuming it.
 */
public class EmpoweredStrikeEffect extends SmpEffect {

    public static final String ID = "empowered_strike";

    public static final Codec<EmpoweredStrikeEffect> CODEC = Codec.composite(
            SmpEffect.BASE_CODEC.forGetter(SmpEffect::getBaseProperties),
            Codec.DOUBLE.fieldOf("value").forGetter(EmpoweredStrikeEffect::getMagnitude),
            EmpoweredStrikeEffect::new
    );

    private final double value;

    /**
     * @param value extra melee damage dealt on the next hit, as a percentage (0.3 = 30% more damage)
     */
    public EmpoweredStrikeEffect(int duration, double value) {
        super(ID, duration);
        this.value = value;
    }

    public EmpoweredStrikeEffect(BaseProperties base, double value) {
        super(ID, base);
        this.value = value;
    }

    @Override
    public double getMagnitude() {
        return value;
    }

    @Override
    public boolean isPersistent() {
        return false;
    }

    @Override
    public @Nullable Component getDisplayComponent() {
        return Component.text(Utils.formatDecimal(value * 100) + "% sát thương kế", NamedTextColor.GOLD);
    }

    @Override
    public void onDamageEntity(DamageEvent event) {
        if (event.getDamageType() != DamageType.MELEE) return;
        event.addDamageModifier(value, DamageOperation.INCREASE_BASE);
        setDuration(0); // Consumed - removed on the next tick pass
    }
}
