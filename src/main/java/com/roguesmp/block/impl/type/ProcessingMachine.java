package com.roguesmp.block.impl.type;

import com.roguesmp.block.impl.SmpMachine;
import com.roguesmp.constant.ComponentKeys;
import com.roguesmp.gui.MachineGui;
import com.roguesmp.item.BaseItem;
import com.roguesmp.item.component.impl.NameComponent;
import org.bukkit.Material;

public abstract class ProcessingMachine extends SmpMachine {

    private final int GUI_ROWS = 3;

    public ProcessingMachine(BaseItem baseItem, Material progressDisplay) {
        super(baseItem, progressDisplay);
    }

    private NameComponent getMachineName(){
        return getItem().getComponent(ComponentKeys.ITEM_NAME);
    }
    public abstract void registerRecipes();

    @Override
    protected MachineGui createGui() {
        return new MachineGui(this, getMachineName().value(), GUI_ROWS) {
            @Override
            public int[] getInputSlots() {
                return new int[]{19, 20};
            }

            @Override
            public int[] getOutputSlots() {
                return new int[]{24, 25};
            }

            @Override
            public int getProcessingSlot() {
                return 22;
            }

        };
    }
}
