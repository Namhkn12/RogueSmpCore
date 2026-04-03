package com.roguesmp.block.impl.generator.passive;

import com.roguesmp.block.impl.type.PassiveGenerator;
import com.roguesmp.gui.PassiveGeneratorGui;
import com.roguesmp.registry.ItemRegistry;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

public class SolarPanel extends PassiveGenerator {

    private final int GENERATION_RATE = 20;
    private final int TRANSFER_RATE = 20;
    private final int MAX_ENERGY = 500;

    public SolarPanel(){
        super(ItemRegistry.getInstance().getBaseItem("solar_panel"));
    }

    @Override
    public int getGenerationRate() {
        return GENERATION_RATE;
    }

    @Override
    public void generate(Location loc) {
        PassiveGeneratorGui genGui = (PassiveGeneratorGui) gui;
        if(getEnergy() >= getMaxEnergy()) {
            isNotRunning(genGui);
            return;
        }

        Block block = loc.getBlock();

        //Check if the sky is visible
        if(block.getRelative(BlockFace.UP).getLightFromSky() < 15) {
            isNotRunning(genGui);
            return;
        }

        long time = block.getWorld().getTime();
        boolean isDayTime = time >=0 && time < 12000;
        boolean isRaining = block.getWorld().hasStorm();

        if(block.getWorld().hasSkyLight() && isDayTime & !isRaining){
            this.receiveEnergy(getGenerationRate(), false);
            isRunning(genGui);
        }
    }

    @Override
    public int getMaxEnergy() {
        return MAX_ENERGY;
    }

    @Override
    public int getTransferRate() {
        return TRANSFER_RATE;
    }

    @Override
    public void setPercent(int percent) {}
}
