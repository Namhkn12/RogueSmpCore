package com.roguesmp.attribute.impl;

import com.roguesmp.attribute.Attributes;
import com.roguesmp.attribute.SmpAttribute;
import com.roguesmp.constant.Keys;
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

public class MiningEfficiency implements SmpAttribute {

    private static final NamespacedKey KEY = Keys.of("mining_efficiency");

    @Override
    public @NotNull String getId() {
        return "mining_efficiency";
    }

    @Override
    public @NotNull Attributes getEnumConstant() {
        return Attributes.MINING_EFFICIENCY;
    }

    @Override
    public @NotNull String getSimpleName() {
        return "Tốc độ đào";
    }

    @Override
    public @Nullable List<Component> getDisplayText(double value, @Nullable SmpPlayer player, PersistentDataContainerView pdc) {
        return defaultFlatLoreProvider(value);
    }

    @Override
    public @NotNull String getSimpleDescription() {
        return "Hoạt động giống vanilla";
    }

    @Override
    public Material getIcon() {
        return Material.NETHERITE_PICKAXE;
    }

    @Override
    public void addVanillaAttribute(Player player, double value) {
        AttributeInstance instance = player.getAttribute(Attribute.MINING_EFFICIENCY);
        if (instance != null) {
            instance.removeModifier(KEY);
            instance.addTransientModifier(new AttributeModifier(KEY, value, AttributeModifier.Operation.ADD_NUMBER));
        }
    }

    @Override
    public void removeVanillaAttribute(Player player) {
        AttributeInstance instance = player.getAttribute(Attribute.MINING_EFFICIENCY);
        if (instance != null) {
            instance.removeModifier(KEY);
        }
    }
}
