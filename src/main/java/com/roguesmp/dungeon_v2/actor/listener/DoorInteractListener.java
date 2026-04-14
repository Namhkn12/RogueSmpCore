package com.roguesmp.dungeon_v2.actor.listener;

import com.roguesmp.dungeon_v2.controller_.DungeonFlowController;
import com.roguesmp.dungeon_v2.manager.InstanceManager;
import com.roguesmp.dungeon_v2.utils.DungeonEcho;
import com.roguesmp.dungeon_v2.utils.filterchain.EventFilter;
import com.roguesmp.dungeon_v2.utils.filterchain.FilterChain;
import com.roguesmp.dungeon_v2.utils.filterchain.impl.InteractFilters;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Vault;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.BoundingBox;

public class DoorInteractListener implements Listener {

    private final DungeonFlowController dungeonFlowController;
    private final InstanceManager instanceManager;

    public DoorInteractListener(DungeonFlowController dungeonFlowController, InstanceManager instanceManager) {
        this.dungeonFlowController = dungeonFlowController;
        this.instanceManager = instanceManager;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if(!VAULT_FILTER.test(event)) return;
        event.setCancelled(true);

        Block block = event.getClickedBlock();
        Vault vault = (Vault) block.getState();
        Player player = event.getPlayer();

        dungeonFlowController.handleOpenNextDoor(vault.getBlock(), player);
    }

    @EventHandler
    public void onPlayerGetInRewardRoom(PlayerMoveEvent event) {
        if (!event.hasChangedBlock()) return;

        Player player = event.getPlayer();
        String worldName = player.getWorld().getName();

        if (!worldName.startsWith("dungeon_")) return;
        if (!isOverlappingBlockType(event, Material.END_GATEWAY)) return;
        /*Get into the treasure room*/
        dungeonFlowController.handleGetIntoTreasurePortal(player);
    }

    @EventHandler
    public void onPlayerLeaveRewardRoom(PlayerMoveEvent event) {
        if (!event.hasChangedBlock()) return;

        Player player = event.getPlayer();
        String worldName = player.getWorld().getName();

        if (!worldName.startsWith("dungeon_")) return;
        if (player.getLocation().getBlock().getType() != Material.END_PORTAL) return;
    }

    @EventHandler
    public void onCancelPlayerTeleportByPortal(PlayerTeleportEvent event) {
        if (!event.getPlayer().getWorld().getName().startsWith("dungeon_")) return;

        PlayerTeleportEvent.TeleportCause cause = event.getCause();
        if (cause == PlayerTeleportEvent.TeleportCause.END_PORTAL
                || cause == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL
                || cause == PlayerTeleportEvent.TeleportCause.END_GATEWAY) {
            event.setCancelled(true);
        }
    }


    @EventHandler
    public void onPlayerEnterEndGateway(PlayerTeleportEvent e) {
//        if (e.getCause() != PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) return;
//        if (!e.getPlayer().getWorld().getName().startsWith("dungeon_")) return;
//
//        e.setCancelled(true);
//
//        Player player = e.getPlayer();
//
//        var partyResponse = partyController.getPartyByPlayer(player);
//        if (partyResponse == null || partyResponse.getData() == null) return;
//        Party party = partyResponse.getData();
//
//        var instanceResponse = dungeonController.getInstanceByParty(party.getPartyId());
//        if (instanceResponse == null || instanceResponse.getData() == null) return;
//        DungeonInstance instance = instanceResponse.getData();
//
//        // TODO: xử lý thoát dungeon — teleport về lobby, trao thưởng, cleanup instance...
//        Location baseLoc = instance.getRegion().getLocation();
//        int rewardCount = instance.getRewardRoomCount();
//        Location buildLoc = baseLoc.clone().add(0, rewardCount * 50, 0);
//
//        buildingController.buildSchematicById(
//                "schemeta_20260329202750",
//                buildLoc
//        );
//        instance.setRewardRoomCount(++rewardCount);
//        player.teleport(buildLoc);
    }

    private static final EventFilter<PlayerInteractEvent> VAULT_FILTER =
            FilterChain.of(PlayerInteractEvent.class)
                    .require(InteractFilters.rightClickBlock())
                    .require(InteractFilters.mainHand())
                    .require(InteractFilters.hasBlock())
                    .require(InteractFilters.blockState(Vault.class))
                    .require(InteractFilters.clickedBlockInWorld("dungeon_"))
                    .build();

    private boolean isOverlappingBlockType(PlayerMoveEvent event, Material material) {
        if (event.getTo() == null) return false;

        Player player = event.getPlayer();
        BoundingBox movedBox = player.getBoundingBox().clone().shift(
                event.getTo().getX() - event.getFrom().getX(),
                event.getTo().getY() - event.getFrom().getY(),
                event.getTo().getZ() - event.getFrom().getZ()
        );

        World world = player.getWorld();
        int minX = (int) Math.floor(movedBox.getMinX());
        int maxX = (int) Math.floor(movedBox.getMaxX());
        int minY = (int) Math.floor(movedBox.getMinY());
        int maxY = (int) Math.floor(movedBox.getMaxY());
        int minZ = (int) Math.floor(movedBox.getMinZ());
        int maxZ = (int) Math.floor(movedBox.getMaxZ());

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (world.getBlockAt(x, y, z).getType() == material) return true;
                }
            }
        }

        return false;
    }
}
