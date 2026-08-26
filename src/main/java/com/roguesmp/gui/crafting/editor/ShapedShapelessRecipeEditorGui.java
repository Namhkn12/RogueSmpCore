package com.roguesmp.gui.crafting.editor;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.crafting.CraftingIngredient;
import com.roguesmp.crafting.CraftingManager;
import com.roguesmp.crafting.recipe.CraftingRecipe;
import com.roguesmp.crafting.recipe.ShapedCraftingRecipe;
import com.roguesmp.crafting.recipe.ShapelessCraftingRecipe;
import com.roguesmp.gui.BaseGui;
import com.roguesmp.gui.crafting.RecipeBrowserGui;
import com.roguesmp.registry.Registries;
import com.roguesmp.utils.PlayerUtils;
import com.roguesmp.utils.Utils;
import com.roguesmp.utils.dialog.DialogBuilder;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

public class ShapedShapelessRecipeEditorGui extends BaseGui {
    private static final int[] GRID_SLOTS = {10, 11, 12, 19, 20, 21, 28, 29, 30};
    private static final int GRID_WIDTH = 3;
    private static final int GRID_HEIGHT = 3;
    private static final int RESULT_SLOT = 23;
    private static final int BACK_SLOT = 45;
    private static final int RECIPE_ID_SETTER = 48;
    private static final int SAVE_SLOT_SHAPED = 49;
    private static final int SAVE_SLOT_SHAPELESS = 50;
    private static final int SHAPED_MIRRORED = 51;
    private static final int DELETE_SLOT = 53;

    private static final String SYMBOL_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private final Player player;
    private final boolean isPreview;

    private String recipeId;
    private boolean mirrored = true;
    private boolean refundItem = true;
    private @Nullable CraftingRecipe sourceRecipe;

    public ShapedShapelessRecipeEditorGui(Player player) {
        this(player, false, "Shaped Recipe");
    }

    private ShapedShapelessRecipeEditorGui(Player player, boolean isPreview, String title) {
        super(Component.text(title, NamedTextColor.DARK_GRAY), 6);
        this.player = player;
        this.isPreview = isPreview;
    }

    /** Opens this editor pre-filled from an existing recipe (id, grid/result contents, mirrored flag) - used by the recipe viewer's right-click-to-edit. */
    public static ShapedShapelessRecipeEditorGui editExisting(Player player, CraftingRecipe recipe) {
        ShapedShapelessRecipeEditorGui gui = new ShapedShapelessRecipeEditorGui(player, false, "Shaped Recipe");
        gui.prefill(recipe, false);
        return gui;
    }

    /** Opens a locked, read-only view of an existing recipe - used by the recipe viewer's left-click-to-inspect. */
    public static ShapedShapelessRecipeEditorGui preview(Player player, CraftingRecipe recipe) {
        ShapedShapelessRecipeEditorGui gui = new ShapedShapelessRecipeEditorGui(player, true, "Công thức: " + recipe.getId());
        gui.refundItem = false;
        gui.prefill(recipe, true);
        return gui;
    }

    private void prefill(CraftingRecipe recipe, boolean withLabel) {
        this.recipeId = recipe.getId();
        this.sourceRecipe = recipe;

        Inventory inv = getInventory();
        if (recipe instanceof ShapedCraftingRecipe shaped) {
            this.mirrored = shaped.isMirrored();

            List<String> pattern = shaped.getPattern();
            Map<String, CraftingIngredient> keyMap = shaped.getKeyMap();
            for (int row = 0; row < pattern.size() && row < GRID_HEIGHT; row++) {
                String line = pattern.get(row);
                for (int col = 0; col < line.length() && col < GRID_WIDTH; col++) {
                    char symbol = line.charAt(col);
                    if (symbol == ' ') continue;

                    CraftingIngredient ingredient = keyMap.get(String.valueOf(symbol));
                    ItemStack stack = resolveStack(ingredient, withLabel);
                    if (stack != null) inv.setItem(GRID_SLOTS[row * GRID_WIDTH + col], stack);
                }
            }
        } else if (recipe instanceof ShapelessCraftingRecipe shapeless) {
            List<CraftingIngredient> ingredients = shapeless.getIngredients();
            for (int i = 0; i < ingredients.size() && i < GRID_SLOTS.length; i++) {
                ItemStack stack = resolveStack(ingredients.get(i), withLabel);
                if (stack != null) inv.setItem(GRID_SLOTS[i], stack);
            }
        }

        ItemStack resultStack = resolveStack(recipe.getResult(), withLabel);
        if (resultStack != null) inv.setItem(RESULT_SLOT, resultStack);
    }

    @Override
    public void setup() {
        fillBackground();

        ItemStack backItem = ItemStack.of(Material.BOOK);
        backItem.setData(DataComponentTypes.ITEM_NAME, Component.text("Mở List Recipe"));
        addButton(BACK_SLOT, backItem, event -> {
            event.setCancelled(true);
            new RecipeBrowserGui(player).showInventory(player);
        });

        if (isPreview) {
            addButton(RECIPE_ID_SETTER, infoItem(), ClickHandler.noAction());
            lockContentSlots();
            return;
        }

        addButton(RECIPE_ID_SETTER, recipeIdButton(), event -> {
            event.setCancelled(true);
            openRecipeIdDialog();
        });

        addButton(SAVE_SLOT_SHAPED, saveButton("Lưu (Shaped)", Material.LIME_CONCRETE, "Vị trí nguyên liệu trong lưới quan trọng."), event -> {
            event.setCancelled(true);
            attemptSave(this::buildShapedRecipe);
        });
        addButton(SAVE_SLOT_SHAPELESS, saveButton("Lưu (Shapeless)", Material.CYAN_CONCRETE, "Chỉ cần đúng nguyên liệu, vị trí không quan trọng."), event -> {
            event.setCancelled(true);
            attemptSave(this::buildShapelessRecipe);
        });

        ItemStack mirroredItem;
        if (mirrored) {
            mirroredItem = ItemStack.of(Material.REDSTONE);
            mirroredItem.setData(DataComponentTypes.ITEM_NAME, Component.text("Mirrored: True", NamedTextColor.GREEN));
        } else {
            mirroredItem = ItemStack.of(Material.EMERALD);
            mirroredItem.setData(DataComponentTypes.ITEM_NAME, Component.text("Mirrored: False", NamedTextColor.RED));
        }
        addButton(SHAPED_MIRRORED, mirroredItem, event -> {
            event.setCancelled(true);
            mirrored = !mirrored;
            setup();
        });

        ItemStack delItem = ItemStack.of(Material.LAVA_BUCKET);
        delItem.setData(DataComponentTypes.ITEM_NAME, Component.text("DELETE?", NamedTextColor.RED));
        addButton(DELETE_SLOT, delItem, event -> {
            event.setCancelled(true);
            openDeleteDialog((Player) event.getWhoClicked());
        });
    }

    /** Binds a locked ({@link ClickHandler#noAction()}) handler over whatever's already sitting in each grid/result slot - only meaningful in preview mode. */
    private void lockContentSlots() {
        Inventory inv = getInventory();
        for (int slot : GRID_SLOTS) {
            ItemStack item = inv.getItem(slot);
            if (item != null) addButton(slot, item, ClickHandler.noAction());
        }
        ItemStack result = inv.getItem(RESULT_SLOT);
        if (result != null) addButton(RESULT_SLOT, result, ClickHandler.noAction());
    }

    private void openDeleteDialog(Player player) {
        Dialog dialog = DialogBuilder.create(Component.text("Xác nhận xóa"))
                .canCloseWithEscape(false)
                .addTextBody(Component.text("Xác nhận xóa?", NamedTextColor.RED, TextDecoration.BOLD))
                .confirmation()
                .yesButton(Component.text("Xác nhận"), null, (response, audience) -> {
                    Registries.CRAFTING_RECIPE.removeAndDeleteFiles(RogueSmpCore.getInstance(), recipeId);
                    CraftingManager.getInstance().rebuild();
                    player.sendMessage(Component.text("Deleted recipe: " + recipeId, NamedTextColor.RED));
                    Utils.runLater(() -> new RecipeBrowserGui(player).showInventory(player));
                })
                .noButton(Component.text("Huỷ"), null, (response, audience) -> Utils.runLater(() -> showInventory(player)))
                .build();

        player.showDialog(dialog);
    }

    /** Fills every slot except the grid, the result, and the buttons - never the reverse, so the grid/result contents are never disturbed by a redraw. */
    private void fillBackground() {
        Inventory inv = getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            if (isReservedSlot(i)) continue;
            addButton(i, FILLER_BLACK, ClickHandler.noAction());
        }
    }

    private boolean isReservedSlot(int slot) {
        for (int gridSlot : GRID_SLOTS) {
            if (gridSlot == slot) return true;
        }
        return slot == RESULT_SLOT || slot == RECIPE_ID_SETTER || slot == SAVE_SLOT_SHAPED || slot == SAVE_SLOT_SHAPELESS;
    }

    private ItemStack recipeIdButton() {
        boolean set = recipeId != null && !recipeId.isBlank();
        ItemStack item = ItemStack.of(Material.NAME_TAG);
        item.setData(DataComponentTypes.ITEM_NAME, Component.text("Recipe ID: " + (set ? recipeId : "(chưa đặt)"), set ? NamedTextColor.GREEN : NamedTextColor.RED));
        item.setData(DataComponentTypes.LORE, ItemLore.lore(List.of(
                Utils.text("Click để đặt ID công thức.", NamedTextColor.GRAY),
                Utils.text("Bắt buộc phải đặt trước khi lưu.", NamedTextColor.DARK_GRAY)
        )));
        return item;
    }

    private ItemStack infoItem() {
        ItemStack item = ItemStack.of(Material.NAME_TAG);
        item.setData(DataComponentTypes.ITEM_NAME, Component.text("Recipe ID: " + recipeId, NamedTextColor.GREEN));

        List<Component> lore = new ArrayList<>();
        if (sourceRecipe instanceof ShapedCraftingRecipe shaped) {
            lore.add(Utils.text("Loại: Shaped", NamedTextColor.GRAY));
            lore.add(Utils.text("Pattern: " + String.join(" | ", shaped.getPattern()), NamedTextColor.GRAY));
            lore.add(Utils.text("Mirrored: " + shaped.isMirrored(), NamedTextColor.GRAY));
        } else {
            lore.add(Utils.text("Loại: Shapeless", NamedTextColor.GRAY));
        }
        item.setData(DataComponentTypes.LORE, ItemLore.lore(lore));
        return item;
    }

    private ItemStack saveButton(String label, Material material, String hint) {
        ItemStack item = ItemStack.of(material);
        item.setData(DataComponentTypes.ITEM_NAME, Component.text(label, NamedTextColor.GOLD));
        item.setData(DataComponentTypes.LORE, ItemLore.lore(List.of(Utils.text(hint, NamedTextColor.GRAY))));
        return item;
    }

    // ==========================================
    // RECIPE ID DIALOG
    // ==========================================

    private void openRecipeIdDialog() {
        Dialog dialog = DialogBuilder.create(Component.text("Đặt ID công thức"))
                .canCloseWithEscape(false)
                .addTextInput("value", Component.text("ID, vd: fire_sword_upgrade"), b -> b.initial(recipeId == null ? "" : recipeId).maxLength(128))
                .confirmation()
                .yesButton(Component.text("Xác nhận"), null, (response, audience) -> {
                    String sanitized = sanitizeId(response.getText("value"));
                    if (sanitized.isEmpty()) player.sendMessage(Utils.text("ID không hợp lệ.", NamedTextColor.RED));
                    else this.recipeId = sanitized;
                    Utils.runLater(() -> showInventory(player));
                })
                .noButton(Component.text("Huỷ"), null, (response, audience) -> Utils.runLater(() -> showInventory(player)))
                .build();

        player.showDialog(dialog);
    }

    private static String sanitizeId(@Nullable String raw) {
        if (raw == null) return "";
        return raw.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", "_");
    }

    // ==========================================
    // SAVE
    // ==========================================

    private void attemptSave(Function<String, @Nullable CraftingRecipe> builder) {
        if (recipeId == null || recipeId.isBlank()) {
            player.sendMessage(Utils.text("Bạn cần đặt ID công thức trước khi lưu.", NamedTextColor.RED));
            return;
        }

        CraftingRecipe recipe = builder.apply(recipeId);
        if (recipe == null) {
            player.sendMessage(Utils.text("Công thức không hợp lệ - cần đặt kết quả và ít nhất 1 nguyên liệu.", NamedTextColor.RED));
            return;
        }

        if (Registries.CRAFTING_RECIPE.get(recipeId) != null) {
            player.showDialog(buildOverwriteConfirmDialog(recipe));
            return;
        }

        doSave(recipe);
    }

    private Dialog buildOverwriteConfirmDialog(CraftingRecipe recipe) {
        return DialogBuilder.create(Component.text("Ghi đè công thức?"))
                .canCloseWithEscape(false)
                .addTextBody(Component.text("Đã tồn tại một công thức với ID '" + recipe.getId() + "'. Ghi đè?"))
                .confirmation()
                .yesButton(Component.text("Ghi đè"), null, (response, audience) -> {
                    doSave(recipe);
                    Utils.runLater(() -> showInventory(player));
                })
                .noButton(Component.text("Huỷ"), null, (response, audience) -> Utils.runLater(() -> showInventory(player)))
                .build();
    }

    private void doSave(CraftingRecipe recipe) {
        Registries.CRAFTING_RECIPE.registerAndSave(RogueSmpCore.getInstance(), recipe.getId(), recipe);
        CraftingManager.getInstance().rebuild(); // the trie must re-index or the new/edited recipe won't be matchable until restart

        player.sendMessage(Utils.text("Đã lưu công thức '" + recipe.getId() + "'.", NamedTextColor.GREEN));
    }

    private @Nullable CraftingRecipe buildShapedRecipe(String id) {
        Inventory inv = getInventory();

        ItemStack resultStack = inv.getItem(RESULT_SLOT);
        String resultKey = CraftingIngredient.resolveKey(resultStack);
        if (resultKey == null) return null;

        // One symbol per distinct (key, count) pair - two cells sharing a key but needing different
        // amounts need separate symbols, since a symbol maps to exactly one CraftingIngredient.
        Map<String, String> symbolByCombo = new LinkedHashMap<>();
        StringBuilder[] rows = new StringBuilder[GRID_HEIGHT];
        for (int row = 0; row < GRID_HEIGHT; row++) rows[row] = new StringBuilder();

        boolean anyIngredient = false;
        for (int row = 0; row < GRID_HEIGHT; row++) {
            for (int col = 0; col < GRID_WIDTH; col++) {
                ItemStack stack = inv.getItem(GRID_SLOTS[row * GRID_WIDTH + col]);
                String key = CraftingIngredient.resolveKey(stack);
                if (key == null) {
                    rows[row].append(' ');
                    continue;
                }

                anyIngredient = true;
                String combo = key + "@" + stack.getAmount();
                String symbol = symbolByCombo.computeIfAbsent(combo, c -> {
                    if (symbolByCombo.size() >= SYMBOL_ALPHABET.length()) return null;
                    return String.valueOf(SYMBOL_ALPHABET.charAt(symbolByCombo.size()));
                });
                if (symbol == null) return null; // more distinct (key, count) combos than we have symbols for

                rows[row].append(symbol);
            }
        }
        if (!anyIngredient) return null;

        List<String> pattern = List.of(rows[0].toString(), rows[1].toString(), rows[2].toString());

        Map<String, CraftingIngredient> keyMap = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : symbolByCombo.entrySet()) {
            String combo = entry.getKey();
            int at = combo.lastIndexOf('@');
            keyMap.put(entry.getValue(), new CraftingIngredient(combo.substring(0, at), Integer.parseInt(combo.substring(at + 1))));
        }

        CraftingIngredient result = new CraftingIngredient(resultKey, resultStack.getAmount());
        return new ShapedCraftingRecipe(new CraftingRecipe.BaseProperties(id, result), pattern, keyMap, mirrored, Map.of());
    }

    /**
     * Unlike {@link #buildShapedRecipe}, position doesn't matter - one {@link CraftingIngredient}
     * entry per occupied grid slot (not merged by key), matching {@link ShapelessCraftingRecipe}'s
     * "a duplicate key is a distinct requirement" semantics.
     */
    private @Nullable CraftingRecipe buildShapelessRecipe(String id) {
        Inventory inv = getInventory();

        ItemStack resultStack = inv.getItem(RESULT_SLOT);
        String resultKey = CraftingIngredient.resolveKey(resultStack);
        if (resultKey == null) return null;

        List<CraftingIngredient> ingredients = new ArrayList<>();
        for (int slot : GRID_SLOTS) {
            ItemStack stack = inv.getItem(slot);
            String key = CraftingIngredient.resolveKey(stack);
            if (key == null) continue;
            ingredients.add(new CraftingIngredient(key, stack.getAmount()));
        }
        if (ingredients.isEmpty()) return null;

        CraftingIngredient result = new CraftingIngredient(resultKey, resultStack.getAmount());
        return new ShapelessCraftingRecipe(new CraftingRecipe.BaseProperties(id, result), ingredients);
    }

    /** Resolves an ingredient to a placeable stack - {@code withLabel} additionally appends a "key xN" lore line, used only in {@link #isPreview} mode. */
    private static @Nullable ItemStack resolveStack(@Nullable CraftingIngredient ingredient, boolean withLabel) {
        if (ingredient == null) return null;
        ItemStack resolved = ingredient.toItemStack();
        if (resolved == null) return null;

        ItemStack stack = resolved.clone();
        if (withLabel) {
            List<Component> lore = new ArrayList<>();
            ItemLore existing = stack.getData(DataComponentTypes.LORE);
            if (existing != null) lore.addAll(existing.lines());
            lore.add(Component.empty());
            lore.add(Component.text(ingredient.key() + " x" + ingredient.count(), NamedTextColor.WHITE));
            stack.setData(DataComponentTypes.LORE, ItemLore.lore(lore));
        }
        return stack;
    }

    public void setRefundItem(boolean refundItem) {
        this.refundItem = refundItem;
    }

    @Override
    public void onCloseInventory(InventoryCloseEvent event) {
        if (!refundItem) return;
        Inventory inv = getInventory();
        for (int slot : GRID_SLOTS) {
            ItemStack item = inv.getItem(slot);
            if (item != null && !item.getType().isAir()) PlayerUtils.giveItem(player, item);
        }
        ItemStack result = inv.getItem(RESULT_SLOT);
        if (result != null && !result.getType().isAir()) PlayerUtils.giveItem(player, result);
    }

    @Override
    public void onClickBottomInventory(InventoryClickEvent event) {
        if (isPreview) event.setCancelled(true);
    }
}
