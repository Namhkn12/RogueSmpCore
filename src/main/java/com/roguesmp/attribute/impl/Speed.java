package com.roguesmp.attribute.impl;

import com.roguesmp.attribute.SmpAttribute;
import com.roguesmp.constant.Attributes;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.Utils;
import io.papermc.paper.persistence.PersistentDataContainerView;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class Speed implements SmpAttribute {

    public static NamespacedKey MODIFIER_ID = new NamespacedKey("smp","speed");

    @Override
    public @NotNull String getId() {
        return "speed";
    }

    @Override
    public @NotNull Attributes getEnumConstant() {
        return Attributes.SPEED;
    }

    @Override
    public @NotNull String getSimpleName() {
        return "Tốc chạy";
    }

    @Override
    public @NotNull List<Component> getDisplayText(double value, SmpPlayer player, PersistentDataContainerView pdc) {
        return Utils.fromStrings("<!i><aqua>" + value + " " + getSimpleName());
    }

    @Override
    public void addVanillaAttribute(Player player, double value) {
        player.getAttribute(Attribute.MOVEMENT_SPEED).addTransientModifier(new AttributeModifier(MODIFIER_ID, value, AttributeModifier.Operation.ADD_NUMBER));
    }

    @Override
    public void removeVanillaAttribute(Player player) {
        player.getAttribute(Attribute.MOVEMENT_SPEED).removeModifier(MODIFIER_ID);
    }
}
