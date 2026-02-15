package com.roguesmp.attribute.impl;

import com.roguesmp.attribute.SmpAttribute;
import com.roguesmp.constant.Attributes;
import com.roguesmp.constant.DamageOperation;
import com.roguesmp.constant.DamageType;
import com.roguesmp.context.DamageContext;
import com.roguesmp.damage.DefenseModifier;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.Utils;
import io.papermc.paper.persistence.PersistentDataContainerView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class DefenseFlat implements SmpAttribute {
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
    public void onHurt(DamageContext context, double value, SmpPlayer player) {
        context.addDefenseModifier(new DefenseModifier(getId(), value, DamageType.PHYSICAL, DamageOperation.ADD_BASE));
    }
}
