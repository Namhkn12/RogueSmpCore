package com.roguesmp.block.manager;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.block.SmpBlock;
import com.roguesmp.block.impl.SmpMachine;
import com.roguesmp.constant.RecipeType;
import com.roguesmp.gui.MachineGui;
import com.roguesmp.recipe.BaseRecipe;
import com.roguesmp.recipe.impl.MachineRecipe;
import com.roguesmp.recipe.manager.RecipeManager;
import com.roguesmp.registry.BlockRegistry;
import com.roguesmp.utils.MachineTransferUtils;
import com.roguesmp.utils.RecipeUtils;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class BlockManager {
    private static BlockManager INSTANCE = null;
    private final Map<Location, String> blockIdLocation = new HashMap<>();
    private final Map<Location, SmpBlock> blockLocation = new HashMap<>();
    private final static Map<String, Supplier<SmpBlock>> blockConstructors = new HashMap<>();
    private Plugin plugin;
    private final int PERIOD = 20;

    public BlockManager(RogueSmpCore plugin){
        this.plugin = plugin;

        //Run every one sec to check if block/machine have valid recipe or to run the recipe
        new BukkitRunnable(){

            @Override
            public void run() {
                blockLocation.forEach((loc, block) -> {
                    if(block instanceof SmpMachine machine){
                        MachineGui gui = machine.getGui();

                        if (machine.isProgressing() && machine.getCurrentRecipe() == null) {
                            String machineId = block.getItem().getId();
                            BaseRecipe foundRecipe = RecipeManager.findRecipe(machineId, gui.getInventory(), gui.getInputSlots());

                            if (foundRecipe instanceof MachineRecipe) {
                                machine.setCurrentRecipe((MachineRecipe) foundRecipe);
                            } else {
                                machine.setProgressing(false);
                                machine.setProgress(0);
                                return;
                            }
                        }

                        if (!machine.isProgressing()) {
                            String machineId = block.getItem().getId();
                            BaseRecipe foundRecipe = RecipeManager.findRecipe(machineId, gui.getInventory(), gui.getInputSlots());

                            if (foundRecipe != null && foundRecipe.getType() == RecipeType.PROCESSING) {
                                MachineRecipe machineRecipe = (MachineRecipe) foundRecipe;
                                boolean haveEnoughOutputSlots = RecipeUtils.canFitInSlots(gui.getInventory(), gui.getOutputSlots(), machineRecipe.getOutputs());

                                if (haveEnoughOutputSlots) {
                                    RecipeUtils.consumeInputs(gui.getInventory(), gui.getInputSlots(), machineRecipe.getInputs());
                                    machine.setCurrentRecipe(machineRecipe);
                                    machine.setProgressing(true);
                                    machine.setProgress(0);
                                    machine.setPercent(0);
                                }
                            }
                        }

                        else {
                            MachineRecipe recipe = machine.getCurrentRecipe();

                            if (recipe == null) {
                                machine.setProgressing(false);
                                return;
                            }

                            int currentProgress = machine.getProgress() + 1;
                            int maxProgressTime = recipe.getBaseProcessTime();

                            machine.setProgress(currentProgress);

                            int percent = (currentProgress * 100) / maxProgressTime;
                            machine.setPercent(percent);

                            if (currentProgress >= maxProgressTime) {
                                if (RecipeUtils.canFitInSlots(gui.getInventory(), gui.getOutputSlots(), recipe.getOutputs())) {
                                    RecipeUtils.addToOutputSlot(gui.getInventory(), gui.getOutputSlots(), recipe.getOutputs());

                                    machine.setProgressing(false);
                                    machine.setProgress(0);
                                    machine.setCurrentRecipe(null);
                                    machine.setPercent(0);
                                } else {
                                    machine.setProgress(currentProgress - 1);
                                }
                            }
                        }

                        MachineTransferUtils.processMachineTransfers(machine, loc, BlockManager.this);
                    }
                });
            }

        }.runTaskTimer(this.plugin, 0, PERIOD);
    }

    public static void registerBlockType(String id, Supplier<SmpBlock> constructor){
        blockConstructors.put(id, constructor);
    }

    public void registerBlock(Location loc, String id){
        Supplier<SmpBlock> constructor = blockConstructors.get(id);
        SmpBlock block = null;

        if(constructor!=null){
            block = constructor.get();
        }
        else{
            block = BlockRegistry.getBlock(id);
        }

        blockIdLocation.put(loc, id);
        blockLocation.put(loc, block);
    }

    public void removeBlock(Location loc){
        blockIdLocation.remove(loc);
        blockLocation.remove(loc);
    }

    public static BlockManager getInstance() {
        if (INSTANCE == null) {
            throw new RuntimeException(BlockManager.class.getSimpleName() + "is null when getInstance() is called.");
        }
        return INSTANCE;
    }

    public String getBlockId(Location loc){
        return blockIdLocation.get(loc);
    }

    public boolean isSmpBlock(Location loc){
        return blockLocation.containsKey(loc);
    }

    public SmpBlock getBlock(Location loc){
        return blockLocation.get(loc);
    }

    public SmpBlock getBlock(String id){
        return BlockRegistry.getInstance().getRegistry().get(id);
    }

    public Map<Location, SmpBlock> getAllBlocks() {return blockLocation;}

    public Map<Location, String> getAllBlockIds() {return blockIdLocation;}

    public static void init(RogueSmpCore core) {INSTANCE = new BlockManager(core);}

}
