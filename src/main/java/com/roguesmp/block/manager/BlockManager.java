package com.roguesmp.block.manager;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.block.IEnergyStorage;
import com.roguesmp.block.SmpBlock;
import com.roguesmp.block.impl.SmpMachine;
import com.roguesmp.block.impl.blocks.EnergyNode;
import com.roguesmp.block.impl.type.Generator;
import com.roguesmp.block.impl.type.PassiveGenerator;
import com.roguesmp.block.impl.type.ProcessingMachine;
import com.roguesmp.constant.RecipeType;
import com.roguesmp.gui.BaseGui;
import com.roguesmp.gui.MachineGui;
import com.roguesmp.recipe.BaseRecipe;
import com.roguesmp.recipe.impl.MachineRecipe;
import com.roguesmp.recipe.manager.RecipeManager;
import com.roguesmp.registry.BlockRegistry;
import com.roguesmp.utils.MachineTransferUtils;
import com.roguesmp.utils.RecipeUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
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
                    if(block instanceof Generator gen){
                        gen.pushEnergy(loc);
                    }
                    if(block instanceof EnergyNode node){
                        transferToOtherEnergyNode(node);
                        transferToElectricMachine(node, loc);
                    }
                    if(block instanceof ProcessingMachine machine){
                        MachineGui gui = (MachineGui) machine.getGui();

                        findAndSetCurrentRecipe(block, machine, gui);

                        if (!machine.isProgressing()) {processingIfCan(block, machine, gui);}
                        else {addProgressOrStopMachine(machine, gui);}

                        //Auto push/pull items into other inventory or machine
                        MachineTransferUtils.processMachineTransfers(machine, loc, BlockManager.this);
                    }
                    if(block instanceof PassiveGenerator gen){
                        gen.generate(loc);
                    }
                });
            }

        }.runTaskTimer(this.plugin, 0, PERIOD);
    }

    private void findAndSetCurrentRecipe(SmpBlock block, ProcessingMachine machine, MachineGui gui){
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
    }

    private void processingIfCan(SmpBlock block, ProcessingMachine machine, MachineGui gui){
        String machineId = block.getItem().getId();
        BaseRecipe foundRecipe = RecipeManager.findRecipe(machineId, gui.getInventory(), gui.getInputSlots());

        if (foundRecipe != null && foundRecipe.getType() == RecipeType.PROCESSING) {
            MachineRecipe machineRecipe = (MachineRecipe) foundRecipe;

            // 1. Kiểm tra đầu ra (Output slots) có đủ chỗ không
            boolean haveEnoughOutputSlots = RecipeUtils.canFitInSlots(gui.getInventory(), gui.getOutputSlots(), machineRecipe.getOutputs());

            // 2. Kiểm tra năng lượng (Xem có đủ điện để mồi chạy tick đầu tiên không)
            boolean haveEnoughEnergy = true;
            if (machine instanceof IEnergyStorage energyMachine) {
                int energyNeeded = machine.getEnergyPerSec();
                // SỬA LỖI: Chỉ khi lượng rút thử < lượng cần thiết thì mới là thiếu điện
                if (energyMachine.extractEnergy(energyNeeded, true) < energyNeeded) {
                    haveEnoughEnergy = false;
                }
            }

            // 3. Nếu mọi điều kiện đều hoàn hảo -> BẮT ĐẦU CHẠY
            if (haveEnoughOutputSlots && haveEnoughEnergy) {
                RecipeUtils.consumeInputs(gui.getInventory(), gui.getInputSlots(), machineRecipe.getInputs());
                machine.setCurrentRecipe(machineRecipe);
                machine.setProgressing(true);
                machine.setProgress(0);
                machine.setPercent(0);
            }
        }
    }

    private void addProgressOrStopMachine(ProcessingMachine machine, MachineGui gui){
        MachineRecipe recipe = machine.getCurrentRecipe();

        if (recipe == null) {
            machine.setProgressing(false);
            return;
        }

        // 1. KIỂM TRA VÀ TRỪ ĐIỆN CHO TICK NÀY
        boolean canProcessThisTick = true;

        if (machine instanceof IEnergyStorage energyMachine) {
            int energyNeeded = machine.getEnergyPerSec();

            // Thử rút điện
            if (energyMachine.extractEnergy(energyNeeded, true) == energyNeeded) {
                // Đủ điện -> TRỪ ĐIỆN THẬT
                energyMachine.extractEnergy(energyNeeded, false);
            } else {
                // Thiếu điện -> Đánh dấu không thể xử lý tick này
                canProcessThisTick = false;
            }
        }

        // 2. CHỈ TĂNG TIẾN TRÌNH KHI ĐỦ ĐIỆN
        if (canProcessThisTick) {
            int currentProgress = machine.getProgress() + 1;
            int maxProgressTime = recipe.getBaseProcessTime();

            machine.setProgress(currentProgress);

            // Tính %
            int percent = (currentProgress * 100) / maxProgressTime;
            machine.setPercent(percent);

            // 3. XỬ LÝ KHI HOÀN THÀNH 100%
            if (currentProgress >= maxProgressTime) {
                if (RecipeUtils.canFitInSlots(gui.getInventory(), gui.getOutputSlots(), recipe.getOutputs())) {
                    RecipeUtils.addToOutputSlot(gui.getInventory(), gui.getOutputSlots(), recipe.getOutputs());

                    // Trả về trạng thái rảnh rỗi chờ mẻ mới
                    machine.setProgressing(false);
                    machine.setProgress(0);
                    machine.setCurrentRecipe(null);
                    machine.setPercent(0);
                }
            }
        }
    }

    private void transferToOtherEnergyNode(EnergyNode node){
        for(Location targetLoc : node.getConnections()){
            SmpBlock targetSmpBlock = BlockManager.this.getBlock(targetLoc);

            if(targetSmpBlock instanceof EnergyNode targetNode){
                int diff = node.getEnergy() - targetNode.getEnergy();

                if(diff > 10){
                    int amountToTransfer = Math.min(diff / 2, node.getTransferRate());

                    node.extractEnergy(amountToTransfer, false);
                    targetNode.receiveEnergy(amountToTransfer, false);
                }
            }
        }
    }

    private void transferToElectricMachine(EnergyNode node, Location loc){
        for(BlockFace face: SmpMachine.FACES){
            Location adjLoc = loc.getBlock().getRelative(face).getLocation();
            SmpBlock adjBlock = BlockManager.this.getBlock(adjLoc);

            if(adjBlock instanceof IEnergyStorage adjEnergyMachine && !(adjBlock instanceof EnergyNode)){
                int toPush = node.extractEnergy(node.getTransferRate(), true);
                if(toPush > 0) {
                    int accepted = adjEnergyMachine.receiveEnergy(toPush, false);
                    node.extractEnergy(accepted, false);
                }
            }
        }
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
