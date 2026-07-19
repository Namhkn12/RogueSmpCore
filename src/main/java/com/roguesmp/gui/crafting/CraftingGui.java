package com.roguesmp.gui.crafting;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.gui.BaseGui;
import com.roguesmp.utils.PlayerUtils;
import com.roguesmp.utils.Utils;
import dev.jorel.commandapi.CommandAPICommand;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class CraftingGui extends BaseGui {

    private static final int[] CRAFT_SLOTS = new int[]{10, 11, 12, 19, 20, 21, 28, 29, 30};
    private static final int RESULT_SLOT = 23;

    // Visual lore for the preview in the GUI
    private static final List<Component> RESULT_LORE = List.of(Utils.text("------------------", NamedTextColor.DARK_GRAY),
            Utils.text("Thành phẩm chế tạo", NamedTextColor.DARK_GRAY));

    private static final Set<InventoryAction> DISALLOWED_ACTION = EnumSet.of(
            InventoryAction.HOTBAR_SWAP,
            InventoryAction.PICKUP_FROM_BUNDLE,
            InventoryAction.PLACE_FROM_BUNDLE,
            InventoryAction.PICKUP_ALL_INTO_BUNDLE,
            InventoryAction.PLACE_ALL_INTO_BUNDLE,
            InventoryAction.PICKUP_SOME_INTO_BUNDLE,
            InventoryAction.PLACE_SOME_INTO_BUNDLE
    ); //Also include drag, inside InventoryDragEvent

    private final Player player;
    private CraftingState state;

    public CraftingGui(Player player) {
        super(Component.text("Bàn Chế Tạo", NamedTextColor.DARK_GRAY), 6);
        this.player = player;
        this.state = new CraftingState(0, null);
    }

    private boolean isCraftingGridSlot(int slot) {
        for (int craftSlot : CRAFT_SLOTS) {
            if (craftSlot == slot) return true;
        }
        return false;
    }

    @Override
    public void setup() {
        int currentHash = computeGridHash();
        ItemStack resultItem = (state.lastGridHash() == currentHash)
                ? state.lastResult()
                : calculateCraftingResult(getCurrentRecipeStacks());

        boolean hasValidRecipe = resultItem != null && !resultItem.getType().isAir();

        Material borderMaterial = hasValidRecipe ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;
        fillBackgroundDecorations(borderMaterial);

        addButton(49, createCloseButton(), event -> player.closeInventory());

        if (hasValidRecipe) {
            // Apply visual lore to a copy for the GUI display
            ItemStack resultWithLore = resultItem.clone();
            applyResultLore(resultWithLore);
            addButton(RESULT_SLOT, resultWithLore, this::handleCraft);
        } else {
            ItemStack barrier = ItemStack.of(Material.BARRIER);
            addButton(RESULT_SLOT, barrier, ClickHandler.noAction());
        }
    }

    private void applyResultLore(ItemStack item) {
        ItemLore itemLore = item.getData(DataComponentTypes.LORE);
        List<Component> lore = itemLore != null ? new ArrayList<>(itemLore.lines()) : new ArrayList<>();
        lore.addAll(RESULT_LORE);
        item.setData(DataComponentTypes.LORE, ItemLore.lore(lore));
    }

    private void fillBackgroundDecorations(Material borderMaterial) {
        Inventory inv = getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            if (isCraftingGridSlot(i) || i == RESULT_SLOT || i == 49) continue;

            if (getRow(i) == 5) {
                ItemStack border = ItemStack.of(borderMaterial);
                addButton(i, border, ClickHandler.noAction());
            } else {
                addButton(i, FILLER_BLACK, ClickHandler.noAction());
            }
        }
    }

    @Override
    public void onClickTopInventory(InventoryClickEvent event) {
        if (DISALLOWED_ACTION.contains(event.getAction())) {
            event.setCancelled(true);
            return;
        }

        int slot = event.getSlot();

        if (isCraftingGridSlot(slot)) {
            Utils.runLater(this::checkAndSyncState);
            return;
        }

        super.onClickTopInventory(event);
    }

    @Override
    public void onClickBottomInventory(InventoryClickEvent event) {
        if (DISALLOWED_ACTION.contains(event.getAction())) {
            event.setCancelled(true);
            return;
        }
        Utils.runLater(this::checkAndSyncState);
    }

    @Override
    public void onDragInventory(InventoryDragEvent event) {
        event.setCancelled(true);
    }

    private void checkAndSyncState() {
        int newHash = computeGridHash();
        if (state.lastGridHash() == newHash) return;

        ItemStack newResult = calculateCraftingResult(getCurrentRecipeStacks());

        if (state.lastGridHash() == newHash && Objects.equals(state.lastResult(), newResult)) {
            return;
        }

        this.state = new CraftingState(newHash, newResult);
        setup();
    }

    private void handleCraft(InventoryClickEvent event) {
        event.setCancelled(true);

        ItemStack cleanCachedResult = state.lastResult();
        if (cleanCachedResult == null || cleanCachedResult.getType().isAir()) return;

        ItemStack cleanResultItem = cleanCachedResult.clone();

        ItemStack cursor = event.getView().getCursor();
        boolean isShift = event.isShiftClick();

        if (!isShift) {
            Material cursorType = cursor.getType();
            if (!cursorType.isAir() && !cursor.isSimilar(cleanResultItem)) return;
            if (!cursorType.isAir() && (cursor.getAmount() + cleanResultItem.getAmount() > cursor.getMaxStackSize())) return;

            if (cursorType.isAir()) {
                event.getView().setCursor(cleanResultItem);
            } else {
                cursor.setAmount(cursor.getAmount() + cleanResultItem.getAmount());
            }

            consumeIngredients(1);
        } else {
            int maxCrafts = getMaxCraftsByInventory(cleanResultItem);
            if (maxCrafts <= 0) return;

            int actualCraftsCompleted = 0;

            for (int i = 0; i < maxCrafts; i++) {
                ItemStack craftedUnit = cleanResultItem.clone();

                player.getInventory().addItem(craftedUnit);
                actualCraftsCompleted++;
            }

            if (actualCraftsCompleted > 0) {
                consumeIngredients(actualCraftsCompleted);
            }
        }

        Utils.runLater(this::checkAndSyncState);
    }

    private void consumeIngredients(int amountToConsume) {
        Inventory inv = getInventory();
        for (int slot : CRAFT_SLOTS) {
            ItemStack item = inv.getItem(slot);
            if (item != null && !item.getType().isAir()) {
                int newAmount = item.getAmount() - amountToConsume;
                if (newAmount <= 0) {
                    inv.setItem(slot, null);
                } else {
                    item.setAmount(newAmount);
                }
            }
        }
    }

    private int getMaxCraftsByInventory(ItemStack resultItem) {
        int maxCraftsByGrid = Integer.MAX_VALUE;
        Inventory inv = getInventory();

        // 1. Calculate how many we can craft based purely on available grid ingredients
        for (int slot : CRAFT_SLOTS) {
            ItemStack item = inv.getItem(slot);
            if (item != null && !item.getType().isAir()) {
                maxCraftsByGrid = Math.min(maxCraftsByGrid, item.getAmount());
            }
        }

        if (maxCraftsByGrid == Integer.MAX_VALUE || maxCraftsByGrid <= 0) return 0;

        // 2. Calculate the total individual item capacity of the player's inventory
        int maxStackSize = resultItem.getMaxStackSize();
        int availableCapacityInUnits = 0;

        // Only scan the main storage contents (slots 0-35), ignoring armor and off-hand
        ItemStack[] storage = player.getInventory().getStorageContents();
        for (ItemStack stack : storage) {
            if (stack == null || stack.getType().isAir()) {
                availableCapacityInUnits += maxStackSize;
            } else if (stack.isSimilar(resultItem)) {
                availableCapacityInUnits += Math.max(0, maxStackSize - stack.getAmount());
            }
        }

        // 3. How many full craft executions fit in that capacity?
        // (e.g. if we have room for 6 individual diamonds, and the recipe yields 4, we can only run 1 craft)
        int yieldPerCraft = resultItem.getAmount();
        int maxCraftsBySpace = availableCapacityInUnits / yieldPerCraft;

        return Math.min(maxCraftsByGrid, maxCraftsBySpace);
    }

    private int computeGridHash() {
        int hash = 1;
        Inventory inv = getInventory();
        for (int slot : CRAFT_SLOTS) {
            ItemStack item = inv.getItem(slot);
            if (item == null || item.getType().isAir()) {
                hash = 31 * hash;
                continue;
            }
            hash = 31 * hash + item.getType().hashCode();
            hash = 31 * hash + item.getAmount();
        }
        return hash;
    }

    private ItemStack[] getCurrentRecipeStacks() {
        ItemStack[] stacks = new ItemStack[9];
        Inventory inv = getInventory();
        for (int i = 0; i < CRAFT_SLOTS.length; i++) {
            ItemStack item = inv.getItem(CRAFT_SLOTS[i]);
            stacks[i] = (item == null) ? ItemStack.of(Material.AIR) : item;
        }
        return stacks;
    }

    @Override
    public void onCloseInventory(InventoryCloseEvent event) {
        Inventory inv = getInventory();
        for (int slot : CRAFT_SLOTS) {
            ItemStack item = inv.getItem(slot);
            if (item != null && !item.getType().isAir()) {
                PlayerUtils.giveItem(player, item);
            }
        }
    }

    private @Nullable ItemStack calculateCraftingResult(ItemStack[] grid) {
        for (ItemStack item : grid) {
            if (item == null || item.getType() != Material.COAL) {
                return null;
            }
        }
        return ItemStack.of(Material.DIAMOND, 32);
    }

    private ItemStack createCloseButton() {
        ItemStack barrier = ItemStack.of(Material.BARRIER);
        barrier.setData(DataComponentTypes.ITEM_NAME, Component.text("Đóng", NamedTextColor.RED));
        return barrier;
    }

    public record CraftingState(int lastGridHash, @Nullable ItemStack lastResult) {
    }

    public static void registerCmd() {
        new CommandAPICommand("crafting")
                .executesPlayer((player1, commandArguments) -> {
                    new CraftingGui(player1).showInventory(player1);
                })
                .register(RogueSmpCore.getInstance());
    }
}