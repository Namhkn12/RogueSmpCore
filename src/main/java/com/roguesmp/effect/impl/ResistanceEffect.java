package com.roguesmp.effect.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.roguesmp.constant.DamageOperation;
import com.roguesmp.constant.DamageType;
import com.roguesmp.effect.SmpEffect;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
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
    public @NotNull JsonObject serialize() {
        JsonObject json = new JsonObject();

        // Parent/Base fields
        json.addProperty("duration", this.duration);
        json.addProperty("deathBehavior", this.getDeathBehavior().name());

        // ResistanceEffect specific fields
        json.addProperty("value", this.value);

        // Only serialize damage types if they aren't the default set
        if (!this.allowedDamageType.equals(DEFAULT_DAMAGE_TYPE)) {
            JsonArray typeArray = new JsonArray();
            for (DamageType type : this.allowedDamageType) {
                typeArray.add(type.name());
            }
            json.add("allowedTypes", typeArray);
        }

        return json;
    }

    public static ResistanceEffect deserialize(JsonObject json) {
        // 1. Extract Parent Data
        int duration = json.has("duration") ? json.get("duration").getAsInt() : 0;

        DeathBehavior behavior = DeathBehavior.HALVES_ON_DEATH;
        if (json.has("deathBehavior")) {
            try {
                behavior = DeathBehavior.valueOf(json.get("deathBehavior").getAsString());
            } catch (IllegalArgumentException e) {
                // Fallback if the enum name in the file is invalid
            }
        }

        // 2. Extract ResistanceEffect Data
        double value = json.has("value") ? json.get("value").getAsDouble() : 0.0;

        // Handle DamageType Set
        Set<DamageType> allowedTypes;
        if (json.has("allowedTypes")) {
            allowedTypes = EnumSet.noneOf(DamageType.class);
            JsonArray array = json.getAsJsonArray("allowedTypes");
            for (JsonElement element : array) {
                try {
                    allowedTypes.add(DamageType.valueOf(element.getAsString()));
                } catch (IllegalArgumentException ignored) {
                    // Skip unknown damage types from old versions
                }
            }
        } else {
            allowedTypes = DEFAULT_DAMAGE_TYPE;
        }

        // 3. Construct and Return

        return new ResistanceEffect(duration, value, behavior, allowedTypes);
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
