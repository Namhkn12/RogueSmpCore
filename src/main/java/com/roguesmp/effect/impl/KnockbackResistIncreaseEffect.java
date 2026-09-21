package com.roguesmp.effect.impl;

import com.roguesmp.constant.Keys;
import com.roguesmp.effect.SmpEffect;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import org.bukkit.attribute.Attributable;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Nullable;

public class KnockbackResistIncreaseEffect extends SmpEffect {

    public static final String ID = "kb_resist_increase";

    private final double value;

    public KnockbackResistIncreaseEffect(int duration, double value) {
        super(ID, duration);
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
    public @Nullable Component getDisplayComponent() {
        return Component.text("+" + Utils.formatDecimal(value * 100) + "% Kháng đánh lùi");
    }

    @Override
    public void onGainEffect(Entity entity) {
        if (entity instanceof Attributable attributable) {
            AttributeInstance kb = attributable.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
            AttributeInstance kbExp = attributable.getAttribute(Attribute.EXPLOSION_KNOCKBACK_RESISTANCE);
            if (kb != null) {
                kb.removeModifier(Keys.of(ID));
                kb.addTransientModifier(new AttributeModifier(Keys.of(ID), value, AttributeModifier.Operation.ADD_NUMBER));
            }
            if (kbExp != null) {
                kbExp.addTransientModifier(new AttributeModifier(Keys.of(ID), value, AttributeModifier.Operation.ADD_NUMBER));
                kbExp.removeModifier(Keys.of(ID));
            }
        }
    }

    @Override
    public void onLoseEffect(Entity entity) {
        if (entity instanceof Attributable attributable) {
            AttributeInstance kb = attributable.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
            AttributeInstance kbExp = attributable.getAttribute(Attribute.EXPLOSION_KNOCKBACK_RESISTANCE);
            if (kb != null) {
                kb.removeModifier(Keys.of(ID));
            }
            if (kbExp != null) {
                kbExp.removeModifier(Keys.of(ID));
            }
        }
    }

    public double getValue() {
        return value;
    }
}
