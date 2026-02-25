package com.roguesmp.effect.impl;

import com.roguesmp.constant.Keys;
import com.roguesmp.effect.SmpEffect;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

public class SpeedBuffEffect extends SmpEffect {
    public static final String EFFECT_ID = "speed_buff";

    private final double value;
    private final String modifierId;

    public SpeedBuffEffect(int duration, double value, DeathBehavior behavior, String modifierId) {
        super(duration, EFFECT_ID, behavior);
        this.value = value;
        this.modifierId = modifierId;
    }

    public SpeedBuffEffect(int duration, double value, String modifierId) {
        super(duration, EFFECT_ID);
        this.value = value;
        this.modifierId = modifierId;
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
    public void onGainEffect(Entity entity) {
        if (entity instanceof LivingEntity le) {
            AttributeInstance speedInstance = le.getAttribute(Attribute.MOVEMENT_SPEED);
            if (speedInstance != null) {
                AttributeModifier modifier = new AttributeModifier(Keys.of(modifierId), value, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
                speedInstance.addTransientModifier(modifier);
            }
            entity.sendMessage("GAINED " + value);
        }
    }

    @Override
    public void onLoseEffect(Entity entity) {
        if (entity instanceof LivingEntity le) {
            AttributeInstance speedInstance = le.getAttribute(Attribute.MOVEMENT_SPEED);
            if (speedInstance != null) {
                speedInstance.removeModifier(Keys.of(modifierId));
            }
            entity.sendMessage("EXPIRED " + value);
        }
    }
}
