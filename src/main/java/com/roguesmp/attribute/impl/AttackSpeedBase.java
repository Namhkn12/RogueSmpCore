package com.roguesmp.attribute.impl;

import com.roguesmp.attribute.SmpAttribute;
import com.roguesmp.constant.Attributes;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.Utils;
import io.papermc.paper.persistence.PersistentDataContainerView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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

public class AttackSpeedBase implements SmpAttribute {

    public static NamespacedKey MODIFIER_KEY = new NamespacedKey("smp","atk_spd_base");

    @Override
    public @NotNull String getId() {
        return "attack_speed_base";
    }

    @Override
    public @NotNull Attributes getEnumConstant() {
        return Attributes.ATTACK_SPEED_BASE;
    }

    @Override
    public @NotNull String getSimpleName() {
        return "Tốc đánh";
    }

    @Override
    public @NotNull List<Component> getDisplayText(double value, @Nullable SmpPlayer player, PersistentDataContainerView pdc) {
        Component res = Component.text(" " + Utils.formatDecimal(value) + " " + getSimpleName(), NamedTextColor.DARK_GREEN).decoration(TextDecoration.ITALIC, false);
        return List.of(res);
    }

    @Override
    public void addVanillaAttribute(Player player, double value) {
        AttributeInstance ai = player.getAttribute(Attribute.ATTACK_SPEED);
        if (ai != null) {
            ai.removeModifier(MODIFIER_KEY);
            ai.addTransientModifier(new AttributeModifier(MODIFIER_KEY, value - 4, AttributeModifier.Operation.ADD_NUMBER));
        }
    }

    @Override
    public void removeVanillaAttribute(Player player) {
        AttributeInstance ai = player.getAttribute(Attribute.ATTACK_SPEED);
        if (ai != null) {
            ai.removeModifier(MODIFIER_KEY);
        }
    }

    @Override
    public @NotNull String getSimpleDescription() {
        return "Hoạt động giống như minecraft thông thường";
    }

    @Override
    public Material getIcon() {
        return Material.IRON_SWORD;
    }
}
