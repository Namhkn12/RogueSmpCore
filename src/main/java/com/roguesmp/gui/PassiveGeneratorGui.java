package com.roguesmp.gui;

import com.roguesmp.utils.ItemStackUtils;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class PassiveGeneratorGui extends BaseGui {

    private final ItemStack FILLER = ItemStackUtils.hideTooltip(ItemStack.of(Material.BLUE_STAINED_GLASS_PANE));
    private final ItemStack WORKING = ItemStack.of(Material.GREEN_STAINED_GLASS_PANE);
    private final ItemStack NOT_WORKING = ItemStack.of(Material.RED_STAINED_GLASS_PANE);

    private final int row;
    private final int DISPLAY_SLOT = 13;

    public PassiveGeneratorGui(String name, int row) {
        super(Utils.fromString(name), row);
        this.row = row;

        setup();
    }

    @Override
    public void setup() {
        decorateItem();
        for(int i=0; i<row; i++){
            for(int j=0; j<9; j++){
                this.addButton(i * 9 + j, FILLER, ClickHandler.noAction());
            }
        }

        this.addButton(DISPLAY_SLOT, NOT_WORKING, ClickHandler.noAction());
    }

    private void decorateItem() {
        ItemStackUtils.setItemName(WORKING, Component.text("Đang hoạt động", NamedTextColor.YELLOW));
        ItemStackUtils.setItemName(NOT_WORKING, Component.text("Dừng hoạt động", NamedTextColor.YELLOW));
    }

    public void setDisplaySlot(boolean isRunning) {
        if(isRunning){
            this.addButton(DISPLAY_SLOT, WORKING, ClickHandler.noAction());
        }
        else{
            this.addButton(DISPLAY_SLOT, NOT_WORKING, ClickHandler.noAction());
        }
    }

}
