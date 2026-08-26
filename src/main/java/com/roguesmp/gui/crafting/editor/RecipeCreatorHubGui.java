package com.roguesmp.gui.crafting.editor;

import com.roguesmp.gui.BaseGui;
import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class RecipeCreatorHubGui extends BaseGui {

    private enum RecipeType {
        SHAPED_SHAPELESS, FUSION
    }

    private final Player player;

    public RecipeCreatorHubGui(Player player) {
        super(Component.text("Pick Recipe Type"), 3);
        this.player = player;
    }

    @Override
    public void setup() {
        fillEmpty(FILLER_BLACK);

        ItemStack shaped = ItemStack.of(Material.CRAFTING_TABLE);
        shaped.setData(DataComponentTypes.ITEM_NAME, Component.text("Shaped & Shapeless", NamedTextColor.GOLD));
        addButton(1, 3, shaped,event -> openEditorByType(RecipeType.SHAPED_SHAPELESS));

//        ItemStack shapeless = ItemStack.of(Material.CRAFTING_TABLE);
//        shapeless.setData(DataComponentTypes.ITEM_NAME, Component.text("Shapeless", NamedTextColor.GOLD));
//        addButton(1, 4, shapeless,event -> openEditorByType(RecipeType.SHAPELESS));

        ItemStack fusion = ItemStack.of(Material.ENDER_EYE);
        fusion.setData(DataComponentTypes.ITEM_NAME, Component.text("Fusion", NamedTextColor.GOLD));
        addButton(1, 5,fusion,event -> openEditorByType(RecipeType.FUSION));
    }

    private void openEditorByType(RecipeType type) {
        switch (type) {
            case SHAPED_SHAPELESS -> new ShapedShapelessRecipeEditorGui(player).showInventory(player);
            case FUSION -> new FusionRecipeEditorGui(player).showInventory(player);
        }
    }
}
