package com.roguesmp.item;

import com.roguesmp.constant.Keys;
import com.roguesmp.context.ItemDataContext;
import com.roguesmp.context.ItemLoreContext;
import com.roguesmp.item.component.ComponentKey;
import com.roguesmp.item.component.ItemComponent;
import com.roguesmp.item.lore.LoreBuilder;
import com.roguesmp.item.modifier.ItemModifier;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.registry.ItemRegistry;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import io.papermc.paper.persistence.PersistentDataContainerView;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Represent items with modifiers applied (in-game items)
 */
public class SmpItem {
    private final BaseItem baseItem;
    private ItemStack oldStack;

    private final Map<String, ItemComponent> componentMap = new HashMap<>();
    private final Map<String, ItemModifier> modifierMap = new HashMap<>();

    public SmpItem(@NotNull ItemStack itemStack) {
        this.oldStack = itemStack;
        PersistentDataContainerView pdc = itemStack.getPersistentDataContainer();

        String itemId = pdc.get(Keys.ITEM_ID, PersistentDataType.STRING);
        if (itemId != null) {
            this.baseItem = ItemRegistry.getInstance().getBaseItem(itemId);
        } else this.baseItem = null;

        if (baseItem != null) {
            baseItem.getComponents().forEach((s, itemComponent) -> {
                componentMap.put(s, itemComponent.copy());
            });
        }
    }

    public SmpItem(@NotNull BaseItem baseItem) {
        this.baseItem = baseItem;
        this.oldStack = ItemStack.of(Material.STICK);
        baseItem.getComponents().forEach((s, itemComponent) -> {
            componentMap.put(s, itemComponent.copy());
        });
    }

    public @Nullable <T extends ItemComponent> T getComponent(ComponentKey<T> key) {
        return (T) componentMap.get(key.id());
    }

    public ItemStack generateItemStack(SmpPlayer player, int stackAmount) {
        ItemStack result = ItemStack.of(baseItem.getBase(), stackAmount);

        result.setData(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplay.tooltipDisplay().hiddenComponents(Set.of(DataComponentTypes.ENCHANTMENTS, DataComponentTypes.ATTRIBUTE_MODIFIERS)).build());

        PersistentDataContainerView oldData = oldStack.getPersistentDataContainer();
        LoreBuilder loreBuilder = new LoreBuilder();

        result.editPersistentDataContainer(pdc -> {

            oldData.copyTo(pdc, true);
            pdc.set(Keys.ITEM_ID, PersistentDataType.STRING, baseItem.getId());
            ItemDataContext dataContext = new ItemDataContext(this, player, result, oldStack, pdc);
            ItemLoreContext loreContext = new ItemLoreContext(this, player, loreBuilder, pdc);

            componentMap.forEach((s, itemComponent) -> {
                itemComponent.modifyStack(dataContext);
                itemComponent.contributeLore(loreContext);
            });
        });

        result.setData(DataComponentTypes.LORE, ItemLore.lore(loreBuilder.build()));

        oldStack = result;

        return result;
    }
}
