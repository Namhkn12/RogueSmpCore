package com.roguesmp.block.storage;

import com.google.gson.reflect.TypeToken;
import com.roguesmp.RogueSmpCore;
import com.roguesmp.block.impl.interfaces.IEnergyStorage;
import com.roguesmp.block.SmpBlock;
import com.roguesmp.block.impl.SmpMachine;
import com.roguesmp.block.impl.blocks.EnergyNode;
import com.roguesmp.block.impl.interfaces.IHaveLockedRecipe;
import com.roguesmp.block.impl.type.ProcessingMachine;
import com.roguesmp.block.manager.BlockManager;
import com.roguesmp.constant.TransferMode;
import com.roguesmp.dto.BlockSaveData;
import com.roguesmp.recipe.BaseRecipe;
import com.roguesmp.recipe.IProcessableRecipe;
import com.roguesmp.recipe.impl.MachineRecipe;
import com.roguesmp.recipe.manager.RecipeManager;
import com.roguesmp.utils.InventoryBase64;
import com.roguesmp.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BlockStorage {
    public static BlockStorage INSTANCE;

    private final String FOLDER_NAME = "blocks";
    private final RogueSmpCore plugin;
    private final BlockManager manager;

    public BlockStorage(RogueSmpCore plugin, BlockManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    private File getDataFolder() {
        return new File(plugin.getDataFolder(), FOLDER_NAME);
    }

    public void saveToFile(boolean override) {
        File folder = getDataFolder();

        if (!folder.exists() && !folder.mkdirs()) {
            plugin.getLogger().severe("Failed to create " + FOLDER_NAME + " directory");
            return;
        }

        plugin.getLogger().info("Saving block data...");

        // 1. Gom nhóm block theo World
        Map<String, List<BlockSaveData>> worldDataMap = new HashMap<>();
        Map<Location, SmpBlock> blocks = manager.getAllBlocks();
        Map<Location, String> ids = manager.getAllBlockIds();

        for (Map.Entry<Location, SmpBlock> entry : blocks.entrySet()) {
            Location loc = entry.getKey();
            String worldName = loc.getWorld().getName();

            BlockSaveData data = new BlockSaveData(loc, ids.get(loc), entry.getValue());
            worldDataMap.computeIfAbsent(worldName, k -> new ArrayList<>()).add(data);
        }

        // 2. Lặp qua TẤT CẢ các world đang được load trên server
        for (World world : Bukkit.getWorlds()) {
            String worldName = world.getName();
            File worldFile = new File(folder, worldName + ".json");

            // Lấy danh sách máy móc của world này (có thể null nếu world không có máy nào)
            List<BlockSaveData> dataList = worldDataMap.get(worldName);

            // Nếu KHÔNG cho phép override và file đã tồn tại -> Bỏ qua
            if (worldFile.exists() && !override) {
                continue;
            }

            // TRƯỜNG HỢP A: World này không có/không còn máy móc nào
            if (dataList == null || dataList.isEmpty()) {
                // XÓA FILE CŨ ĐỂ WIPE DATA (Sửa lỗi đập hết máy nhưng lúc load vẫn còn)
                if (worldFile.exists() && override) {
                    worldFile.delete();
                }
                continue; // Chuyển sang world tiếp theo
            }

            // TRƯỜNG HỢP B: World có máy móc -> Ghi đè file
            // Lớp FileWriter(worldFile) mặc định sẽ wipe sạch nội dung file cũ trước khi ghi
            try (Writer writer = new FileWriter(worldFile)) {
                Utils.GSON.toJson(dataList, writer);
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to save blocks for world: " + worldName);
                e.printStackTrace();
            }
        }
    }

    public void loadFromFile() {
        File folder = getDataFolder();

        // 1. Kiểm tra thư mục có tồn tại không
        if (!folder.exists()) {
            return;
        }

        // 2. Lấy danh sách tất cả các file .json trong thư mục blocks
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null || files.length == 0) {
            return;
        }

        plugin.getLogger().info("Loading block data...");

        // Khai báo kiểu List<BlockSaveData> cho GSON hiểu để parse cấu trúc mảng JSON
        Type listType = new TypeToken<List<BlockSaveData>>(){}.getType();

        for (File file : files) {
            // Lấy tên world từ tên file (VD: "world.json" -> "world")
            String worldName = file.getName().replace(".json", "");
            World world = Bukkit.getWorld(worldName);

            // Bỏ qua nếu world chưa được load (thường xảy ra với các plugin tạo map động)
            if (world == null) {
                plugin.getLogger().warning("Bỏ qua file " + file.getName() + " vì World này chưa được load!");
                continue;
            }

            try (Reader reader = new FileReader(file)) {
                // SỬ DỤNG Utils.GSON ĐỂ ĐỒNG BỘ PROJECT
                List<BlockSaveData> dataList = Utils.GSON.fromJson(reader, listType);

                if (dataList == null || dataList.isEmpty()) {
                    continue;
                }

                for (BlockSaveData data : dataList) {
                    Location loc = new Location(world, data.x, data.y, data.z);

                    // 1. Đăng ký block vào Manager (Gọi hàm tạo Block và lưu vào Map)
                    manager.registerBlock(loc, data.blockId);

                    // 2. Phục hồi dữ liệu nếu nó là Máy móc
                    if (data.isMachine) {
                        SmpBlock block = manager.getBlock(loc);
                        if (block instanceof SmpMachine) {
                            SmpMachine machine = (SmpMachine) block;

                            machine.setProgress(data.progress);
                            machine.setProgressing(data.isProgressing);

                            if (data.sideConfigs != null) {
                                for (Map.Entry<BlockFace, TransferMode> entry : data.sideConfigs.entrySet()) {
                                    machine.setTransferMode(entry.getKey(), entry.getValue());
                                }
                            }

                            // --- PHỤC HỒI RECIPE ---
                            if (data.currentRecipeId != null && !data.currentRecipeId.isEmpty()) {
                                BaseRecipe savedRecipe = RecipeManager.getRecipe(data.currentRecipeId);

                                if (savedRecipe != null) {
                                    machine.setCurrentRecipe(savedRecipe);
                                } else {
                                    // Đề phòng trường hợp Admin xóa mất recipe đó khỏi code/config
                                    plugin.getLogger().warning("Không tìm thấy Recipe ID '" + data.currentRecipeId + "' cho máy tại " + loc.toString() + ". Máy sẽ bị dừng.");
                                    machine.setProgressing(false);
                                    machine.setProgress(0);
                                    machine.setCurrentRecipe(null);
                                }
                            }

                            // Phục hồi Inventory từ Base64
                            Map<Integer, ItemStack> savedItems = InventoryBase64.itemMapFromBase64(data.inventoryBase64);
                            for (Map.Entry<Integer, ItemStack> entry : savedItems.entrySet()) {
                                machine.getGui().getInventory().setItem(entry.getKey(), entry.getValue());
                            }

                            //Phục hồi điện nếu máy có điện
                            if (block instanceof IEnergyStorage energyMachine) {
                                energyMachine.setEnergy(data.storedEnergy);
                            }

                            //Phục hồi connections của energy node
                            if(block instanceof EnergyNode node){
                                for(String connectedNode: data.linkedNodes){
                                    node.addConnection(Utils.stringToLocation(connectedNode));
                                }
                            }

                            if(machine instanceof ProcessingMachine processingMachine){
                                // Cập nhật lại giao diện (% hiển thị)
                                if(machine.isProgressing() && machine.getCurrentRecipe() != null) {
                                    BaseRecipe currentRecipe = machine.getCurrentRecipe();
                                    if(!(currentRecipe instanceof IProcessableRecipe processableRecipe)){
                                        return;
                                    }
                                    processingMachine.setPercent((machine.getProgress() * 100) / processableRecipe.getBaseProcessTime());
                                }
                            }

                            //Phục hồi locked recipe nếu block đó có
                            if(machine instanceof IHaveLockedRecipe mLockedRecipe){
                                mLockedRecipe.setLockedRecipe(RecipeManager.getRecipe(data.lockedRecipeId));
                            }
                        }
                    }
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to load block file: " + file.getName());
                e.printStackTrace();
            }
        }

        plugin.getLogger().info("Successfully loaded block data.");
    }

    public static void init(RogueSmpCore plugin, BlockManager manager) {INSTANCE = new BlockStorage(plugin, manager);}

    public static BlockStorage getInstance() {return INSTANCE;}
}
