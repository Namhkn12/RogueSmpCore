package com.roguesmp.crafting.gui;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.crafting.CraftingManager;
import com.roguesmp.crafting.recipe.CraftingRecipe;
import com.roguesmp.crafting.recipe.CraftingRecipes;
import com.roguesmp.crafting.recipe.RecipeKey;
import com.roguesmp.crafting.recipe.ShapedCraftingRecipe;
import com.roguesmp.crafting.recipe.ShapelessCraftingRecipe;
import com.roguesmp.gui.crafting.editor.ShapedShapelessRecipeEditorGui;
import com.roguesmp.registry.Registries;
import com.roguesmp.utils.Utils;
import com.roguesmp.utils.dialog.DialogBuilder;
import com.roguesmp.utils.dialog.DialogTypeBuilder;
import com.roguesmp.utils.dialog.JsonEntryDialog;
import com.roguesmp.utils.dialog.LenientJson;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class RecipeDialogEditor {

    private static final String NEW_RECIPE_TEMPLATE = "type: , id: , results: []";

    private RecipeDialogEditor() {
    }

    public static boolean usesGridEditor(CraftingRecipe recipe) {
        return recipe instanceof ShapedCraftingRecipe || recipe instanceof ShapelessCraftingRecipe;
    }

    public static void openExisting(Player player, CraftingRecipe recipe, Runnable onExit) {
        player.closeInventory();
        player.showDialog(entryDialog(player, recipe.getId(), onExit).build(player, recipe));
    }

    public static void openTypePicker(Player player, Runnable onExit) {
        DialogTypeBuilder.MultiAction multi = DialogBuilder.create(Component.text("Tạo công thức"))
                .canCloseWithEscape(false)
                .multiAction();

        multi.addButton(Component.text(ShapedCraftingRecipe.TYPE_NAME + " / " + ShapelessCraftingRecipe.TYPE_NAME, NamedTextColor.GOLD), Component.text("Trình chỉnh sửa lưới 3x3", NamedTextColor.GRAY),
                (response, audience) -> Utils.runLater(() -> new ShapedShapelessRecipeEditorGui(player).showInventory(player)));
        multi.addButton(Component.text("Loại khác (JSON)", NamedTextColor.AQUA), null,
                (response, audience) -> Utils.runLater(() -> player.showDialog(entryDialog(player, null, onExit).buildWithText(player, NEW_RECIPE_TEMPLATE))));
        multi.columns(2);
        multi.exitButton(Component.text("Huỷ"), (response, audience) -> Utils.runLater(onExit));

        player.closeInventory();
        player.showDialog(multi.build());
    }

    private static JsonEntryDialog<CraftingRecipe> entryDialog(Player player, @Nullable String originalId, Runnable onExit) {
        return JsonEntryDialog.of(originalId == null ? "Công thức mới" : "Công thức: " + originalId, CraftingRecipe.CODEC,
                recipe -> {
                    Registries.CRAFTING_RECIPE.registerAndSave(RogueSmpCore.getInstance(), recipe.getId(), recipe);
                    if (originalId != null && !originalId.equals(recipe.getId())) {
                        Registries.CRAFTING_RECIPE.removeAndDeleteFiles(RogueSmpCore.getInstance(), originalId);
                    }
                    CraftingManager.getInstance().rebuild();
                    player.sendMessage(Utils.text("Đã lưu công thức '" + recipe.getId() + "'.", NamedTextColor.GREEN));
                    onExit.run();
                },
                originalId == null ? null : () -> {
                    Registries.CRAFTING_RECIPE.removeAndDeleteFiles(RogueSmpCore.getInstance(), originalId);
                    CraftingManager.getInstance().rebuild();
                    player.sendMessage(Utils.text("Đã xoá công thức '" + originalId + "'.", NamedTextColor.RED));
                    onExit.run();
                },
                onExit)
                .withHelp(help())
                .withValidator(recipe -> validate(recipe, originalId));
    }

    private static @Nullable String validate(CraftingRecipe recipe, @Nullable String originalId) {
        String id = recipe.getId();
        if (id == null || id.isBlank()) return "Cần đặt id công thức.";
        if (!id.equals(originalId) && Registries.CRAFTING_RECIPE.get(id) != null) return "Đã tồn tại công thức với id '" + id + "'.";
        return null;
    }

    private static List<Component> help() {
        List<String> types = new ArrayList<>();
        for (RecipeKey<?> key : CraftingRecipes.all()) {
            if (key != CraftingRecipes.SHAPED && key != CraftingRecipes.SHAPELESS) types.add(key.id());
        }
        return List.of(LenientJson.typesHelp(types));
    }
}
