package com.roguesmp.effect.impl;

import com.roguesmp.constant.Keys;
import com.roguesmp.effect.EffectManager;
import com.roguesmp.effect.SmpEffect;
import com.roguesmp.utils.Utils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.DoubleArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public class SpeedEffect extends SmpEffect {
    public static final String EFFECT_ID = "speed_buff";

    private final double value;
    private final String modifierId;

    public SpeedEffect(int duration, double value, DeathBehavior behavior, String modifierId) {
        super(duration, EFFECT_ID, behavior);
        this.value = value;
        this.modifierId = modifierId;
    }

    public SpeedEffect(int duration, double value, String modifierId) {
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
    public @Nullable Component getDisplay() {
        return Component.text("+" + Utils.formatDecimal(value * 100) + "% tốc chạy", NamedTextColor.GREEN);
    }

    @Override
    public void onGainEffect(Entity entity) {
        if (entity instanceof LivingEntity le) {
            AttributeInstance speedInstance = le.getAttribute(Attribute.MOVEMENT_SPEED);
            if (speedInstance != null) {
                AttributeModifier modifier = new AttributeModifier(Keys.of(modifierId), value, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
                speedInstance.addTransientModifier(modifier);
            }
        }
    }

    @Override
    public void onLoseEffect(Entity entity) {
        if (entity instanceof LivingEntity le) {
            AttributeInstance speedInstance = le.getAttribute(Attribute.MOVEMENT_SPEED);
            if (speedInstance != null) {
                speedInstance.removeModifier(Keys.of(modifierId));
            }
        }
    }

    public static CommandAPICommand registerCommand() {
        return new CommandAPICommand("speed")
                .withArguments(
                        new IntegerArgument("duration"),
                        new DoubleArgument("percent"),
                        new StringArgument("modifierId"),
                        new StringArgument("source")
                )
                .executesPlayer((player, args) -> {
                    int duration = (Integer) args.get("duration");
                    double value = (Double) args.get("percent");
                    String modifierId = (String) args.get("modifierId");
                    String source = (String) args.get("source");

                    EffectManager.getInstance().addEffect(player, source, new SpeedEffect(duration, value / 100, modifierId));
                });
    }
}
