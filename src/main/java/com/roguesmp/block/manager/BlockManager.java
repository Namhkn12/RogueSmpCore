package com.roguesmp.block.manager;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.block.SmpBlock;
import com.roguesmp.block.impl.SmpMachine;
import com.roguesmp.constant.RecipeType;
import com.roguesmp.gui.MachineGui;
import com.roguesmp.recipe.BaseRecipe;
import com.roguesmp.recipe.impl.MachineRecipe;
import com.roguesmp.recipe.manager.RecipeManager;
import com.roguesmp.registry.BlockMachineRegistry;
import com.roguesmp.utils.RecipeUtils;
import org.bukkit.Bukkit;
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
                        // 1. NẾU MÁY ĐANG RẢNH -> TÌM CÔNG THỨC MỚI VÀ BẮT ĐẦU
                        if (!machine.isProgressing()) {
                            String machineId = block.getItem().getId();
                            BaseRecipe foundRecipe = RecipeManager.findRecipe(machineId, gui.getInventory(), gui.getInputSlots());

                            if (foundRecipe != null && foundRecipe.getType() == RecipeType.PROCESSING) {
                                MachineRecipe machineRecipe = (MachineRecipe) foundRecipe;
                                boolean haveEnoughOutputSlots = RecipeUtils.canFitInSlots(gui.getInventory(), gui.getOutputSlots(), machineRecipe.getOutputs());

                                // Bắt đầu chạy
                                if (haveEnoughOutputSlots) {
                                    RecipeUtils.consumeInputs(gui.getInventory(), gui.getInputSlots(), machineRecipe.getInputs());
                                    machine.setCurrentRecipe(machineRecipe); // Ghi nhớ công thức
                                    machine.setProgressing(true);
                                    machine.setProgress(0);
                                    machine.setPercent(0);
                                }
                            }
                        }

                        // 2. NẾU MÁY ĐANG CHẠY -> TĂNG TIẾN TRÌNH
                        else {
                            MachineRecipe recipe = machine.getCurrentRecipe();

                            // Đề phòng trường hợp lỗi mất data
                            if (recipe == null) {
                                machine.setProgressing(false);
                                return;
                            }

                            int currentProgress = machine.getProgress() + 1; // Cộng lên 1 ngay lập tức
                            int maxProgressTime = recipe.getBaseProcessTime();

                            machine.setProgress(currentProgress);

                            // Sửa lỗi tính phần trăm: Ép kiểu float hoặc nhân 100 trước khi chia
                            int percent = (currentProgress * 100) / maxProgressTime;
                            machine.setPercent(percent);

                            // 3. NẾU XONG -> NHẢ OUTPUT VÀ RESET
                            if (currentProgress >= maxProgressTime) {
                                // (Tùy chọn) Kiểm tra lại output slot lỡ như player vừa nhét thêm đồ làm đầy kho
                                if (RecipeUtils.canFitInSlots(gui.getInventory(), gui.getOutputSlots(), recipe.getOutputs())) {
                                    RecipeUtils.addToOutputSlot(gui.getInventory(), gui.getOutputSlots(), recipe.getOutputs());

                                    // Reset trạng thái về rảnh rỗi
                                    machine.setProgressing(false);
                                    machine.setProgress(0);
                                    machine.setCurrentRecipe(null);
                                    machine.setPercent(0);
                                } else {
                                    // Nếu đầy kho ngay lúc chuẩn bị nhả đồ -> Máy kẹt (giữ nguyên ở 100%)
                                    machine.setProgress(currentProgress - 1); // Lùi lại 1 tick để chờ
                                }
                            }
                        }
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
        SmpBlock block = constructor.get();
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
        return BlockMachineRegistry.getInstance().getRegistry().get(id);
    }

    public static void init(RogueSmpCore core) {INSTANCE = new BlockManager(core);}
}
