package com.roguesmp.item.gem;

import com.roguesmp.constant.Attributes;
import com.roguesmp.constant.EquipSlot;
import com.roguesmp.item.BaseItem;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.registry.ItemRegistry;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collections;
import java.util.Map;

public class GemData {
    private final String id;
    private final String icon;
    private final Map<EquipSlot, Map<Attributes, Double>> attributes;

    public GemData(String id, String icon, Map<EquipSlot, Map<Attributes, Double>> attributes) {
        this.id = id;
        this.icon = icon;
        this.attributes = attributes;
    }

    public @Nullable ItemStack getIconStack(SmpPlayer player) {
        BaseItem baseItem = ItemRegistry.getInstance().getBaseItem(icon);
        if (baseItem == null) {
            player.getPlayer().sendMessage("This gem has invalid icon id!");
            return null;
        }
        return baseItem.generateItemStack(player, 1);
    }

    public String getId() {
        return id;
    }

    public String getIconId() {
        return icon;
    }

    public @Unmodifiable Map<EquipSlot, Map<Attributes, Double>> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }
}
