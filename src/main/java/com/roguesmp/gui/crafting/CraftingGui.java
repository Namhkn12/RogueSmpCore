package com.roguesmp.gui.crafting;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.crafting.CraftingManager;
import com.roguesmp.crafting.recipe.CraftingRecipe;
import com.roguesmp.crafting.recipe.CraftingRecipes;
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

/**
 * A vanilla-style 3x3 crafting table, backed by the real {@link CraftingManager} - matches shaped
 * recipes first, then shapeless (see {@link #matchRecipe}), same priority {@code CraftingManager}'s
 * own multi-key {@code match} convenience documents.
 */
public class CraftingGui extends BaseGui {

    private static final int[] CRAFT_SLOTS = new int[]{10, 11, 12, 19, 20, 21, 28, 29, 30};
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
        CraftingRecipe matched = (state.lastGridHash() == currentHash) ? state.lastRecipe() : matchRecipe(getCurrentItems());

        ItemStack resultItem = matched == null ? null : matched.getResultStack();
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

    /**
     * Shaped takes priority over shapeless. Queried separately (rather than through
     * {@code CraftingManager}'s multi-key {@code match} convenience) because shapeless recipes don't
     * care about position - they must be looked up with {@code width}/{@code height} of {@code 0, 0}
     * (see {@link CraftingManager#match(com.roguesmp.crafting.recipe.RecipeKey, ItemStack[], int, int)}),
     * same as {@code FusionGui} does for fusion recipes; passing the grid's actual 3x3 dimensions for
     * both keys would build a positional trie path that a shapeless recipe (indexed unordered) can
     * never match.
     */
    private static @Nullable CraftingRecipe matchRecipe(ItemStack[] items) {
        CraftingRecipe shaped = CraftingManager.getInstance().match(CraftingRecipes.SHAPED, items, GRID_WIDTH, GRID_HEIGHT);
        if (shaped != null) return shaped;
        return CraftingManager.getInstance().match(CraftingRecipes.SHAPELESS, items, 0, 0);
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
        Utils.runLater(this::checkAndSyncState);
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
        Utils.runLater(this::checkAndSyncState);
    }

    private void checkAndSyncState() {
        int newHash = computeGridHash();
        if (state.lastGridHash() == newHash) return;

        CraftingRecipe newRecipe = matchRecipe(getCurrentItems());
        if (state.lastRecipe() == newRecipe) return; // same recipe object (or both null) - nothing actually changed

        this.state = new CraftingState(newHash, newRecipe);
        setup();
    }

    private void handleCraft(InventoryClickEvent event) {
        event.setCancelled(true);

        CraftingRecipe recipe = state.lastRecipe();
        if (recipe == null) return;

        ItemStack result = recipe.getResultStack();
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

            applyConsumedItems(recipe.consume(items, GRID_WIDTH, GRID_HEIGHT));
        } else {
            int maxCrafts = getMaxCraftsByInventory(recipe, items, cleanResultItem);
            if (maxCrafts <= 0) return;

            for (int i = 0; i < maxCrafts; i++) {
                player.getInventory().addItem(cleanResultItem.clone());
            }

            ItemStack[] leftover = items;
            for (int i = 0; i < maxCrafts; i++) {
                leftover = recipe.consume(leftover, GRID_WIDTH, GRID_HEIGHT);
            }
            applyConsumedItems(leftover);
        }

        Utils.runLater(this::checkAndSyncState);
    }

    private void applyConsumedItems(ItemStack[] leftover) {
        Inventory inv = getInventory();
        for (int i = 0; i < CRAFT_SLOTS.length; i++) {
            inv.setItem(CRAFT_SLOTS[i], leftover[i]);
        }
    }

    private int getMaxCraftsByInventory(CraftingRecipe recipe, ItemStack[] items, ItemStack resultItem) {
        int maxCraftsByGrid = simulateMaxCrafts(recipe, items);
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

    public record CraftingState(int lastGridHash, @Nullable CraftingRecipe lastRecipe) {
    }

    public static void registerCmd() {
        new CommandAPICommand("crafting")
                .executesPlayer((player1, commandArguments) -> {
                    new CraftingGui(player1).showInventory(player1);
                })
                .register(RogueSmpCore.getInstance());
    }
}
