package com.roguesmp.attribute.impl;

import com.roguesmp.attribute.Attributes;
import com.roguesmp.attribute.SmpAttribute;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.Utils;
import io.papermc.paper.persistence.PersistentDataContainerView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class StaffRangeBase implements SmpAttribute {
    @Override
    public @NotNull String getId() {
        return "staff_range_base";
    }

    @Override
    public @NotNull Attributes getEnumConstant() {
        return Attributes.STAFF_RANGE_BASE;
    }

    @Override
    public @NotNull String getSimpleName() {
        return "Tầm đánh trượng";
    }

    @Override
    public @NotNull List<Component> getDisplayText(double value, @Nullable SmpPlayer player, PersistentDataContainerView pdc) {
        Component res = Component.text(" " + Utils.formatDecimal(value) + " " + getSimpleName(), NamedTextColor.DARK_GREEN).decoration(TextDecoration.ITALIC, false);
        return List.of(res);
    }

    @Override
    public @NotNull String getSimpleDescription() {
        return "Khoảng cách tối đa (số block) mà tia phép từ trượng có thể bay tới";
    }

    @Override
    public Material getIcon() {
        return Material.SPYGLASS;
    }
}
