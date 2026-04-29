package com.roguesmp.effect.impl;

import com.google.gson.JsonObject;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SpeedEffect extends SmpEffect {
    public static final String EFFECT_ID = "speed";

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
    public @NotNull JsonObject serialize() {
        JsonObject json = new JsonObject();

        json.addProperty("duration", this.duration);
        json.addProperty("deathBehavior", this.getDeathBehavior().name());

        // SpeedEffect specific fields
        json.addProperty("value", this.value);
        json.addProperty("modifierId", this.modifierId);

        // Display flags (if you want them persisted)
        json.addProperty("display", this.isDisplay());

        return json;
    }

    public static SpeedEffect deserialize(JsonObject json) {
        // 1. Extract Parent Data with defaults
        int duration = json.has("duration") ? json.get("duration").getAsInt() : 0;

        // Handle the Enum (DeathBehavior)
        DeathBehavior behavior = DeathBehavior.HALVES_ON_DEATH; // Default
        if (json.has("deathBehavior")) {
            String name = json.get("deathBehavior").getAsString();
            behavior = DeathBehavior.valueOf(name);
        }

        // 2. Extract SpeedEffect Data
        double value = json.has("value") ? json.get("value").getAsDouble() : 0.0;
        String modifierId = json.has("modifierId") ? json.get("modifierId").getAsString() : "unknown";

        // 3. Return a new instance using your existing constructor
        return new SpeedEffect(duration, value, behavior, modifierId);
    }

    @Override
    public @Nullable Component getDisplay() {
        if (value <= 0) {
            return Component.text(Utils.formatDecimal(value * 100) + "% tốc chạy", NamedTextColor.RED);
        }
        return Component.text("+" + Utils.formatDecimal(value * 100) + "% tốc chạy", NamedTextColor.GREEN);
    }

    @Override
    public void onGainEffect(Entity entity) {
        if (entity instanceof LivingEntity le) {
            AttributeInstance speedInstance = le.getAttribute(Attribute.MOVEMENT_SPEED);
            if (speedInstance != null) {
                speedInstance.removeModifier(Keys.of(modifierId));
                AttributeModifier modifier = new AttributeModifier(Keys.of(modifierId), value, AttributeModifier.Operation.ADD_SCALAR);
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
