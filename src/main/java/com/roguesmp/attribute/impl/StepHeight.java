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

public class StepHeight implements SmpAttribute {
    public static final NamespacedKey MODIFIER_ID = new NamespacedKey("smp", "step_height");

    @Override public @NotNull String getId() { return "step_height_flat"; }
    @Override public @NotNull Attributes getEnumConstant() { return Attributes.STEP_HEIGHT; }
    @Override public @NotNull String getSimpleName() { return "Độ cao bước chân"; }

    @Override
    public void addVanillaAttribute(Player player, double value) {
        AttributeInstance ai = player.getAttribute(Attribute.STEP_HEIGHT);
        if (ai != null) {
            ai.removeModifier(MODIFIER_ID);
            // value of 0.4 is default. value of 1.0 allows walking up full blocks.
            ai.addTransientModifier(new AttributeModifier(MODIFIER_ID, value, AttributeModifier.Operation.ADD_NUMBER));
        }
    }

    @Override
    public void removeVanillaAttribute(Player player) {
        AttributeInstance ai = player.getAttribute(Attribute.STEP_HEIGHT);
        if (ai != null) {
            ai.removeModifier(MODIFIER_ID);
        }
    }

    @Override public @NotNull List<Component> getDisplayText(double value, @Nullable SmpPlayer player, PersistentDataContainerView pdc) {
        return defaultFlatLoreProvider(value);
    }

    @Override
    public @NotNull String getSimpleDescription() {
        return "Tăng độ cao bước chân thêm x khối (Khi x = 0.5, người chơi có thể bước lên khối mà không cần nhảy";
    }

    @Override
    public Material getIcon() {
        return Material.STONE_STAIRS;
    }
}
