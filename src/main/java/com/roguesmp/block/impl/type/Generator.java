package com.roguesmp.block.impl.type;

import com.roguesmp.block.IEnergyStorage;
import com.roguesmp.block.SmpBlock;
import com.roguesmp.block.impl.SmpMachine;
import com.roguesmp.block.manager.BlockManager;
import com.roguesmp.constant.ComponentKeys;
import com.roguesmp.gui.BaseGui;
import com.roguesmp.item.BaseItem;
import com.roguesmp.item.component.impl.NameComponent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

public abstract class Generator extends SmpMachine implements IEnergyStorage {

    private int energy = 0;
    private final int MAX_ENERGY = 5000;
    private final int TRANSFER_RATE = 100;

    public Generator(BaseItem baseItem, Material progressDisplay) {
        super(baseItem, progressDisplay);
    }
    public Generator(BaseItem baseItem) {super(baseItem);}

    protected abstract BaseGui createGui();
    public abstract void registerRecipes();

    @Override public int getEnergy() {return energy;}
    @Override public void setEnergy(int energy) {this.energy = Math.max(0, Math.min(energy, MAX_ENERGY));}
    @Override public int getMaxEnergy() {return MAX_ENERGY;}
    public abstract int getTransferRate();

    public abstract void generate(Location loc);

    public void pushEnergy(Location loc) {
        if (this.getEnergy() <= 0) return; // Hết điện thì thôi không đẩy

        Block currentBlock = loc.getBlock();

        for (BlockFace face : FACES) {
            Location adjLoc = currentBlock.getRelative(face).getLocation();
            SmpBlock adjBlock = BlockManager.getInstance().getBlock(adjLoc);

            // Nếu khối bên cạnh nhận được điện (Và không phải là một cái Generator khác để tránh bơm chéo nhau)
            if (adjBlock instanceof IEnergyStorage targetStorage && !(adjBlock instanceof Generator)) {

                // 1. Hỏi xem máy phát có thể đẩy tối đa bao nhiêu
                int toPush = this.extractEnergy(TRANSFER_RATE, true);

                if (toPush > 0) {
                    // 2. Ép đối phương nhận và xem đối phương nhận được bao nhiêu
                    int accepted = targetStorage.receiveEnergy(toPush, false);

                    // 3. Trừ đi lượng điện thực tế đã bơm thành công
                    this.extractEnergy(accepted, false);
                }
            }
        }
    }
}
