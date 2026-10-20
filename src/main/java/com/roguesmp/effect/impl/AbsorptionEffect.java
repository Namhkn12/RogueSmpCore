package com.roguesmp.effect.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.effect.SmpEffect;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public class AbsorptionEffect extends SmpEffect {

    public static final String ID = "absorption";

    public static final Codec<AbsorptionEffect> CODEC = Codec.composite(
            SmpEffect.BASE_CODEC.forGetter(SmpEffect::getBaseProperties),
            Codec.DOUBLE.fieldOf("value").forGetter(AbsorptionEffect::getMagnitude),
            Codec.BOOLEAN.optionalFieldOf("granted", false).forGetter(effect -> effect.granted),
            AbsorptionEffect::new
    );

    private final double value;
    private boolean granted;

    public AbsorptionEffect(int duration, double value) {
        super(ID, duration);
        this.value = value;
    }

    public AbsorptionEffect(BaseProperties base, double value, boolean granted) {
        super(ID, base);
        this.value = value;
        this.granted = granted;
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
        return Component.text("+" + Utils.formatDecimal(value) + " máu hấp thụ", NamedTextColor.GOLD);
    }

    @Override
    public void onGainEffect(Entity entity) {
        if (!(entity instanceof LivingEntity living)) return;
        AttributeInstance maxAbsorption = living.getAttribute(Attribute.MAX_ABSORPTION);
        if (maxAbsorption == null) return;

        maxAbsorption.removeModifier(sourceKey());
        maxAbsorption.addTransientModifier(new AttributeModifier(sourceKey(), value, AttributeModifier.Operation.ADD_NUMBER));
        if (granted) return;
        granted = true;
        living.setAbsorptionAmount(Math.min(maxAbsorption.getValue(), living.getAbsorptionAmount() + value));
    }

    @Override
    public void onLoseEffect(Entity entity) {
        if (!(entity instanceof LivingEntity living)) return;
        AttributeInstance maxAbsorption = living.getAttribute(Attribute.MAX_ABSORPTION);
        if (maxAbsorption == null) return;

        maxAbsorption.removeModifier(sourceKey());
        living.setAbsorptionAmount(Math.min(maxAbsorption.getValue(), living.getAbsorptionAmount()));
    }
}
