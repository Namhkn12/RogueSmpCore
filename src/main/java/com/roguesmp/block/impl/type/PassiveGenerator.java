package com.roguesmp.block.impl.type;

import com.roguesmp.gui.BaseGui;
import com.roguesmp.gui.PassiveGeneratorGui;
import com.roguesmp.item.BaseItem;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.block.BlockBreakEvent;

public abstract class PassiveGenerator extends Generator {

    public PassiveGenerator(BaseItem baseItem) {
        super(baseItem);
    }

    @Override
    protected BaseGui createGui() {
        return new PassiveGeneratorGui(getMachineName().value(), 3);
    }

    @Override
    public void registerRecipes() {}

    public abstract int getGenerationRate();

    protected void isRunning(PassiveGeneratorGui gui){
        gui.setDisplaySlot(true);
        setProgressing(true);
    }

    protected void isNotRunning(PassiveGeneratorGui gui){
        gui.setDisplaySlot(false);
        setProgressing(false);
    }
}
