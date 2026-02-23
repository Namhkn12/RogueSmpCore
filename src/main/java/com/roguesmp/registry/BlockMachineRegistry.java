package com.roguesmp.registry;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.block.SmpBlock;
import com.roguesmp.block.impl.SmpMachine;
import com.roguesmp.block.impl.machine.SteelFurnace;
import com.roguesmp.block.manager.BlockManager;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;

public class BlockMachineRegistry {
    private static BlockMachineRegistry INSTANCE;
    public static final String FOLDER_NAME = "block";
    private final Plugin plugin;
    private static final Map<String, SmpBlock> dataMap = new HashMap<>();

    public BlockMachineRegistry(RogueSmpCore plugin) {
        this.plugin = plugin;

        setup();
    }

    private void setup(){
        BlockManager.registerBlockType("steel_furnace", SteelFurnace::new);
        dataMap.put("steel_furnace", new SteelFurnace());
    }

    public void registerMachineRecipes(){
        for(SmpBlock block : dataMap.values()){
            if(block instanceof SmpMachine machine){
                machine.registerRecipes();
            }
        }
    }

    public Map<String, SmpBlock> getRegistry() {return new HashMap<>(dataMap);}

    public static void init(RogueSmpCore plugin) {INSTANCE = new BlockMachineRegistry(plugin);}

    public static BlockMachineRegistry getInstance(){
        return INSTANCE;
    }
}
