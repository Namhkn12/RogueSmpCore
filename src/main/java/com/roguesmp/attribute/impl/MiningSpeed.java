package com.roguesmp.attribute.impl;

import com.roguesmp.attribute.Attributes;
import com.roguesmp.attribute.SmpAttribute;
import com.roguesmp.block.mining.MiningSpeedCalculator;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.Utils;
import io.papermc.paper.persistence.PersistentDataContainerView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Purely custom stat, unrelated to vanilla {@code Attribute.MINING_EFFICIENCY} attribute
 * ({@link MiningEfficiency} wraps that one) - only affects {@link com.roguesmp.block.SmpBlock}s,
 * read via {@link SmpPlayer#getActiveAttributes()} by {@link MiningSpeedCalculator}.
 */
public class MiningSpeed implements SmpAttribute {

    @Override
    public @NotNull String getId() {
        return "mining_speed";
    }

    @Override
    public @NotNull Attributes getEnumConstant() {
        return Attributes.MINING_SPEED;
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
        return "Tăng tốc độ đào các khối (chỉ hoạt động với khối custom)";
    }

    @Override
    public Material getIcon() {
        return Material.GOLDEN_PICKAXE;
    }
}
