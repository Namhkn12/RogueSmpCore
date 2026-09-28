package com.roguesmp.attribute.impl;

import com.roguesmp.attribute.Attributes;
import com.roguesmp.attribute.SmpAttribute;
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
 * Purely custom stat, only affects {@link com.roguesmp.block.SmpBlock}s: a block with a declared
 * {@code break_strength} is unbreakable to a player whose gear total is below it (see
 * {@link com.roguesmp.block.mining.MiningSpeedCalculator}).
 */
public class BreakStrength implements SmpAttribute {

    @Override
    public @NotNull String getId() {
        return "break_strength";
    }

    @Override
    public @NotNull Attributes getEnumConstant() {
        return Attributes.BREAK_STRENGTH;
    }

    @Override
    public @NotNull String getSimpleName() {
        return "Sức đào";
    }

    @Override
    public @Nullable List<Component> getDisplayText(double value, @Nullable SmpPlayer player, PersistentDataContainerView pdc) {
        Component fullDisplay;
        TextColor color = NamedTextColor.DARK_GREEN;
        fullDisplay = Component.text(" " + Utils.formatDecimal(value) + " " + getSimpleName(), color).decoration(TextDecoration.ITALIC, false);

        return List.of(fullDisplay);
    }

    @Override
    public @NotNull String getSimpleDescription() {
        return "Nếu sức đào thấp hơn độ cứng của khối thì sẽ không đào được";
    }

    @Override
    public Material getIcon() {
        return Material.NETHERITE_PICKAXE;
    }
}
