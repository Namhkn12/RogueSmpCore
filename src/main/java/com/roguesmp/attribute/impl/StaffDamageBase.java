package com.roguesmp.attribute.impl;

import com.roguesmp.attribute.Attributes;
import com.roguesmp.attribute.SmpAttribute;
import com.roguesmp.constant.DamageOperation;
import com.roguesmp.constant.DamageOrigin;
import com.roguesmp.event.DamageEvent;
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

public class StaffDamageBase implements SmpAttribute {
    @Override
    public @NotNull String getId() {
        return "staff_damage_base";
    }

    @Override
    public @NotNull Attributes getEnumConstant() {
        return Attributes.STAFF_DAMAGE_BASE;
    }

    @Override
    public @NotNull String getSimpleName() {
        return "Sát thương trượng";
    }

    @Override
    public @NotNull List<Component> getDisplayText(double value, @Nullable SmpPlayer player, PersistentDataContainerView pdc) {
        Component res = Component.text(" " + Utils.formatDecimal(value) + " " + getSimpleName(), NamedTextColor.DARK_GREEN).decoration(TextDecoration.ITALIC, false);
        return List.of(res);
    }

    @Override
    public void onDamageEntity(DamageEvent event, double value, @NotNull SmpPlayer player) {
        if (event.getOrigin() == DamageOrigin.STAFF) {
            event.addDamageModifier(value, DamageOperation.BASE);
        }
    }

    @Override
    public @NotNull String getSimpleDescription() {
        return "Sát thương gốc của tia phép từ trượng (chữ xanh lam trên trượng), tất cả các buff phép sẽ dựa vào con số này";
    }

    @Override
    public Material getIcon() {
        return Material.BLAZE_ROD;
    }
}
