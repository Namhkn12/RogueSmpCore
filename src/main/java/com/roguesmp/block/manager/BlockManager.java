package com.roguesmp.block.manager;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.block.impl.interfaces.IEnergyStorage;
import com.roguesmp.block.SmpBlock;
import com.roguesmp.block.impl.SmpMachine;
import com.roguesmp.block.impl.blocks.EnergyNode;
import com.roguesmp.block.impl.interfaces.IHaveInputOutput;
import com.roguesmp.block.impl.type.ActiveGenerator;
import com.roguesmp.block.impl.type.Generator;
import com.roguesmp.block.impl.type.PassiveGenerator;
import com.roguesmp.block.impl.type.ProcessingMachine;
import com.roguesmp.constant.RecipeType;
import com.roguesmp.gui.ActiveGeneratorGui;
import com.roguesmp.gui.MachineGui;
import com.roguesmp.recipe.BaseRecipe;
import com.roguesmp.recipe.IProcessableRecipe;
import com.roguesmp.recipe.impl.EnergyRecipe;
import com.roguesmp.recipe.impl.MachineRecipe;
import com.roguesmp.recipe.manager.RecipeManager;
import com.roguesmp.registry.BlockRegistry;
import com.roguesmp.utils.MachineTransferUtils;
import com.roguesmp.utils.RecipeUtils;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.Inventory;
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
                        if(gen instanceof PassiveGenerator pGen){
                            pGen.generate(loc);
                        }

                        //Đẩy điện đi
                        gen.pushEnergy(loc);
                    }
                    if(block instanceof EnergyNode node){
                        transferToOtherEnergyNode(node);
                        transferToElectricMachine(node, loc);
                    }
                    if(block instanceof SmpMachine machine && (machine instanceof ProcessingMachine || machine instanceof ActiveGenerator)){

                        findAndSetCurrentRecipe(block, machine);

                        if (!machine.isProgressing()) {processingIfCan(block, machine);}
                        else {addProgressOrStopMachine(machine, loc);}

                        //Auto push/pull items into other inventory or machine
                        MachineTransferUtils.processMachineTransfers(machine, loc, BlockManager.this);
                    }
                });
            }

        }.runTaskTimer(this.plugin, 0, PERIOD);
    }

    private void findAndSetCurrentRecipe(SmpBlock block, SmpMachine machine){
        if (machine.isProgressing() && machine.getCurrentRecipe() == null) {
            String machineId = block.getItem().getId();

            int[] inputSlots = new int[0];
            Inventory inv = machine.getGui().getInventory();

            if (machine.getGui() instanceof IHaveInputOutput mGui) {
                inputSlots = mGui.getInputSlots();
            }

            BaseRecipe foundRecipe = RecipeManager.findRecipe(machineId, inv, inputSlots);

            // KIỂM TRA SỰ TƯƠNG THÍCH GIỮA MÁY VÀ LOẠI CÔNG THỨC
            if (machine instanceof ProcessingMachine && foundRecipe instanceof MachineRecipe) {
                machine.setCurrentRecipe(foundRecipe);
            }
            else if (machine instanceof ActiveGenerator && foundRecipe instanceof EnergyRecipe) {
                machine.setCurrentRecipe(foundRecipe);
            }
            else {
                machine.setProgressing(false);
                machine.setProgress(0);
                return;
            }
        }
    }

    private void processingIfCan(SmpBlock block, SmpMachine machine){
        String machineId = block.getItem().getId();

        Inventory inv = machine.getGui().getInventory();
        int[] inputSlots = new int[0];
        int[] outputSlots = new int[0];

        // Lấy slot dựa theo loại GUI
        if (machine.getGui() instanceof IHaveInputOutput mGui) {
            inputSlots = mGui.getInputSlots();
            outputSlots = mGui.getOutputSlots();
        }

        BaseRecipe foundRecipe = RecipeManager.findRecipe(machineId, inv, inputSlots);

        if (foundRecipe != null) {
            // Phân loại: Đúng loại máy mới được chạy đúng loại công thức
            boolean isProcessingMatch = (machine instanceof ProcessingMachine && foundRecipe.getType() == RecipeType.PROCESSING);
            boolean isEnergyMatch = (machine instanceof ActiveGenerator && foundRecipe.getType() == RecipeType.ENERGY);

            if (isProcessingMatch || isEnergyMatch) {
                // 1. Kiểm tra đầu ra có đủ chỗ không (Dùng chung vì EnergyRecipe có thể có output như Xô Không)
                boolean haveEnoughOutputSlots = RecipeUtils.canFitInSlots(inv, outputSlots, foundRecipe.getOutputs());

                // 2. Phân nhánh logic năng lượng
                boolean canOperateEnergyWise = true;

                if (machine instanceof ProcessingMachine pMachine) {
                    int energyNeeded = pMachine.getEnergyPerSec();
                    if (pMachine instanceof IEnergyStorage pEnergyMachine && pEnergyMachine.extractEnergy(energyNeeded, true) < energyNeeded) {
                        canOperateEnergyWise = false;
                    }
                }
                if (machine instanceof ActiveGenerator aGen) {
                    int energyGen = aGen.getGenerationRate();
                    // Phát điện: Đầy điện thì KHÔNG chạy
                    if (aGen.receiveEnergy(energyGen, true) == 0) {
                        canOperateEnergyWise = false;
                    }
                }

                // 3. Nếu đủ điều kiện -> Bắt đầu chạy
                if (haveEnoughOutputSlots && canOperateEnergyWise) {
                    RecipeUtils.consumeInputs(inv, inputSlots, foundRecipe.getInputs());
                    machine.setCurrentRecipe(foundRecipe);
                    machine.setProgressing(true);
                    machine.setProgress(0);
                    machine.setPercent(0);
                }
            }
        }
    }

    private void addProgressOrStopMachine(SmpMachine machine, Location loc){
        BaseRecipe recipe = machine.getCurrentRecipe();

        if (recipe == null) {
            machine.setProgressing(false);
            return;
        }
        if (!(recipe instanceof IProcessableRecipe processableRecipe)) {
            machine.setProgressing(false);
            machine.setCurrentRecipe(null);
            return;
        }

        // 1. Phân nhánh trừ/cộng điện cho tick này
        boolean canProcessThisTick = true;

        if (machine instanceof ProcessingMachine pMachine) {
            int energyNeeded = pMachine.getEnergyPerSec();
            if (pMachine instanceof IEnergyStorage pEnergyMachine){
                if (pEnergyMachine.extractEnergy(energyNeeded, true) == energyNeeded) {
                    pEnergyMachine.extractEnergy(energyNeeded, false); // Rút điện để chạy
                } else {
                    canProcessThisTick = false; // Hết điện -> Tạm đứng im
                }
            }
        }
        else if (machine instanceof ActiveGenerator aGen) {
            int energyGen = aGen.getGenerationRate();
            int received = aGen.receiveEnergy(energyGen, true);

            if (received > 0) {
                aGen.generate(loc); // Nạp điện vừa sinh ra vào bản thân
            } else {
                canProcessThisTick = false; // Máy đầy điện -> Đứng im chờ rút bớt
            }
        }

        // 2. Chỉ tăng tiến trình khi có điện để hoạt động
        if (canProcessThisTick) {
            int currentProgress = machine.getProgress() + 1;
            int maxProgressTime = processableRecipe.getBaseProcessTime();

            machine.setProgress(currentProgress);
            machine.setPercent((currentProgress * 100) / maxProgressTime);

            // 3. Xử lý khi hoàn thành (100%)
            if (currentProgress >= maxProgressTime) {

                Inventory inv = machine.getGui().getInventory();
                int[] outputSlots = new int[0];

                if (machine.getGui() instanceof MachineGui mGui) {
                    outputSlots = mGui.getOutputSlots();
                } else if (machine.getGui() instanceof ActiveGeneratorGui aGui) {
                    outputSlots = aGui.getOutputSlots();
                }

                if (RecipeUtils.canFitInSlots(inv, outputSlots, recipe.getOutputs())) {
                    RecipeUtils.addToOutputSlot(inv, outputSlots, recipe.getOutputs());

                    machine.setProgressing(false);
                    machine.setProgress(0);
                    machine.setCurrentRecipe(null);
                    machine.setPercent(0);
                } else {
                    // Tắc đầu ra -> Không reset tiến trình, đợi chỗ trống
                    machine.setProgress(currentProgress - 1);
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

            if(adjBlock instanceof IEnergyStorage adjEnergyMachine
                    && !(adjBlock instanceof EnergyNode)
                    && !(adjBlock instanceof Generator)){

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
