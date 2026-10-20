package com.roguesmp.effect.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.effect.SmpEffect;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public class RegenerationEffect extends SmpEffect {

    public static final String ID = "regeneration";

    public static final Codec<RegenerationEffect> CODEC = Codec.composite(
            SmpEffect.BASE_CODEC.forGetter(SmpEffect::getBaseProperties),
            Codec.DOUBLE.fieldOf("value").forGetter(RegenerationEffect::getMagnitude),
            RegenerationEffect::new
    );

    private final double value;

    public RegenerationEffect(int duration, double value) {
        super(ID, duration);
        this.value = value;
    }

    public RegenerationEffect(BaseProperties base, double value) {
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
        return Component.text("+" + Utils.formatDecimal(value) + " máu/s", NamedTextColor.RED);
    }

    @Override
    public void onTick(Entity entity, boolean oneHz, boolean twoHz) {
        if (oneHz && entity instanceof LivingEntity living && living.isValid()) living.heal(value);
    }
}
