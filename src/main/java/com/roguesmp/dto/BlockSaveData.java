package com.roguesmp.dto;

import com.roguesmp.annotation.GsonIgnore;
import com.roguesmp.block.SmpBlock;
import com.roguesmp.block.impl.SmpMachine;
import com.roguesmp.constant.TransferMode;
import com.roguesmp.utils.InventoryBase64;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NonNull;

import java.util.HashMap;
import java.util.Map;

public class BlockSaveData {

    public String blockId;
    public int x, y, z;

    // Các biến cho máy móc
    public boolean isMachine;
    public int progress;
    public boolean isProgressing;
    public String inventoryBase64;
    public String currentRecipeId;
    public Map<BlockFace, TransferMode> sideConfigs;

    // Biến này chỉ dùng lúc code, KHÔNG LƯU VÀO JSON
    @GsonIgnore
    public transient org.bukkit.Location tempLocation;

    public BlockSaveData(Location loc, String id, SmpBlock block) {
        this.blockId = id;
        this.x = loc.getBlockX();
        this.y = loc.getBlockY();
        this.z = loc.getBlockZ();
        this.tempLocation = loc;

        if (block instanceof SmpMachine) {
            SmpMachine machine = (SmpMachine) block;
            this.isMachine = true;
            this.progress = machine.getProgress();
            this.isProgressing = machine.isProgressing();
            this.sideConfigs = machine.getSideConfigs();

            // LƯU ID CỦA RECIPE (Nếu máy đang chạy một recipe nào đó)
            if (machine.getCurrentRecipe() != null) {
                this.currentRecipeId = machine.getCurrentRecipe().getId();
            } else {
                this.currentRecipeId = null;
            }

            // Hàm chuyển Map Item thành Base64 bằng Paper API (như đã bàn ở trên)
            // Lấy từ gui.getInputSlots() và gui.getOutputSlots()
            this.inventoryBase64 = InventoryBase64.itemMapToBase64(getMachineItems(machine));
        } else {
            this.isMachine = false;
        }
    }

    // Helper lọc đồ
    private Map<Integer, org.bukkit.inventory.ItemStack> getMachineItems(SmpMachine machine) {
        Map<Integer, org.bukkit.inventory.ItemStack> map = new HashMap<>();
        org.bukkit.inventory.Inventory inv = machine.getGui().getInventory();

        for (int slot : machine.getGui().getInputSlots()) {
            if (inv.getItem(slot) != null) map.put(slot, inv.getItem(slot));
        }
        for (int slot : machine.getGui().getOutputSlots()) {
            if (inv.getItem(slot) != null) map.put(slot, inv.getItem(slot));
        }
        return map;
    }
}
