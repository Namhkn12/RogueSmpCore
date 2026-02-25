package com.roguesmp.gui;

import com.roguesmp.block.impl.SmpMachine;
import com.roguesmp.utils.ItemStackUtils;
import com.roguesmp.utils.Utils;
import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;

public abstract class MachineGui extends BaseGui {

    private final ItemStack INPUT_FILLER = ItemStackUtils.hideTooltip(ItemStack.of(Material.BLUE_STAINED_GLASS_PANE));
    private final ItemStack OUTPUT_FILLER = ItemStackUtils.hideTooltip(ItemStack.of(Material.ORANGE_STAINED_GLASS_PANE));
    private final ItemStack PROCESSING_FILLER = ItemStackUtils.hideTooltip(ItemStack.of(Material.GRAY_STAINED_GLASS_PANE));
    private final ItemStack SETTING_FILLER = ItemStackUtils.hideTooltip(ItemStack.of(Material.GREEN_STAINED_GLASS_PANE));
//    private final ItemStack upgrade = ItemStack.of(Material.ITEM_FRAME);
    private final ItemStack SETTINGS = ItemStack.of(Material.NETHER_STAR);
    private final int row;
    private final String name;
    private final int settingSlot = 8;
    protected SmpMachine machine;

    /**
     * Instantiate the machine GUI
     * @param name: The name of the machine to show in the left
     * @param row: The number of rows that the GUI have except the setting row,
     *           the first row is automatically the settings row,
     *           the rest belongs to the machine itself
     */
    public MachineGui(SmpMachine machine, String name, int row) {
        super(Utils.fromString(name), row+1);
        this.row = row + 1;
        this.name = name;
        this.machine = machine;

        setup();
    }

    @Override
    public void setup() {
        fillMachineSides();
        itemDecoration();
        for(int i: getInputSlots()){
            if(i > 9*row)
                throw new IllegalArgumentException("Input slots for this " + name + " machine is outside gui");
            else{
                this.addButton(i, null, event -> {});
            }
        }
        for(int i: getOutputSlots()){
            if(i > 9*row)
                throw new IllegalArgumentException("Output slots for this " + name + " machine is outside gui");
            else{
                this.addButton(i, null, event -> {});
            }
        }
        this.addButton(8, SETTINGS, ClickHandler.openGui(new MachineTransferGui(machine, this)));
    }

    private void fillMachineSides(){
        for(int i=0; i<9; i++){
            this.addButton(i, SETTING_FILLER, ClickHandler.noAction());
        }
        for(int i=1; i < row; i++){
            int thisRowProcessSlot = i * 9 + 4;
            for(int j=0; j<9; j++){
                int thisSlot = i * 9 + j;
                if(thisSlot < thisRowProcessSlot) this.addButton(thisSlot, INPUT_FILLER, ClickHandler.noAction());
                else if(thisSlot > thisRowProcessSlot) this.addButton(thisSlot, OUTPUT_FILLER, ClickHandler.noAction());
                else this.addButton(thisSlot, PROCESSING_FILLER, ClickHandler.noAction());
            }
        }
    }

    private void itemDecoration(){
        ItemStackUtils.setItemName(SETTINGS, Component.text("Cài đặt", NamedTextColor.GREEN));
    }

    /**
     * Input slots for the machine
     * Need to minus 9 for each slot
     */
    public abstract int[] getInputSlots();
    /**
     * Output slots for the machine
     * Need to minus 9 for each slot
     */
    public abstract int[] getOutputSlots();
    /**
     * The processing itemStack inside the machine
     * Need to minus 9
     */
    public abstract int getProcessingSlot();

    public void setProcessing(ItemStack processing){
        this.addButton(getProcessingSlot(), processing, ClickHandler.noAction());
    }

    public void setProcessingDefault(){
        this.addButton(getProcessingSlot(), PROCESSING_FILLER, ClickHandler.noAction());
    }

}
