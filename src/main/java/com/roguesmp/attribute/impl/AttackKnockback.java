package com.roguesmp.attribute.impl;

import com.roguesmp.attribute.SmpAttribute;
import com.roguesmp.constant.Attributes;
import com.roguesmp.player.SmpPlayer;
import io.papermc.paper.persistence.PersistentDataContainerView;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class AttackKnockback implements SmpAttribute {
    public static final NamespacedKey MODIFIER_ID = new NamespacedKey("smp", "attack_knockback");

    @Override public @NotNull String getId() { return "attack_knockback"; }
    @Override public @NotNull Attributes getEnumConstant() { return Attributes.ATTACK_KNOCKBACK; }
    @Override public @NotNull String getSimpleName() { return "Đẩy lùi"; }

    @Override
    public void addVanillaAttribute(Player player, double value) {
        AttributeInstance ai = player.getAttribute(Attribute.ATTACK_KNOCKBACK);
        if (ai != null) {
            ai.removeModifier(MODIFIER_ID);
            ai.addTransientModifier(new AttributeModifier(MODIFIER_ID, value, AttributeModifier.Operation.ADD_NUMBER));
        }
    }

    @Override
    public void removeVanillaAttribute(Player player) {
        AttributeInstance ai = player.getAttribute(Attribute.ATTACK_KNOCKBACK);
        if (ai != null) {
            ai.removeModifier(MODIFIER_ID);
        }
    }

    @Override public @NotNull List<Component> getDisplayText(double value, @Nullable SmpPlayer player, PersistentDataContainerView pdc) {
        return defaultFlatLoreProvider(value);
    }

    @Override
    public @NotNull String getSimpleDescription() {
        return "Tăng khoảng cách mục tiêu bị đẩy lùi khi tấn công thêm x khối";
    }

    @Override
    public Material getIcon() {
        return Material.SLIME_BLOCK;
    }
}
