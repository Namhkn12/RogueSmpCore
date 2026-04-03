package com.roguesmp.gui;

import com.roguesmp.recipe.BaseRecipe;
import com.roguesmp.recipe.impl.MachineRecipe;
import com.roguesmp.utils.ItemStackUtils;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class RecipeViewerGui extends BaseGui {

    private final ItemStack SETTING_FILLER = ItemStackUtils.hideTooltip(ItemStack.of(Material.GREEN_STAINED_GLASS_PANE));
    private final ItemStack PROCESSING_FILLER = ItemStackUtils.hideTooltip(ItemStack.of(Material.GRAY_STAINED_GLASS_PANE));
    private final ItemStack BACK_BTN = ItemStack.of(Material.ARROW);

    private BaseGui machineGui;
    private int row;
    private BaseGui previousGui;
    private BaseRecipe recipe;

    private RecipeViewerGui(String name, int row){
        super(Utils.fromString(name), row);
    }

    public RecipeViewerGui(BaseGui previousGui, BaseGui imitateGui, BaseRecipe recipe, ItemStack mainItem){
        this("Xem công thức của " + PlainTextComponentSerializer.plainText().serialize(mainItem.effectiveName()),imitateGui.getInventory().getSize() / 9);

        this.row = imitateGui.getInventory().getSize() / 9;
        this.machineGui = imitateGui;
        this.previousGui = previousGui;
        this.recipe = recipe;

        itemDecoration();
        setup();
    }

    @Override
    public void setup() {
        for(int i=0; i<row; i++){
            for(int j=0; j<9; j++){
                int cell = i * 9 + j;

                switch (cell){
                    case 0 -> addButton(cell, BACK_BTN, event -> {
                        previousGui.showInventory(event.getWhoClicked());
                    });
                    default -> {
                        if(cell < 9) {
                            addButton(cell, SETTING_FILLER, ClickHandler.noAction());
                        }
                        else if (cell == 13){
                            addButton(cell, PROCESSING_FILLER, ClickHandler.noAction());
                        }
                        else {
                            addButton(cell, machineGui.getInventory().getItem(cell), ClickHandler.noAction());
                        }
                    }
                }
            }
        }

        if(machineGui instanceof MachineGui gui && recipe instanceof MachineRecipe mRecipe){
            int[] inputSlots = gui.getInputSlots();
            int[] outputSlots = gui.getOutputSlots();
            int processingSlot = gui.getProcessingSlot();

            for(int k=0; k<inputSlots.length; k++){
                ItemStack input = null;
                try{
                    input = recipe.getInputs().get(k);
                }
                catch (Exception ignored){}
                addButton(inputSlots[k], input, ClickHandler.noAction());
            }
            for(int k=0; k<outputSlots.length; k++){
                ItemStack output = null;
                try{
                    output = recipe.getOutputs().get(k);
                }
                catch (Exception ignored){}
                addButton(outputSlots[k], output, ClickHandler.noAction());
            }

            ItemStack time = ItemStack.of(Material.CLOCK);
            ItemStackUtils.setItemName(time, Component.text("Thời gian xử lý: ")
                    .decoration(TextDecoration.ITALIC, false)
                    .color(NamedTextColor.WHITE)
                    .append(Component.text(mRecipe.getBaseProcessTime() + "s")
                            .color(NamedTextColor.YELLOW))
            );

            addButton(processingSlot, time, ClickHandler.noAction());
        }
    }

    private void itemDecoration(){
        ItemStackUtils.setItemName(BACK_BTN, Component.text("Quay lại", NamedTextColor.GREEN));
    }
}
