package com.roguesmp.attribute.impl;

import com.roguesmp.attribute.SmpAttribute;
import com.roguesmp.constant.Attributes;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.Utils;
import io.papermc.paper.persistence.PersistentDataContainerView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class FallDamage implements SmpAttribute {
    public static final NamespacedKey MODIFIER_ID = new NamespacedKey("smp", "fall_damage");

    @Override public @NotNull String getId() { return "fall_damage"; }
    @Override public @NotNull Attributes getEnumConstant() { return Attributes.FALL_DAMAGE; }
    @Override public @NotNull String getSimpleName() { return "Sát thương rơi"; }

    @Override
    public void addVanillaAttribute(Player player, double value) {
        AttributeInstance ai = player.getAttribute(Attribute.FALL_DAMAGE_MULTIPLIER);
        if (ai != null) {
            ai.removeModifier(MODIFIER_ID);
            ai.addTransientModifier(new AttributeModifier(MODIFIER_ID, value, AttributeModifier.Operation.ADD_SCALAR));
        }
    }

    @Override
    public void removeVanillaAttribute(Player player) {
        AttributeInstance ai = player.getAttribute(Attribute.FALL_DAMAGE_MULTIPLIER);
        if (ai != null) {
            ai.removeModifier(MODIFIER_ID);
        }
    }

    @Override public @NotNull List<Component> getDisplayText(double value, @Nullable SmpPlayer player, PersistentDataContainerView pdc) {
        Component fullDisplay;
        TextColor color;

        String numberPrefix = value > 0 ? "+" : "";
        color = value > 0 ? NamedTextColor.RED : NamedTextColor.BLUE;
        fullDisplay = Component.text(numberPrefix + Utils.formatDecimal(value * 100) + "% " + getSimpleName(), color).decoration(TextDecoration.ITALIC, false);
        return List.of(fullDisplay);
    }

    @Override
    public @NotNull String getSimpleDescription() {
        return "Tăng sát thương rơi nhận phải thêm x %";
    }

    @Override
    public Material getIcon() {
        return Material.FEATHER;
    }
}
