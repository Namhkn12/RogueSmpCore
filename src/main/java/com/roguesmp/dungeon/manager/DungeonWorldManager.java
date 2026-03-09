package com.roguesmp.dungeon.manager;

import com.roguesmp.dungeon.data.Region;

import java.util.HashMap;
import java.util.Map;

public class DungeonWorldManager {
    private final String dungeonWorld = "dungeons_test";
    private final Map<String, RegionManager> dungeonWorldManager = new HashMap<>();

    public void init(){
        //create new file world if world name dungeon_test does not exist
    }

    public void generateDungeonWorld(){
        // generate world
        // create region manager
        // add to Map
    }

    public Region getRegionSlotForDungeon(){
        //check dungeonWorldManager to get RegionManager instance
        //check is full slot in regionManager
        //if have slot => create new region by regionManager
        //if all not => generate new dungeon world and assign new RegionManager then create new region
        //return region
        return new Region();
    }
}
