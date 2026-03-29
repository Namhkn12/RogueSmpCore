package com.roguesmp.dto;

import com.roguesmp.annotation.GsonIgnore;
import com.roguesmp.block.impl.interfaces.IEnergyStorage;
import com.roguesmp.block.SmpBlock;
import com.roguesmp.block.impl.SmpMachine;
import com.roguesmp.block.impl.blocks.EnergyNode;
import com.roguesmp.block.impl.interfaces.IHaveInputOutput;
import com.roguesmp.block.impl.type.ActiveGenerator;
import com.roguesmp.block.impl.type.PassiveGenerator;
import com.roguesmp.block.impl.type.ProcessingMachine;
import com.roguesmp.constant.TransferMode;
import com.roguesmp.gui.ActiveGeneratorGui;
import com.roguesmp.gui.MachineGui;
import com.roguesmp.utils.InventoryBase64;
import com.roguesmp.utils.Utils;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

import java.util.HashMap;
import java.util.List;
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
    public int storedEnergy;

    // Chỉ dành cho Energy Node
    public List<String> linkedNodes;

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
            if(!(machine instanceof PassiveGenerator) && machine.getGui() != null){
                this.inventoryBase64 = InventoryBase64.itemMapToBase64(getMachineItems(machine));
            }

            if(block instanceof IEnergyStorage energyMachine){
                this.storedEnergy = energyMachine.getEnergy();
            }
            else{
                this.storedEnergy = 0;
            }

            if(block instanceof EnergyNode node){
                this.linkedNodes = node.getConnections().stream().map(Utils::locationToString).toList();
            }
        } else {
            this.isMachine = false;
        }

    }

    // Helper lọc đồ
    private Map<Integer, org.bukkit.inventory.ItemStack> getMachineItems(SmpMachine machine) {
        Map<Integer, org.bukkit.inventory.ItemStack> map = new HashMap<>();
        org.bukkit.inventory.Inventory inv = machine.getGui().getInventory();

        int[] inputSlots = new int[0];
        int[] outputSlots = new int[0];

        if(machine instanceof IHaveInputOutput inputOutput) {
            inputSlots = inputOutput.getInputSlots();
            outputSlots = inputOutput.getOutputSlots();
        }

        for (int slot : inputSlots) {
            if (inv.getItem(slot) != null) map.put(slot, inv.getItem(slot));
        }
        for (int slot : outputSlots) {
            if (inv.getItem(slot) != null) map.put(slot, inv.getItem(slot));
        }

        return map;
    }
}
