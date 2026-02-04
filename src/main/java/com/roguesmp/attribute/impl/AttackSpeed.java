package com.roguesmp.attribute.impl;

import com.roguesmp.attribute.SmpAttribute;
import com.roguesmp.constant.Attributes;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.Utils;
import io.papermc.paper.persistence.PersistentDataContainerView;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class AttackSpeed implements SmpAttribute {

    public static NamespacedKey MODIFIER_KEY = new NamespacedKey("smp","atk_spd");

    @Override
    public @NotNull String getId() {
        return "attack_speed";
    }

    @Override
    public @NotNull Attributes getEnumConstant() {
        return Attributes.ATTACK_SPEED;
    }

    @Override
    public @NotNull String getSimpleName() {
        return "Tốc đánh";
    }

    @Override
    public @NotNull List<Component> getDisplayText(double value, SmpPlayer player, PersistentDataContainerView pdc) {
        return Utils.fromStrings("<!i><blue>" + value + " " + getSimpleName());
    }

    @Override
    public void addVanillaAttribute(Player player, double value) {
        player.getAttribute(Attribute.ATTACK_SPEED).addTransientModifier(new AttributeModifier(MODIFIER_KEY, value, AttributeModifier.Operation.ADD_NUMBER));
    }
}
