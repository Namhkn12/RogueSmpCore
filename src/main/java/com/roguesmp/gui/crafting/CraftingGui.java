package com.roguesmp.gui.crafting;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.crafting.CraftingManager;
import com.roguesmp.crafting.recipe.CraftingRecipe;
import com.roguesmp.crafting.recipe.CraftingRecipes;
import com.roguesmp.gui.ReactiveGui;
import com.roguesmp.utils.PlayerUtils;
import com.roguesmp.utils.SmpItemUtils;
import com.roguesmp.utils.Utils;
import dev.jorel.commandapi.CommandAPICommand;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MenuType;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * A vanilla-style 3x3 crafting table, backed by {@link CraftingManager} - matches shaped
 * recipes first, then shapeless (see {@link #matchCustomRecipe}), then falls back to
 * vanilla recipe via {@link Bukkit#craftItem} (see {@link #craftVanillaResult}).
 */
public class CraftingGui extends ReactiveGui<CraftingGui.CraftingState> {

    private static final int[] CRAFT_SLOTS = new int[]{10, 11, 12, 19, 20, 21, 28, 29, 30};
    private static final int TABLE_SHORTCUT_SLOT = 50;
    private static final int RESULT_SLOT = 23;
    private static final int GRID_WIDTH = 3;
    private static final int GRID_HEIGHT = 3;

    /** Safety cap on shift-click multi-craft simulation - no vanilla stack exceeds 64, so no single-slot-depleting recipe ever needs more than that. */
    private static final int MAX_SIMULATED_CRAFTS = 64;

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
    );

    private final Player player;

    public CraftingGui(Player player) {
        super(Component.text("Bàn Chế Tạo", NamedTextColor.DARK_GRAY), 6);
        this.player = player;
        initState(new CraftingState(0, null, null));
    }

    private boolean isCraftingGridSlot(int slot) {
        for (int craftSlot : CRAFT_SLOTS) {
            if (craftSlot == slot) return true;
        }
        return false;
    }

    @Override
    protected CraftingState computeState() {
        int currentHash = computeGridHash();
        CraftingState current = getState();
        if (current.lastGridHash() == currentHash) return current;

        ItemStack[] items = getCurrentItems();
        CraftingRecipe custom = matchCustomRecipe(items);
        ItemStack vanillaResult = custom != null ? null : craftVanillaResult(items);
        return new CraftingState(currentHash, custom, vanillaResult);
    }

    @Override
    protected void render(CraftingState state) {
        ItemStack resultItem = state.result();
        boolean hasValidRecipe = resultItem != null && !resultItem.getType().isAir();

        Material borderMaterial = hasValidRecipe ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;
        fillBackgroundDecorations(borderMaterial);

        addButton(49, createCloseButton(), event -> player.closeInventory());
        ItemStack tableShortcut = ItemStack.of(Material.CRAFTING_TABLE);
        tableShortcut.setData(DataComponentTypes.ITEM_NAME, Component.text("Dùng bàn chế tạo vanilla", NamedTextColor.GREEN));
        tableShortcut.setData(DataComponentTypes.LORE, ItemLore.lore(List.of(Utils.text("Lưu ý: Công thức custom sẽ không hoạt động ở bàn này!", NamedTextColor.RED))));
        addButton(TABLE_SHORTCUT_SLOT, tableShortcut, event -> {
            event.setCancelled(true);
            event.getWhoClicked().openInventory(MenuType.CRAFTING.create(event.getWhoClicked()));
        });
        if (hasValidRecipe) {
            // Apply visual lore to a copy for the GUI display
            ItemStack resultWithLore = resultItem.clone();
            applyResultLore(resultWithLore);
            addButton(RESULT_SLOT, resultWithLore, this::handleCraft);
        } else {
            ItemStack barrier = ItemStack.of(Material.BARRIER);
            barrier.setData(DataComponentTypes.ITEM_NAME, Component.text("Thành phẩm", NamedTextColor.RED));
            addButton(RESULT_SLOT, barrier, ClickHandler.noAction());
        }
    }

    private @Nullable CraftingRecipe matchCustomRecipe(ItemStack[] items) {
        CraftingRecipe shaped = CraftingManager.getInstance().match(CraftingRecipes.SHAPED, items, GRID_WIDTH, GRID_HEIGHT);
        if (shaped != null) return shaped;

        return CraftingManager.getInstance().match(CraftingRecipes.SHAPELESS, items, 0, 0);
    }

    /**
     * Falls back to vanilla recipe via {@link Bukkit#craftItem}
     */
    private @Nullable ItemStack craftVanillaResult(ItemStack[] items) {
        if (containsCustomItem(items)) return null;

        ItemStack[] matrix = new ItemStack[items.length];
        for (int i = 0; i < items.length; i++) {
            matrix[i] = items[i] == null ? ItemStack.empty() : items[i];
        }

        ItemStack result = Bukkit.craftItem(matrix, player.getWorld());
        return result.getType().isAir() ? null : result;
    }

    /** Blocks our own items from being used as vanilla recipe ingredients. */
    private static boolean containsCustomItem(ItemStack[] items) {
        for (ItemStack item : items) {
            if (item != null && SmpItemUtils.getBaseItem(item) != null) return true;
        }
        return false;
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

        super.onClickTopInventory(event);
    }

    @Override
    public void onClickBottomInventory(InventoryClickEvent event) {
        if (DISALLOWED_ACTION.contains(event.getAction())) {
            event.setCancelled(true);
            return;
        }
        super.onClickBottomInventory(event);
    }

    private void handleCraft(InventoryClickEvent event) {
        event.setCancelled(true);

        CraftingState state = getState();
        ItemStack result = state.result();
        if (result == null) return;

        ItemStack cleanResultItem = result.clone();
        ItemStack[] items = getCurrentItems();

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

            applyConsumedItems(consume(state.customRecipe(), items, 1));
        } else {
            int maxCrafts = getMaxCraftsByInventory(state.customRecipe(), items, cleanResultItem);
            if (maxCrafts <= 0) return;

            for (int i = 0; i < maxCrafts; i++) {
                player.getInventory().addItem(cleanResultItem.clone());
            }

            applyConsumedItems(consume(state.customRecipe(), items, maxCrafts));
        }

        Utils.runLater(this::syncStateInventory);
    }

    /** Applies {@code crafts} consecutive crafts against {@code items} - via {@code customRecipe} if this was a custom match, or the generic vanilla consumption otherwise. */
    private static ItemStack[] consume(@Nullable CraftingRecipe customRecipe, ItemStack[] items, int crafts) {
        if (customRecipe == null) return consumeVanilla(items, crafts);

        ItemStack[] current = items;
        for (int i = 0; i < crafts; i++) current = customRecipe.consume(current, GRID_WIDTH, GRID_HEIGHT);
        return current;
    }

    /**
     * A vanilla shaped/shapeless recipe always needs exactly 1 of each occupied ingredient per
     * craft, so {@code crafts} consecutive crafts is just "decrement every
     * occupied slot by {@code crafts}"
     */
    private static ItemStack[] consumeVanilla(ItemStack[] items, int crafts) {
        ItemStack[] result = new ItemStack[items.length];
        for (int i = 0; i < items.length; i++) {
            ItemStack stack = items[i];
            if (stack == null || stack.getType().isAir()) continue;

            int leftover = stack.getAmount() - crafts;
            if (leftover > 0) {
                ItemStack copy = stack.clone();
                copy.setAmount(leftover);
                result[i] = copy;
            }
        }
        return result;
    }

    private void applyConsumedItems(ItemStack[] leftover) {
        Inventory inv = getInventory();
        for (int i = 0; i < CRAFT_SLOTS.length; i++) {
            inv.setItem(CRAFT_SLOTS[i], leftover[i]);
        }
    }

    private int getMaxCraftsByInventory(@Nullable CraftingRecipe customRecipe, ItemStack[] items, ItemStack resultItem) {
        int maxCraftsByGrid = customRecipe == null ? maxCraftsByGridSimple(items) : simulateMaxCrafts(customRecipe, items);
        if (maxCraftsByGrid <= 0) return 0;

        // Calculate the total individual item capacity of the player's inventory
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

        // How many full craft executions fit in that capacity?
        // (e.g. if we have room for 6 individual diamonds, and the recipe yields 4, we can only run 1 craft)
        int yieldPerCraft = resultItem.getAmount();
        int maxCraftsBySpace = availableCapacityInUnits / yieldPerCraft;

        return Math.min(maxCraftsByGrid, maxCraftsBySpace);
    }

    /**
     * Repeatedly consumes a scratch copy of {@code items} against {@code recipe} until it no longer
     * matches, counting how many times that took - correctly accounts for per-cell amounts and
     * remainders (e.g. a slot that turns into a still-valid ingredient after one craft), unlike a
     * naive "divide each slot's amount by what one craft needs" estimate.
     */
    private static int simulateMaxCrafts(CraftingRecipe recipe, ItemStack[] items) {
        ItemStack[] current = items;
        int crafts = 0;
        while (crafts < MAX_SIMULATED_CRAFTS && recipe.matches(current, GRID_WIDTH, GRID_HEIGHT)) {
            current = recipe.consume(current, GRID_WIDTH, GRID_HEIGHT);
            crafts++;
        }
        return crafts;
    }

    /** A vanilla recipe always needs exactly 1 per occupied slot per craft, so the grid's own capacity is just the smallest occupied stack. */
    private static int maxCraftsByGridSimple(ItemStack[] items) {
        int max = Integer.MAX_VALUE;
        boolean anyOccupied = false;
        for (ItemStack stack : items) {
            if (stack == null || stack.getType().isAir()) continue;
            anyOccupied = true;
            max = Math.min(max, stack.getAmount());
        }
        return anyOccupied ? max : 0;
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
            hash = 31 * hash + item.hashCode();
        }
        return hash;
    }

    private ItemStack[] getCurrentItems() {
        ItemStack[] items = new ItemStack[CRAFT_SLOTS.length];
        Inventory inv = getInventory();
        for (int i = 0; i < CRAFT_SLOTS.length; i++) {
            items[i] = inv.getItem(CRAFT_SLOTS[i]);
        }
        return items;
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

    private ItemStack createCloseButton() {
        ItemStack barrier = ItemStack.of(Material.BARRIER);
        barrier.setData(DataComponentTypes.ITEM_NAME, Component.text("Đóng", NamedTextColor.RED));
        return barrier;
    }

    public record CraftingState(int lastGridHash, @Nullable CraftingRecipe customRecipe, @Nullable ItemStack vanillaResult) {
        private @Nullable ItemStack result() {
            return customRecipe != null ? customRecipe.getResultStack() : vanillaResult;
        }
    }

    public static void registerCmd() {
        new CommandAPICommand("crafting")
                .executesPlayer((player1, commandArguments) -> {
                    new CraftingGui(player1).showInventory(player1);
                })
                .register(RogueSmpCore.getInstance());
    }
}
