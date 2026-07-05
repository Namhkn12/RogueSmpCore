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

public class JumpStrength implements SmpAttribute {
    public static final NamespacedKey MODIFIER_ID = new NamespacedKey("smp", "jump_strength");

    @Override public @NotNull String getId() { return "jump_strength"; }
    @Override public @NotNull Attributes getEnumConstant() { return Attributes.JUMP_STRENGTH; }
    @Override public @NotNull String getSimpleName() { return "Sức nhảy"; }

    @Override
    public void addVanillaAttribute(Player player, double value) {
        AttributeInstance ai = player.getAttribute(Attribute.JUMP_STRENGTH);
        if (ai != null) {
            ai.removeModifier(MODIFIER_ID);
            ai.addTransientModifier(new AttributeModifier(MODIFIER_ID, value, AttributeModifier.Operation.ADD_SCALAR));
        }
    }

    @Override
    public void removeVanillaAttribute(Player player) {
        AttributeInstance ai = player.getAttribute(Attribute.JUMP_STRENGTH);
        if (ai != null) {
            ai.removeModifier(MODIFIER_ID);
        }
    }

    @Override public @NotNull List<Component> getDisplayText(double value, @Nullable SmpPlayer player, PersistentDataContainerView pdc) {
        return defaultPercentLoreProvider(value * 100);
    }

    @Override
    public @NotNull String getSimpleDescription() {
        return "Tăng gia tốc khi nhảy thêm x %";
    }

    @Override
    public Material getIcon() {
        return Material.SLIME_BALL;
    }
}
