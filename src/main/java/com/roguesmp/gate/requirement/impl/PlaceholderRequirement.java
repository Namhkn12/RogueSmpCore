package com.roguesmp.gate.requirement.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.gate.requirement.Requirement;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.Utils;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public class PlaceholderRequirement implements Requirement {

    public static final String TYPE_KEY = "placeholder";
    public static final Codec<PlaceholderRequirement> CODEC = Codec.composite(
            Codec.STRING.fieldOf("placeholder").forGetter(PlaceholderRequirement::getPlaceholder),
            Codec.enumOf(Operator.class).optionalFieldOf("operator", Operator.GTE).forGetter(PlaceholderRequirement::getOperator),
            Codec.STRING.fieldOf("value").forGetter(PlaceholderRequirement::getValue),
            Codec.STRING.fieldOf("display").forGetter(PlaceholderRequirement::getDisplayText),
            PlaceholderRequirement::new
    );

    private final String placeholder;
    private final Operator operator;
    private final String value;
    private final String displayText;

    public PlaceholderRequirement(String placeholder, Operator operator, String value, String displayText) {
        this.placeholder = placeholder;
        this.operator = operator;
        this.value = value;
        this.displayText = displayText;
    }

    @Override
    public String getTypeId() {
        return TYPE_KEY;
    }

    public String getPlaceholder() {
        return placeholder;
    }

    public Operator getOperator() {
        return operator;
    }

    public String getValue() {
        return value;
    }

    public String getDisplayText() {
        return displayText;
    }

    @Override
    public boolean test(SmpPlayer player) {
        Player bukkitPlayer = player.getBukkitPlayer();
        return bukkitPlayer != null && operator.test(PlaceholderAPI.setPlaceholders(bukkitPlayer, placeholder), value);
    }

    @Override
    public Component getDisplay(SmpPlayer player) {
        Player bukkitPlayer = player.getBukkitPlayer();
        return Utils.fromString(bukkitPlayer == null ? displayText : PlaceholderAPI.setPlaceholders(bukkitPlayer, displayText));
    }

    public enum Operator {
        EQ, NEQ, GT, GTE, LT, LTE;

        public boolean test(String actual, String expected) {
            if (this == EQ) return actual.equals(expected);
            if (this == NEQ) return !actual.equals(expected);

            double a;
            double b;
            try {
                a = Double.parseDouble(actual.trim());
                b = Double.parseDouble(expected.trim());
            } catch (NumberFormatException e) {
                return false;
            }
            return switch (this) {
                case GT -> a > b;
                case GTE -> a >= b;
                case LT -> a < b;
                case LTE -> a <= b;
                default -> false;
            };
        }
    }
}
