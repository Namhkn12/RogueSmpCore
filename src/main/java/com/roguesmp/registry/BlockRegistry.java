package com.roguesmp.registry;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.block.SmpBlock;
import com.roguesmp.block.impl.SmpMachine;
import com.roguesmp.block.impl.blocks.EnergyNode;
import com.roguesmp.block.impl.generator.passive.SolarPanel;
import com.roguesmp.block.impl.machine.ElectricSteelFurnace;
import com.roguesmp.block.impl.machine.SteelFurnace;
import com.roguesmp.block.manager.BlockManager;
import com.roguesmp.item.BaseItem;
import com.roguesmp.item.component.impl.NameComponent;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class BlockRegistry {
    private static BlockRegistry INSTANCE;
    public static final String FOLDER_NAME = "block";
    private final Plugin plugin;
    private static final Map<String, SmpBlock> dataMap = new HashMap<>();

    public BlockRegistry(RogueSmpCore plugin) {
        this.plugin = plugin;

        setup();
    }

    private void setup(){
        BlockManager.registerBlockType("steel_furnace", SteelFurnace::new);
        BlockManager.registerBlockType("electric_steel_furnace", ElectricSteelFurnace::new);
        BlockManager.registerBlockType("energy_node", EnergyNode::new);
        BlockManager.registerBlockType("solar_panel", SolarPanel::new);

        registerDataMap();
    }

    private void registerDataMap(){
        dataMap.put("steel_furnace", new SteelFurnace());
        dataMap.put("steel_block", new SmpBlock(ItemRegistry.getInstance().getBaseItem("steel_block"), true));
        dataMap.put("electric_steel_furnace", new ElectricSteelFurnace());
        dataMap.put("energy_node", new EnergyNode());
        dataMap.put("solar_panel", new SolarPanel());
    }

    public void registerMachineRecipes(){
        for(SmpBlock block : dataMap.values()){
            if(block instanceof SmpMachine machine){
                machine.registerRecipes();
            }
        }
    }

    public Map<String, SmpBlock> getRegistry() {return Collections.unmodifiableMap(dataMap);}

    public static SmpBlock getBlock(String id) {return dataMap.get(id);}

    public static void init(RogueSmpCore plugin) {INSTANCE = new BlockRegistry(plugin);}

    public static BlockRegistry getInstance(){
        return INSTANCE;
    }
}
