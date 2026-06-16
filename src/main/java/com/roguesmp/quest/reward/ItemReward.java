package com.roguesmp.quest.reward;

import com.roguesmp.constant.ComponentKeys;
import com.roguesmp.item.BaseItem;
import com.roguesmp.item.component.impl.NameComponent;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.quest.QuestReward;
import com.roguesmp.registry.ItemRegistry;
import com.roguesmp.utils.PlayerUtils;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ItemReward implements QuestReward {

    private final Map<String, Integer> items;

    public ItemReward(Map<String, Integer> items) {
        this.items = items;
    }

    public @Unmodifiable Map<String, Integer> getItems() {
        return Collections.unmodifiableMap(items);
    }

    @Override
    public List<Component> getDisplay(SmpPlayer smpPlayer) {
        List<Component> displayList = new ArrayList<>();

        items.forEach((itemId, amount) -> {
            BaseItem baseItem = ItemRegistry.getInstance().getBaseItem(itemId);
            // Fallback to the raw ID if the item registry returns null
            String itemName;
            if (baseItem == null) {
                itemName = "NOT_FOUND";
            } else {
                NameComponent component = baseItem.getComponent(ComponentKeys.ITEM_NAME);
                if (component == null) {
                    itemName = itemId;
                } else itemName = component.value();
            }

            // Format: "• 5x Kim Cương" with distinct colors
            String displayString = String.format(
                    "<yellow>%dx</yellow> <aqua>%s</aqua>",
                    amount, itemName
            );

            displayList.add(Utils.fromString(displayString));
        });

        return displayList;
    }

    @Override
    public void giveReward(SmpPlayer smpPlayer) {
        List<ItemStack> rewards = new ArrayList<>();
        items.forEach((s, integer) -> {
            BaseItem baseItem = ItemRegistry.getInstance().getBaseItem(s);
            if (baseItem == null) return;
            ItemStack itemStack = baseItem.generateItemStack(smpPlayer, integer);
            rewards.add(itemStack);
        });
        PlayerUtils.giveItem(smpPlayer.getBukkitPlayer(), rewards);
    }
}
