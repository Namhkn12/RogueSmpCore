package com.roguesmp.gui.crafting.editor;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.crafting.CraftingIngredient;
import com.roguesmp.crafting.CraftingManager;
import com.roguesmp.crafting.recipe.CraftingRecipe;
import com.roguesmp.crafting.recipe.FusionRecipe;
import com.roguesmp.gui.BaseGui;
import com.roguesmp.gui.crafting.RecipeBrowserGui;
import com.roguesmp.registry.Registries;
import com.roguesmp.utils.PlayerUtils;
import com.roguesmp.utils.Utils;
import com.roguesmp.utils.dialog.DialogBuilder;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import io.papermc.paper.dialog.Dialog;
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
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

public class FusionRecipeEditorGui extends BaseGui {

    private static final int[] INGREDIENT_SLOTS = {0, 2, 4, 18, 22, 36, 38, 40};
    private static final int INPUT_SLOT = 20;
    private static final int OUTPUT_SLOT = 24;
    private static final int BACK_SLOT = 45;
    private static final int RECIPE_ID_SETTER_SLOT = 48;
    private static final int SAVE_SLOT = 49;
    private static final int DELETE_SLOT = 53;

    private final Player player;
    private final boolean isPreview;

    private String recipeId;
    private boolean refundItem = true;

    public FusionRecipeEditorGui(Player player) {
        this(player, false, "Fusion Recipe");
    }

    private FusionRecipeEditorGui(Player player, boolean isPreview, String title) {
        super(Component.text(title, NamedTextColor.DARK_GRAY), 6);
        this.player = player;
        this.isPreview = isPreview;
    }

    /** Opens this editor pre-filled from an existing recipe (id, input/ingredient/output contents) - used by the recipe viewer's right-click-to-edit. */
    public static FusionRecipeEditorGui editExisting(Player player, FusionRecipe recipe) {
        FusionRecipeEditorGui gui = new FusionRecipeEditorGui(player, false, "Fusion Recipe");
        gui.prefill(recipe, false);
        return gui;
    }

    /** Opens a locked, read-only view of an existing recipe - used by the recipe viewer's left-click-to-inspect. */
    public static FusionRecipeEditorGui preview(Player player, FusionRecipe recipe) {
        FusionRecipeEditorGui gui = new FusionRecipeEditorGui(player, true, "Công thức: " + recipe.getId());
        gui.refundItem = false;
        gui.prefill(recipe, true);
        return gui;
    }

    private void prefill(FusionRecipe recipe, boolean withLabel) {
        this.recipeId = recipe.getId();
        Inventory inv = getInventory();

        ItemStack input = resolveStack(recipe.getInput(), withLabel);
        if (input != null) inv.setItem(INPUT_SLOT, input);

        List<CraftingIngredient> ingredients = recipe.getIngredients();
        for (int i = 0; i < ingredients.size() && i < INGREDIENT_SLOTS.length; i++) {
            ItemStack stack = resolveStack(ingredients.get(i), withLabel);
            if (stack != null) inv.setItem(INGREDIENT_SLOTS[i], stack);
        }

        ItemStack resultStack = resolveStack(recipe.getResult(), withLabel);
        if (resultStack != null) inv.setItem(OUTPUT_SLOT, resultStack);
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
            addButton(RECIPE_ID_SETTER_SLOT, infoItem(), ClickHandler.noAction());
            lockContentSlots();
            return;
        }

        addButton(RECIPE_ID_SETTER_SLOT, recipeIdButton(), event -> {
            event.setCancelled(true);
            openRecipeIdDialog();
        });

        addButton(SAVE_SLOT, saveButton(), event -> {
            event.setCancelled(true);
            attemptSave(this::buildFusionRecipe);
        });

        ItemStack delItem = ItemStack.of(Material.LAVA_BUCKET);
        delItem.setData(DataComponentTypes.ITEM_NAME, Component.text("DELETE?", NamedTextColor.RED));
        addButton(DELETE_SLOT, delItem, event -> {
            event.setCancelled(true);
            openDeleteDialog((Player) event.getWhoClicked());
        });
    }

    /** Binds a locked ({@link ClickHandler#noAction()}) handler over whatever's already sitting in each ingredient/input/output slot - only meaningful in preview mode. */
    private void lockContentSlots() {
        Inventory inv = getInventory();
        lockSlot(inv, INPUT_SLOT);
        lockSlot(inv, OUTPUT_SLOT);
        for (int slot : INGREDIENT_SLOTS) lockSlot(inv, slot);
    }

    private void lockSlot(Inventory inv, int slot) {
        ItemStack item = inv.getItem(slot);
        if (item != null) addButton(slot, item, ClickHandler.noAction());
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

    /** Fills every slot except the ingredients, input, output, and the buttons - never the reverse, so their contents are never disturbed by a redraw. */
    private void fillBackground() {
        Inventory inv = getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            if (isReservedSlot(i)) continue;
            addButton(i, FILLER_BLACK, ClickHandler.noAction());
        }
    }

    private boolean isReservedSlot(int slot) {
        for (int ingredientSlot : INGREDIENT_SLOTS) {
            if (ingredientSlot == slot) return true;
        }
        return slot == INPUT_SLOT || slot == OUTPUT_SLOT || slot == RECIPE_ID_SETTER_SLOT || slot == SAVE_SLOT;
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
        item.setData(DataComponentTypes.LORE, ItemLore.lore(List.of(Utils.text("Loại: Fusion", NamedTextColor.GRAY))));
        return item;
    }

    private ItemStack saveButton() {
        ItemStack item = ItemStack.of(Material.LIME_CONCRETE);
        item.setData(DataComponentTypes.ITEM_NAME, Component.text("Lưu công thức", NamedTextColor.GOLD));
        item.setData(DataComponentTypes.LORE, ItemLore.lore(List.of(Utils.text("Slot giữa (20) là input, các ô xung quanh là nguyên liệu.", NamedTextColor.GRAY))));
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
            player.sendMessage(Utils.text("Công thức không hợp lệ - cần đặt input, kết quả, và ít nhất 1 nguyên liệu.", NamedTextColor.RED));
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

    private @Nullable CraftingRecipe buildFusionRecipe(String id) {
        Inventory inv = getInventory();

        ItemStack outputStack = inv.getItem(OUTPUT_SLOT);
        String outputKey = CraftingIngredient.resolveKey(outputStack);
        if (outputKey == null) return null;

        ItemStack inputStack = inv.getItem(INPUT_SLOT);
        String inputKey = CraftingIngredient.resolveKey(inputStack);
        if (inputKey == null) return null;

        List<CraftingIngredient> ingredients = new ArrayList<>();
        for (int slot : INGREDIENT_SLOTS) {
            ItemStack stack = inv.getItem(slot);
            String key = CraftingIngredient.resolveKey(stack);
            if (key == null) continue;
            ingredients.add(new CraftingIngredient(key, stack.getAmount()));
        }
        if (ingredients.isEmpty()) return null;

        CraftingIngredient input = new CraftingIngredient(inputKey, inputStack.getAmount());
        CraftingIngredient result = new CraftingIngredient(outputKey, outputStack.getAmount());
        return new FusionRecipe(new CraftingRecipe.BaseProperties(id, result), input, ingredients);
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
        for (int slot : INGREDIENT_SLOTS) {
            ItemStack item = inv.getItem(slot);
            if (item != null && !item.getType().isAir()) PlayerUtils.giveItem(player, item);
        }
        ItemStack input = inv.getItem(INPUT_SLOT);
        if (input != null && !input.getType().isAir()) PlayerUtils.giveItem(player, input);
        ItemStack output = inv.getItem(OUTPUT_SLOT);
        if (output != null && !output.getType().isAir()) PlayerUtils.giveItem(player, output);
    }

    @Override
    public void onClickBottomInventory(InventoryClickEvent event) {
        if (isPreview) event.setCancelled(true);
    }
}
