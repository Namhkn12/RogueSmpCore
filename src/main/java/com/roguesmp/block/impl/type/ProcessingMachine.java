package com.roguesmp.block.impl.type;

import com.roguesmp.block.impl.SmpMachine;
import com.roguesmp.block.impl.interfaces.IHaveLockedRecipe;
import com.roguesmp.constant.ComponentKeys;
import com.roguesmp.gui.MachineGui;
import com.roguesmp.gui.interfaces.IHaveBlueprint;
import com.roguesmp.item.BaseItem;
import com.roguesmp.item.component.impl.NameComponent;
import com.roguesmp.recipe.BaseRecipe;
import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public abstract class ProcessingMachine extends SmpMachine implements IHaveLockedRecipe {

    private final int GUI_ROWS = 3;
    private BaseRecipe lockedRecipe = null;

    public ProcessingMachine(BaseItem baseItem, Material progressDisplay) {
        super(baseItem, progressDisplay);
    }

    @Override
    public void setLockedRecipe(@Nullable  BaseRecipe lockedRecipe) {
        this.lockedRecipe = lockedRecipe;
        if(gui instanceof IHaveBlueprint gBlueprint){
            gBlueprint.setBlueprintItem(lockedRecipe);
        }
    }

    @Override
    public @Nullable BaseRecipe getLockedRecipe() {
        return lockedRecipe;
    }

    public abstract void registerRecipes();
    public abstract int getEnergyPerSec();

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

    public void setPercent(int percent) {
        MachineGui machineGui = (MachineGui) gui;
        if(!isProgressing){
            machineGui.setProcessingDefault();
        }
        else{
            ItemStack processing = ItemStack.of(progressDisplay);
            int maxDamage = processing.getData(DataComponentTypes.MAX_DAMAGE);
            int currentDamage = maxDamage - (maxDamage * percent / 100);

            // (Tùy chọn) Chốt chặn an toàn để tránh bị lỗi hiển thị nếu percent tính sai
            currentDamage = Math.max(0, Math.min(currentDamage, maxDamage));
            processing.setData(DataComponentTypes.DAMAGE, currentDamage);
            processing.setData(DataComponentTypes.ITEM_NAME, Component.text(percent + "%").color(NamedTextColor.GREEN));

            machineGui.setProcessing(processing);
        }
    }

    @Override
    public void onBlockBreak(BlockBreakEvent event) {
        super.onBlockBreak(event);

        MachineGui machineGui = (MachineGui) gui;

        int[] inputSlots = machineGui.getInputSlots();
        int[] outputSlots = machineGui.getOutputSlots();
        Inventory inv = gui.getInventory();
        Location loc = event.getBlock().getLocation();

        List<ItemStack> itemsToDrop = new ArrayList<>();
        for(int i: inputSlots){
            ItemStack item = inv.getItem(i);
            if(item != null) itemsToDrop.add(item);
        }
        for(int i: outputSlots){
            ItemStack item = inv.getItem(i);
            if(item != null) itemsToDrop.add(item);
        }

        itemsToDrop.forEach(item -> {
            loc.getWorld().dropItemNaturally(loc, item);
        });
    }
}
