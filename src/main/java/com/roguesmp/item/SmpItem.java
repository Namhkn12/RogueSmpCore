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
import com.roguesmp.item.modifier.ModifierRegistry;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import io.papermc.paper.persistence.PersistentDataContainerView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Represent our custom items. To get a cached SmpItem, use {@link ItemManager#}
 */
public class SmpItem {
    private final BaseItem baseItem;
    private final UUID uuid;
    private ItemStack itemStack;

    private final Map<String, ItemComponent> componentMap = new HashMap<>();

    /**
     * Create an SmpItem instance, and load base data from {@link BaseItem} and pdc. To fully update the components, call {@link SmpItem#applyModifiers(SmpPlayer)} <br>
     * To get a cached SmpItem, use {@link ItemManager#wrapItem(ItemStack, SmpPlayer)}
     */
    public SmpItem(@NotNull ItemStack itemStack) {
        this.itemStack = itemStack;
        PersistentDataContainerView pdc = itemStack.getPersistentDataContainer();

        String itemId = pdc.get(Keys.ITEM_ID, PersistentDataType.STRING);
        if (itemId != null) {
            this.baseItem = ItemRegistry.getInstance().getBaseItem(itemId);
            String uuidStr = pdc.get(Keys.ITEM_UUID, PersistentDataType.STRING);
            if (uuidStr == null) this.uuid = null;
            else this.uuid = UUID.fromString(uuidStr);
        } else {
            this.baseItem = null;
            this.uuid = null;
        }

        if (baseItem != null) {
            loadData(pdc);
        }
    }

    /**
     * Create an SmpItem instance, and load base data from {@link BaseItem}.
     */
    public SmpItem(@NotNull BaseItem baseItem) {
        this.baseItem = baseItem;
        this.itemStack = ItemStack.of(baseItem.getBase());
        if (baseItem.isUnique()) this.uuid = UUID.randomUUID();
        else this.uuid = null;

        loadData(this.itemStack.getPersistentDataContainer());
    }

    public static SmpItem wrap(ItemStack itemStack) {
        return ItemManager.getInstance().wrapItem(itemStack);
    }

    public static SmpItem wrap(ItemStack itemStack, @Nullable SmpPlayer smpPlayer) {
        return ItemManager.getInstance().wrapItem(itemStack, smpPlayer);
    }

    public static SmpItem wrap(ItemStack itemStack, SmpPlayer smpPlayer, @Nullable Consumer<SmpItem> onCacheMiss) {
        return ItemManager.getInstance().wrapItem(itemStack, smpPlayer, onCacheMiss);
    }

    @SuppressWarnings("unchecked")
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

    @SuppressWarnings("unchecked")
    public <T extends ItemComponent> T setComponent(ComponentKey<T> key, T component) {
        return (T) componentMap.put(key.id(), component);
    }

    @SuppressWarnings("unchecked")
    public <T extends ItemComponent> T unsetComponent(ComponentKey<T> key) {
        return (T) componentMap.remove(key.id());
    }

    public <T extends ItemComponent> boolean hasComponent(ComponentKey<T> key) {
        return componentMap.containsKey(key.id());
    }

    public @Unmodifiable Map<String, ItemComponent> getComponents() {
        return Collections.unmodifiableMap(componentMap);
    }

    public BaseItem getBaseItem() {
        return baseItem;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getId() {
        return baseItem.getId();
    }

    /**
     * Transforms this wrapper into a Minecraft {@link ItemStack}.
     * <p>
     * This method is an overload of {@link #generateItemStack(SmpPlayer, int)} with player being null.
     * </p>
     *
     * @param stackAmount The amount for the resulting stack.
     * @return A fully processed ItemStack with custom lore and PDC data.
     */
    public ItemStack generateItemStack(int stackAmount) {
        return generateItemStack(null, stackAmount);
    }

    /**
     * Transforms this wrapper into a Minecraft {@link ItemStack}.
     * <p>
     * This method automatically triggers {@link #applyModifiers(SmpPlayer)}.
     * </p>
     *
     * @param player Context for player-specific lore or modifiers.
     * @param stackAmount The amount for the resulting stack.
     * @return A fully processed ItemStack with custom lore and PDC data.
     */
    public ItemStack generateItemStack(@Nullable SmpPlayer player, int stackAmount) {
        if (baseItem == null) return itemStack;

        ItemStack result = ItemStack.of(baseItem.getBase(), stackAmount);

        result.setData(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplay.tooltipDisplay().hiddenComponents(Set.of(DataComponentTypes.ENCHANTMENTS, DataComponentTypes.ATTRIBUTE_MODIFIERS, DataComponentTypes.UNBREAKABLE)).build());
        result.unsetData(DataComponentTypes.ATTRIBUTE_MODIFIERS);
        result.setData(DataComponentTypes.UNBREAKABLE);
        result.setData(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, false);
//        result.setData(DataComponentTypes.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.itemAttributes().build());

        PersistentDataContainerView oldData = itemStack.getPersistentDataContainer();
        LoreBuilder loreBuilder = new LoreBuilder();

        result.editPersistentDataContainer(pdc -> {

            oldData.copyTo(pdc, true);
            pdc.set(Keys.ITEM_ID, PersistentDataType.STRING, baseItem.getId());
            ItemDataContext dataContext = new ItemDataContext(this, player, result, pdc);
            ItemLoreContext loreContext = new ItemLoreContext(this, player, loreBuilder, pdc);

            applyModifiers(player);

            for (Map.Entry<String, ItemComponent> entry : componentMap.entrySet()) {
                ItemComponent itemComponent = entry.getValue();
                itemComponent.save(pdc);
                itemComponent.modifyStack(dataContext);
                itemComponent.contributeLore(loreContext);
            }
        });

        result.setData(DataComponentTypes.LORE, ItemLore.lore(loreBuilder.build()));

        this.itemStack = result;

        return result;
    }

    /**
     * Applies all registered {@link ItemModifier}s to this item.
     * <p>
     * <strong>Note:</strong> This method uses a guard flag. It will only execute logic once
     * per instance. Subsequent calls will do nothing to prevent duplicate stat stacking.
     * </p>
     */
    public void applyModifiers(@Nullable SmpPlayer player) {
        List<ItemModifier> modifierList = ModifierRegistry.getModifiers();
        for (ItemModifier itemModifier : modifierList) {
            itemModifier.collectAndApply(this, player);
        }
    }

    private void loadData(PersistentDataContainerView pdc) {
        for (Map.Entry<String, ItemComponent> entry : baseItem.getComponents().entrySet()) {
            ItemComponent copy = entry.getValue().copy();
            componentMap.put(entry.getKey(), copy);
            copy.load(pdc);
        }
    }
}
