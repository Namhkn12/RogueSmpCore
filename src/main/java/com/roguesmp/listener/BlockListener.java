package com.roguesmp.listener;

import com.roguesmp.block.BlockPos;
import com.roguesmp.block.SmpBlock;
import com.roguesmp.block.manager.BlockManager;
import com.roguesmp.constant.Keys;
import com.roguesmp.item.SmpItem;
import com.roguesmp.item.component.ItemComponentKeys;
import com.roguesmp.item.component.impl.BlockPlaceComponent;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageAbortEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class BlockListener implements Listener {
    private final BlockManager manager;

    public BlockListener(){
        this.manager = BlockManager.getInstance();
    }

    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event){
        manager.onEntitiesLoad(event.getEntities());
    }

    @EventHandler
    public void onEntitiesUnload(EntitiesUnloadEvent event){
        manager.onEntitiesUnload(event.getEntities());
    }

    @EventHandler
    public void onBlockDamage(BlockDamageEvent event){
//        event.getPlayer().sendMessage("hello");
        manager.getMining().onDamage(event);
    }

    @EventHandler
    public void onBlockDamageAbort(BlockDamageAbortEvent event){
//        event.getPlayer().sendMessage("stopped");
        manager.getMining().stop(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void denyBreakBeforeHydration(BlockBreakEvent event){
        SmpBlock block = manager.get(BlockPos.of(event.getBlock()));
        if(block != null && !block.isHydrated()) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event){
        manager.destroy(event);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event){
        Block clicked = event.getClickedBlock();
        if(clicked == null || event.useInteractedBlock() == Event.Result.DENY) return;

        SmpBlock target = manager.get(BlockPos.of(clicked));
        if(target != null && event.getHand() == EquipmentSlot.HAND){
            if(!target.isHydrated()){
                event.setCancelled(true);
                return;
            }

            target.onBlockInteract(event);
            if(event.useInteractedBlock() == Event.Result.DENY) return;
        }

        if(event.getAction() == Action.RIGHT_CLICK_BLOCK) tryPlace(event, clicked, target);
    }

    private void tryPlace(PlayerInteractEvent event, Block clicked, SmpBlock target){
        ItemStack item = event.getItem();
        Player player = event.getPlayer();
        if(item == null || event.getHand() == null || player.getGameMode() == GameMode.ADVENTURE) return;

        if(item.getPersistentDataContainer().get(Keys.ITEM_ID, PersistentDataType.STRING) == null) return;

        BlockPlaceComponent placement = SmpItem.wrap(item).getComponent(ItemComponentKeys.BLOCK_PLACE);
        if(placement == null) return;
        if(target == null && clicked.getType().isInteractable() && !player.isSneaking()) return;

        event.setUseItemInHand(Event.Result.DENY);
        manager.place(player, event.getHand(), clicked, event.getBlockFace(), placement.getBlockId());
    }
}
