package com.roguesmp.block.impl;

import com.roguesmp.block.SmpBlock;
import com.roguesmp.gui.MachineGui;
import com.roguesmp.item.BaseItem;
import com.roguesmp.recipe.BaseRecipe;
import com.roguesmp.recipe.impl.MachineRecipe;
import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

import java.util.List;

public abstract class SmpMachine extends SmpBlock {

    private final MachineGui gui;
    private int progress = 0;
    private boolean isProgressing = false;
    private final Material progressDisplay;
    private MachineRecipe currentRecipe = null;

    public SmpMachine(BaseItem baseItem, Material progressDisplay) {
        super(baseItem);
        this.gui = createGui();
        ItemStack item = ItemStack.of(progressDisplay);
        if(item.hasData(DataComponentTypes.MAX_DAMAGE) && !item.hasData(DataComponentTypes.UNBREAKABLE)){
            this.progressDisplay = progressDisplay;
        }
        else{
            throw new IllegalArgumentException("Progress display item must be a damageable one");
        }
    }

    protected abstract MachineGui createGui();
    public abstract void registerRecipes();
    public MachineGui getGui() {return this.gui;}
    public void setProgress(int progress) {this.progress = progress;}
    public int getProgress() {return this.progress;}
    public boolean isProgressing() {return isProgressing;}
    public void setProgressing(boolean progressing) {isProgressing = progressing;}
    public void setCurrentRecipe(MachineRecipe recipe) {this.currentRecipe = recipe;}
    public MachineRecipe getCurrentRecipe() {return this.currentRecipe;}
    public void setPercent(int percent) {
        if(!isProgressing){
            gui.setProcessingDefault();
        }
        else{
            ItemStack processing = ItemStack.of(progressDisplay);
            int maxDamage = processing.getData(DataComponentTypes.MAX_DAMAGE);
            int currentDamage = maxDamage - (maxDamage * percent / 100);

            // (Tùy chọn) Chốt chặn an toàn để tránh bị lỗi hiển thị nếu percent tính sai
            currentDamage = Math.max(0, Math.min(currentDamage, maxDamage));
            processing.setData(DataComponentTypes.DAMAGE, currentDamage);
            processing.setData(DataComponentTypes.ITEM_NAME, Component.text(percent + "%").color(NamedTextColor.GREEN));

            gui.setProcessing(processing);
        }
    }

    @Override
    public void onBlockInteract(PlayerInteractEvent event) {
        boolean isSneaking = event.getPlayer().isSneaking();
        ItemStack getItemInHand = event.getItem();
        Action action = event.getAction();

        if(action.isRightClick() && !isSneaking){
            event.getPlayer().openInventory(gui.getInventory());
        }
    }

}
