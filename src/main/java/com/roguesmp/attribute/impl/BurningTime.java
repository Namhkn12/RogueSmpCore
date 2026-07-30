package com.roguesmp.attribute.impl;

import com.roguesmp.attribute.SmpAttribute;
import com.roguesmp.attribute.Attributes;
import com.roguesmp.constant.Keys;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.Utils;
import io.papermc.paper.persistence.PersistentDataContainerView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BurningTime implements SmpAttribute {

    public static NamespacedKey MODIFIER_ID = Keys.of("burning_time");

    @Override public @NotNull String getId() { return "burning_time"; }
    @Override public @NotNull Attributes getEnumConstant() { return Attributes.BURNING_TIME; }
    @Override public @NotNull String getSimpleName() { return "Thời gian cháy"; }

    // Doesn't seem to work against lava
//    @Override
//    public void onCombust(EntityCombustEvent event, double value, @NotNull SmpPlayer player) {
//        // value 0.5 = +50% duration.
//        float duration = Math.max(0f, (float) (event.getDuration() * (1 + value)));
//        event.setDuration(duration);
//        player.getBukkitPlayer().sendMessage("Ugh aH");
//    }


    @Override
    public void addVanillaAttribute(Player player, double value) {
        AttributeInstance ai = player.getAttribute(Attribute.BURNING_TIME);
        if (ai != null) {
            ai.removeModifier(MODIFIER_ID);
            ai.addModifier(new AttributeModifier(MODIFIER_ID, value, AttributeModifier.Operation.ADD_SCALAR));
        }
    }

    @Override
    public void removeVanillaAttribute(Player player) {
        AttributeInstance ai = player.getAttribute(Attribute.BURNING_TIME);
        if (ai != null) {
            ai.removeModifier(MODIFIER_ID);
        }
    }

    @Override public @NotNull List<Component> getDisplayText(double value, @Nullable SmpPlayer player, PersistentDataContainerView pdc) {
        Component fullDisplay;
        TextColor color;

        String numberPrefix = value > 0 ? "+" : "";
        color = value > 0 ? NamedTextColor.RED : NamedTextColor.BLUE;
        fullDisplay = Component.text(numberPrefix + Utils.formatDecimal(value * 100) + "% " + getSimpleName(), color).decoration(TextDecoration.ITALIC, false);
        return List.of(fullDisplay);
    }

    @Override
    public @NotNull String getSimpleDescription() {
        return "Tăng thời gian cháy thêm x %";
    }

    @Override
    public Material getIcon() {
        return Material.MAGMA_BLOCK;
    }
}
