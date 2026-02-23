package com.roguesmp.listener;

import com.roguesmp.block.SmpBlock;
import com.roguesmp.block.manager.BlockManager;
import com.roguesmp.constant.Keys;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class BlockListener implements Listener {
    private final BlockManager manager;

    public BlockListener(){
        this.manager = BlockManager.getInstance();
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event){
        ItemStack item = event.getItemInHand();

        if(!item.hasItemMeta()) return;

        PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
        if(!container.has(Keys.ITEM_ID, PersistentDataType.STRING)) return;

        String id = container.get(Keys.ITEM_ID, PersistentDataType.STRING);
        SmpBlock block = manager.getBlock(id);

        block.onBlockPlace(event, id);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event){
        Location loc = event.getBlock().getLocation();

        if(!manager.isSmpBlock(loc)) return;
        else{
            SmpBlock block = manager.getBlock(loc);
            block.onBlockBreak(event);
        }
    }

    @EventHandler
    public void onPistonPush(BlockPistonExtendEvent event){
        List<Block> pushedBlock = event.getBlocks();

        for(Block b: pushedBlock){
            Location loc = b.getLocation();
            if(manager.isSmpBlock(loc)){
                SmpBlock block = manager.getBlock(loc);
                block.onPistonPush(event);
            }
        }
    }

    @EventHandler
    public void onPistonGrab(BlockPistonRetractEvent event){
        Block grabbedBlock = event.getBlock();
        Location loc = grabbedBlock.getLocation();

        if(manager.isSmpBlock(loc)){
            SmpBlock block = manager.getBlock(loc);
            block.onPistonGrab(event);
        }
    }

    @EventHandler
    public void onBlockExploded(BlockExplodeEvent event){
        List<Block> explodedBlocks = event.blockList();

        for(Block b: explodedBlocks){
            Location loc = b.getLocation();
            if(manager.isSmpBlock(loc)){
                SmpBlock block = manager.getBlock(loc);
                block.onBlockExploded(event);
            }
        }
    }

    @EventHandler
    public void onBlockInteract(PlayerInteractEvent event){
        Block block = event.getClickedBlock();

        if(block != null) {
            Location loc = block.getLocation();
            if(manager.isSmpBlock(loc)){
                SmpBlock smpBlock = manager.getBlock(loc);
                smpBlock.onBlockInteract(event);
            }
        }
    }
}
