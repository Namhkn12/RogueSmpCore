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

public class KnockbackResistance implements SmpAttribute {
    public static final NamespacedKey MODIFIER_ID = new NamespacedKey("smp", "knockback_resistance");

    @Override public @NotNull String getId() { return "knockback_resistance"; }
    @Override public @NotNull Attributes getEnumConstant() { return Attributes.KNOCKBACK_RESISTANCE; }
    @Override public @NotNull String getSimpleName() { return "Kháng đẩy lùi"; }

    @Override
    public void addVanillaAttribute(Player player, double value) {
        AttributeInstance ai = player.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
        if (ai != null) {
            ai.removeModifier(MODIFIER_ID);
            ai.addTransientModifier(new AttributeModifier(MODIFIER_ID, value, AttributeModifier.Operation.ADD_NUMBER));
        }
    }

    @Override
    public void removeVanillaAttribute(Player player) {
        AttributeInstance ai = player.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
        if (ai != null) {
            ai.removeModifier(MODIFIER_ID);
        }
    }

    @Override public @NotNull List<Component> getDisplayText(double value, @Nullable SmpPlayer player, PersistentDataContainerView pdc) {
        return defaultPercentLoreProvider(value * 100);
    }

    @Override
    public @NotNull String getSimpleDescription() {
        return "Tăng kháng đẩy lùi thêm x. Khi đạt 10 thì sẽ không bị đẩy lùi nữa";
    }

    @Override
    public Material getIcon() {
        return Material.OBSIDIAN;
    }
}
