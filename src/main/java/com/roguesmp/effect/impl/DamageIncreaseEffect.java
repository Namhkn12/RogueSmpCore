package com.roguesmp.effect.impl;

import com.google.gson.JsonObject;
import com.roguesmp.codec.Codec;
import com.roguesmp.constant.DamageOperation;
import com.roguesmp.effect.EffectManager;
import com.roguesmp.effect.SmpEffect;
import com.roguesmp.event.DamageEvent;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.DoubleArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DamageIncreaseEffect extends SmpEffect {

    public static final Codec<DamageIncreaseEffect> CODEC = Codec.composite(
            SmpEffect.BASE_CODEC.forGetter(SmpEffect::getBaseProperties),
            Codec.DOUBLE.fieldOf("increase_value").forGetter(DamageIncreaseEffect::getMagnitude),
            DamageIncreaseEffect::new
    );

    public static final String ID = "damage_increase";

    private final double increaseValue;

    public DamageIncreaseEffect(int duration, double increaseValue) {
        super(duration, ID);
        this.increaseValue = increaseValue;
    }

    public DamageIncreaseEffect(BaseProperties base, double increaseValue) {
        super(ID, base);
        this.increaseValue = increaseValue;
    }

    @Override
    public double getMagnitude() {
        return increaseValue;
    }

    @Override
    public boolean isPersistent() {
        return false;
    }

    @Override
    public @NotNull JsonObject serialize() {
        JsonObject json = new JsonObject();

        // Parent/Base fields
        json.addProperty("duration", this.duration);

        // DamageIncreaseEffect specific fields
        json.addProperty("increaseValue", this.increaseValue);

        return json;
    }

    public static DamageIncreaseEffect deserialize(JsonObject json) {
        // 1. Extract Parent Data
        int duration = json.has("duration") ? json.get("duration").getAsInt() : 0;

        // 2. Extract DamageIncreaseEffect Data
        double increaseValue = json.has("increaseValue") ? json.get("increaseValue").getAsDouble() : 0.0;

        // 3. Construct the effect
        // Note: Since your constructor only takes (duration, increaseValue),
        // we use a setter for the behavior or ensure the constructor handles it.

        return new DamageIncreaseEffect(duration, increaseValue);
    }

    @Override
    public @Nullable Component getDisplay() {
        return Component.text("+" + increaseValue + " sát thương", NamedTextColor.GREEN);
    }

    @Override
    public void onDamageEntity(DamageEvent event) {
        event.addDamageModifier(increaseValue, DamageOperation.ADD_FINAL);
    }

    public static CommandAPICommand registerCommand() {
        return new CommandAPICommand("damageincrease")
                .withArguments(new IntegerArgument("duration"), new DoubleArgument("magnitude"), new StringArgument("source"))
                .executesPlayer((player, args) -> {
                    int duration = (Integer) args.get("duration");
                    double magnitude = (Double) args.get("magnitude");
                    String source = (String) args.get("source");

                    EffectManager.getInstance().addEffect(player, source, new DamageIncreaseEffect(duration, magnitude));
                });
    }
}
