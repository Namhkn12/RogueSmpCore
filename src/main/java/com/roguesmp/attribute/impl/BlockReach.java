package com.roguesmp.attribute.impl;

import com.roguesmp.attribute.SmpAttribute;
import com.roguesmp.constant.Attributes;
import com.roguesmp.player.SmpPlayer;
import io.papermc.paper.persistence.PersistentDataContainerView;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * This attribute determines the block interaction range for players in blocks. <br> <br>
 * Default	4.5
 * Minimum	0
 * Maximum	64
 */
public class BlockReach implements SmpAttribute {
    public static final NamespacedKey MODIFIER_ID = new NamespacedKey("smp", "block_reach");

    @Override public @NotNull String getId() { return "block_reach"; }
    @Override public @NotNull Attributes getEnumConstant() { return Attributes.BLOCK_REACH; }
    @Override public @NotNull String getSimpleName() { return "Tầm đặt khối"; }

    @Override
    public void addVanillaAttribute(Player player, double value) {
        AttributeInstance ai = player.getAttribute(Attribute.BLOCK_INTERACTION_RANGE);
        if (ai != null) {
            ai.removeModifier(MODIFIER_ID);
            ai.addTransientModifier(new AttributeModifier(MODIFIER_ID, value, AttributeModifier.Operation.ADD_NUMBER));
        }
    }

    @Override
    public void removeVanillaAttribute(Player player) {
        AttributeInstance ai = player.getAttribute(Attribute.BLOCK_INTERACTION_RANGE);
        if (ai != null) {
            ai.removeModifier(MODIFIER_ID);
        }
    }

    @Override public @NotNull List<Component> getDisplayText(double value, @Nullable SmpPlayer player, PersistentDataContainerView pdc) {
        return defaultFlatLoreProvider(value);
    }
}
