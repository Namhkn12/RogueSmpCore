package com.roguesmp.block;

import com.roguesmp.block.manager.BlockManager;
import com.roguesmp.item.BaseItem;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.block.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class SmpBlock {

    private final BaseItem item;
    private boolean placeable = true;
    private final BlockManager manager;

    public SmpBlock(BaseItem item) {
        this.item = item;
        manager = BlockManager.getInstance();
    }

    public SmpBlock(BaseItem item, boolean placeable){
        this(item);
        this.placeable = placeable;
    }

    private ItemStack getItemStack(int stackAmount){
        return this.item.generateItemStack(stackAmount);
    }

    public void onBlockPlace(BlockPlaceEvent event, String id){
        ItemStack item = event.getItemInHand();

        if(!item.hasItemMeta()) return;
        if(placeable){
            Location loc = event.getBlock().getLocation();
            manager.registerBlock(loc, id);
        }
        else{
            event.setCancelled(true);
        }

    }

    public void onBlockBreak(BlockBreakEvent event){
        Location loc = event.getBlock().getLocation();

        manager.removeBlock(loc);
        loc.getWorld().dropItemNaturally(loc, getItemStack(1));
    }

    //Disabled piston push on default
    public void onPistonPush(BlockPistonExtendEvent event){
        event.setCancelled(true);
    }

    //Disabled sticky piston grab on default
    public void onPistonGrab(BlockPistonRetractEvent event){
        event.setCancelled(true);
    }

    //SmpBlock can't get destroyed by explosion
    public void onBlockExploded(BlockExplodeEvent event){

    }

    //SmpBlock on default when interact do nothing
    public void onBlockInteract(PlayerInteractEvent event){

    }

    public BaseItem getItem(){
        return item;
    }
}
