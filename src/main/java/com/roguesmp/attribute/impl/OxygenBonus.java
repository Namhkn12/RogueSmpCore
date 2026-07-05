package com.roguesmp.attribute.impl;

import com.roguesmp.attribute.SmpAttribute;
import com.roguesmp.constant.Attributes;
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

public class OxygenBonus implements SmpAttribute {

    public static NamespacedKey MODIFIER_ID = Keys.of("oxygen_bonus");

    @Override
    public @NotNull String getId() {
        return "oxygen_bonus";
    }

    @Override
    public @NotNull Attributes getEnumConstant() {
        return Attributes.OXYGEN_BONUS;
    }

    @Override
    public @NotNull String getSimpleName() {
        return "Phần oxi thêm";
    }

    @Override
    public @Nullable List<Component> getDisplayText(double value, @Nullable SmpPlayer player, PersistentDataContainerView pdc) {
        return defaultFlatLoreProvider(value);
    }

    @Override
    public void addVanillaAttribute(Player player, double value) {
        AttributeInstance ai = player.getAttribute(Attribute.OXYGEN_BONUS);
        if (ai != null) {
            ai.removeModifier(MODIFIER_ID);
            ai.addTransientModifier(new AttributeModifier(MODIFIER_ID, value, AttributeModifier.Operation.ADD_NUMBER));
        }
    }

    @Override
    public void removeVanillaAttribute(Player player) {
        AttributeInstance ai = player.getAttribute(Attribute.OXYGEN_BONUS);
        if (ai != null) {
            ai.removeModifier(MODIFIER_ID);
        }
    }

    @Override
    public @NotNull String getSimpleDescription() {
        return "Thay đổi tỉ lệ mất không khí mỗi 50 mili giây khi ở dưới nước theo công thức 1/(x+1)";
    }

    @Override
    public Material getIcon() {
        return Material.SEAGRASS;
    }
}
