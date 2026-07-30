package com.roguesmp.attribute.impl;

import com.roguesmp.attribute.SmpAttribute;
import com.roguesmp.attribute.Attributes;
import com.roguesmp.constant.DamageOperation;
import com.roguesmp.constant.DamageType;
import com.roguesmp.event.DamageEvent;
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

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class DefenseFlat implements SmpAttribute {

    public static Set<DamageType> protectedType = EnumSet.of(DamageType.MELEE, DamageType.MELEE_ABILITY, DamageType.PROJECTILE, DamageType.PROJECTILE_ABILITY, DamageType.MAGIC, DamageType.BLAST);

    @Override
    public @NotNull String getId() {
        return "defense_flat";
    }

    @Override
    public @NotNull Attributes getEnumConstant() {
        return Attributes.DEFENSE_FLAT;
    }

    @Override
    public @NotNull String getSimpleName() {
        return "Phòng ngự";
    }

    @Override
    public @Nullable List<Component> getDisplayText(double value, @Nullable SmpPlayer player, PersistentDataContainerView pdc) {
        Component fullDisplay;
        TextColor color;

        String numberPrefix = value > 0 ? "+" : "";
        color = value > 0 ? NamedTextColor.AQUA : NamedTextColor.RED;
        fullDisplay = Component.text(numberPrefix + Utils.formatDecimal(value) + " " + getSimpleName(), color).decoration(TextDecoration.ITALIC, false);

        return List.of(fullDisplay);
    }

    @Override
    public void onHurt(DamageEvent event, double value, @NotNull SmpPlayer player) {
        if (protectedType.contains(event.getDamageType())) {
            event.addDefenseModifier(value, DamageOperation.ADD_BASE);
        }
    }

    @Override
    public @NotNull String getSimpleDescription() {
        return "Giảm sát thương cận chiến, phép, vật bắn ra, vụ nổ nhận vào, công thức: ST * (20 / (20 + x))";
    }

    @Override
    public Material getIcon() {
        return Material.IRON_CHESTPLATE;
    }
}
