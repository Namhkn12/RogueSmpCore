package com.roguesmp.item;

import com.roguesmp.constant.Keys;
import com.roguesmp.context.ItemDataContext;
import com.roguesmp.context.ItemLoreContext;
import com.roguesmp.item.component.ComponentKey;
import com.roguesmp.item.component.ItemComponent;
import com.roguesmp.item.lore.LoreBuilder;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.registry.ItemRegistry;
import com.roguesmp.registry.ModifierRegistry;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemAttributeModifiers;
import io.papermc.paper.datacomponent.item.ItemLore;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import io.papermc.paper.persistence.PersistentDataContainerView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Represent items with modifiers applied (in-game items)
 */
public class SmpItem {
    private final BaseItem baseItem;
    private ItemStack itemStack;
    private boolean loadedModifier = false;

    private final Map<String, ItemComponent> componentMap = new HashMap<>();

    public SmpItem(@NotNull ItemStack itemStack) {
        this.itemStack = itemStack.clone();
        PersistentDataContainerView pdc = itemStack.getPersistentDataContainer();

        String itemId = pdc.get(Keys.ITEM_ID, PersistentDataType.STRING);
        if (itemId != null) {
            this.baseItem = ItemRegistry.getInstance().getBaseItem(itemId);
        } else this.baseItem = null;

        if (baseItem != null) {
            loadData(pdc);
        }
    }

    public SmpItem(@NotNull BaseItem baseItem) {
        this.baseItem = baseItem;
        this.itemStack = ItemStack.of(baseItem.getBase());

        loadData(this.itemStack.getPersistentDataContainer());
    }

    public @Nullable <T extends ItemComponent> T getComponent(ComponentKey<T> key) {
        return (T) componentMap.get(key.id());
    }

    public @NotNull <T extends ItemComponent> T getOrCreate(ComponentKey<T> key, Supplier<T> supplier) {
        T comp = getComponent(key);
        if (comp == null) {
            comp = supplier.get();
            componentMap.put(key.id(), comp);
        }
        return comp;
    }

    public <T extends ItemComponent> T setComponent(ComponentKey<T> key, T component) {
        return (T) componentMap.put(key.id(), component);
    }

    public ItemStack getInputItemStack() {
        return itemStack;
    }

    public ItemStack generateItemStack(int stackAmount) {
        return generateItemStack(null, stackAmount);
    }

    public ItemStack generateItemStack(@Nullable SmpPlayer player, int stackAmount) {
        if (baseItem == null) return itemStack;
        ItemStack result = ItemStack.of(baseItem.getBase(), stackAmount);

        result.setData(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplay.tooltipDisplay().hiddenComponents(Set.of(DataComponentTypes.ENCHANTMENTS, DataComponentTypes.ATTRIBUTE_MODIFIERS)).build());
        result.setData(DataComponentTypes.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.itemAttributes().build());

        PersistentDataContainerView oldData = itemStack.getPersistentDataContainer();
        LoreBuilder loreBuilder = new LoreBuilder();

        result.editPersistentDataContainer(pdc -> {

            oldData.copyTo(pdc, true);
            pdc.set(Keys.ITEM_ID, PersistentDataType.STRING, baseItem.getId());
            ItemDataContext dataContext = new ItemDataContext(this, player, result, itemStack, pdc);
            ItemLoreContext loreContext = new ItemLoreContext(this, player, loreBuilder, pdc);

            applyModifiers(player);

            componentMap.forEach((s, itemComponent) -> {
                itemComponent.save(pdc);
                itemComponent.modifyStack(dataContext);
                itemComponent.contributeLore(loreContext);
            });
        });

        result.setData(DataComponentTypes.LORE, ItemLore.lore(loreBuilder.build()));

        itemStack = result;

        return result;
    }

    public void applyModifiers(@Nullable SmpPlayer player) {
        if (loadedModifier) return;
        ModifierRegistry.getInstance().getModifiers().forEach((itemModifierType, itemModifier) -> {
            itemModifier.collectAndApply(this, player);
        });
        loadedModifier = true;
    }

    private void loadData(PersistentDataContainerView pdc) {
        baseItem.getComponents().forEach((s, itemComponent) -> {
            componentMap.put(s, itemComponent.copy());
        });
        componentMap.forEach((s, itemComponent) -> {
            itemComponent.load(pdc);
        });
    }
}
