package com.roguesmp.block.impl.blocks;

import com.roguesmp.block.impl.interfaces.IEnergyStorage;
import com.roguesmp.block.SmpBlock;
import com.roguesmp.block.impl.SmpMachine;
import com.roguesmp.block.manager.BlockManager;
import com.roguesmp.gui.BaseGui;
import com.roguesmp.registry.ItemRegistry;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Set;

public class EnergyNode extends SmpMachine implements IEnergyStorage {

    private int energy = 0;
    private final int MAX_ENERGY = 5000;
    private final int TRANSFER_RATE = 100;

    private final Set<Location> connections = new HashSet<>();

    public EnergyNode(){
        super(ItemRegistry.getInstance().getBaseItem("energy_node"));
    }

    public void addConnection(Location loc){
        if(this.connections.contains(loc)) return;
        if(BlockManager.getInstance().getBlock(loc) instanceof IEnergyStorage){
            this.connections.add(loc);
        }
    }

    public void removeConnection(Location loc){
        this.connections.remove(loc);
    }

    public Set<Location> getConnections () {return this.connections;}

    public void clearConnections() {this.connections.clear();}

    @Override
    public int getEnergy() {return energy;}

    @Override
    public void setEnergy(int energy) {this.energy = Math.max(0, Math.min(energy, MAX_ENERGY));}

    @Override
    public int getMaxEnergy() {return MAX_ENERGY;}

    public int getTransferRate() {return TRANSFER_RATE;}

    @Override
    protected BaseGui createGui() {return null;}

    @Override
    public void registerRecipes() {}

    @Override
    public void setPercent(int percent) {

    }

    @Override
    public void onBlockInteract(PlayerInteractEvent event) {

    }

    @Override
    public void onBlockBreak(BlockBreakEvent event) {
        Location myLoc = event.getBlock().getLocation();
        BlockManager manager = BlockManager.getInstance();

        for (Location connectedLoc : this.getConnections()) {
            SmpBlock connectedSmp = manager.getBlock(connectedLoc);

            if (connectedSmp instanceof EnergyNode connectedNode) {
                connectedNode.removeConnection(myLoc);
            }
        }

        this.clearConnections();

        super.onBlockBreak(event);
    }
}
