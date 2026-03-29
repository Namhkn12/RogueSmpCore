package com.roguesmp.block.impl;

import com.roguesmp.block.SmpBlock;
import com.roguesmp.constant.ComponentKeys;
import com.roguesmp.constant.TransferMode;
import com.roguesmp.gui.BaseGui;
import com.roguesmp.item.BaseItem;
import com.roguesmp.item.component.impl.NameComponent;
import com.roguesmp.recipe.BaseRecipe;
import com.roguesmp.recipe.impl.MachineRecipe;
import io.papermc.paper.datacomponent.DataComponentTypes;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public abstract class SmpMachine extends SmpBlock {

    protected final BaseGui gui;
    private int progress = 0;
    protected boolean isProgressing = false;
    protected Material progressDisplay;
    private BaseRecipe currentRecipe = null;
    private final Map<BlockFace, TransferMode>  sideConfigs = new HashMap<>();
    public static final BlockFace[] FACES = {
            BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST
    };

    public SmpMachine(BaseItem baseItem){
        super(baseItem);
        this.gui = createGui();

        for(BlockFace face: FACES){
            sideConfigs.put(face, TransferMode.NONE);
        }
    }

    public SmpMachine(BaseItem baseItem, Material progressDisplay) {
        this(baseItem);

        ItemStack item = ItemStack.of(progressDisplay);
        if(item.hasData(DataComponentTypes.MAX_DAMAGE) && !item.hasData(DataComponentTypes.UNBREAKABLE)){
            this.progressDisplay = progressDisplay;
        }
        else{
            throw new IllegalArgumentException("Progress display item must be a damageable one");
        }

    }

    protected abstract BaseGui createGui();

    public abstract void registerRecipes();

    public abstract void setPercent(int percent);

    public BaseGui getGui() {return this.gui;}

    public void setProgress(int progress) {this.progress = progress;}

    public int getProgress() {return this.progress;}

    public boolean isProgressing() {return isProgressing;}

    public void setProgressing(boolean progressing) {isProgressing = progressing;}

    public void setCurrentRecipe(BaseRecipe recipe) {this.currentRecipe = recipe;}

    public BaseRecipe getCurrentRecipe() {return this.currentRecipe;}

    public TransferMode getTransferMode(BlockFace face){
        return sideConfigs.getOrDefault(face, TransferMode.NONE);
    }

    public void setTransferMode(BlockFace face, TransferMode mode){
        sideConfigs.put(face, mode);
    }

    public Map<BlockFace, TransferMode> getSideConfigs() {
        return sideConfigs;
    }

    protected NameComponent getMachineName() {return getItem().getComponent(ComponentKeys.ITEM_NAME);}

    @Override
    public void onBlockInteract(PlayerInteractEvent event) {
        boolean isSneaking = event.getPlayer().isSneaking();
        Action action = event.getAction();

        if(action.isRightClick() && !isSneaking){
            event.setCancelled(true);
            event.getPlayer().openInventory(gui.getInventory());
        }
    }

}
