package com.roguesmp.dungeon.actor.listener;

import com.roguesmp.dungeon.controller.DungeonFlowController;
import com.roguesmp.dungeon.data.runtime.Party;
import com.roguesmp.dungeon.service.IPartyService;
import com.roguesmp.dungeon.utils.filterchain.EventFilter;
import com.roguesmp.dungeon.utils.filterchain.FilterChain;
import com.roguesmp.dungeon.utils.filterchain.impl.InteractFilters;
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
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.Location;
import org.bukkit.util.BoundingBox;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DoorInteractListener implements Listener {

    private final DungeonFlowController dungeonFlowController;
    private final IPartyService partyService;
    private final Map<UUID, String> treasureEnteredByInstance = new ConcurrentHashMap<>();
    private final Set<UUID> insideGateway = Collections.newSetFromMap(new ConcurrentHashMap<>());


    public DoorInteractListener(DungeonFlowController dungeonFlowController, IPartyService partyService) {
        this.dungeonFlowController = dungeonFlowController;
        this.partyService = partyService;
    }

    @EventHandler
    public void onPlayerInteractDoor(PlayerInteractEvent event) {
        if(!VAULT_FILTER.test(event)) return;
        event.setCancelled(true);

        Block block = event.getClickedBlock();
        if(block == null) return;
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

        UUID playerId = player.getUniqueId();
        boolean toInsideGateway = isOverlappingBlockType(event, event.getTo(), Material.END_GATEWAY);

        if (toInsideGateway) {
            if (insideGateway.contains(playerId)) return;
            insideGateway.add(playerId);
        } else {
            insideGateway.remove(playerId);
            return;
        }

        String instanceId = resolveInstanceId(player);
        if (instanceId == null || instanceId.isBlank()) return;

        String enteredInstance = treasureEnteredByInstance.get(playerId);
        if (instanceId.equals(enteredInstance)) return;

        treasureEnteredByInstance.put(playerId, instanceId);
        dungeonFlowController.handleGetIntoTreasurePortal(player);
    }

    @EventHandler
    public void onPlayerLeaveRewardRoom(PlayerMoveEvent event) {
        if (!event.hasChangedBlock()) return;

        Player player = event.getPlayer();
        String worldName = player.getWorld().getName();
        if (!worldName.startsWith("dungeon_")) return;

        UUID playerId = player.getUniqueId();
        boolean toInsideGateway = isOverlappingBlockType(event, event.getTo(), Material.NETHER_PORTAL);

        if (toInsideGateway) {
            if (insideGateway.contains(playerId)) return;
            insideGateway.add(playerId);
        } else {
            insideGateway.remove(playerId);
            return;
        }

        dungeonFlowController.handleLeaveDungeon(player);
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
    public void onPlayerQuit(PlayerQuitEvent event) {
    }

    private static final EventFilter<PlayerInteractEvent> VAULT_FILTER =
            FilterChain.of(PlayerInteractEvent.class)
                    .require(InteractFilters.rightClickBlock())
                    .require(InteractFilters.mainHand())
                    .require(InteractFilters.hasBlock())
                    .require(InteractFilters.blockState(Vault.class))
                    .require(InteractFilters.clickedBlockInWorld("dungeon_"))
                    .build();

    private String resolveInstanceId(Player player) {
        if (player == null) return null;
        Party party = partyService.getPartyByPlayer(player);
        if(party == null) return null;
        return party.getInstanceId();
    }

    private boolean isOverlappingBlockType(PlayerMoveEvent event, Location sampleLoc, Material material) {
        if (sampleLoc == null) return false;

        Player player = event.getPlayer();
        BoundingBox movedBox = player.getBoundingBox().clone().shift(
                sampleLoc.getX() - event.getFrom().getX(),
                sampleLoc.getY() - event.getFrom().getY(),
                sampleLoc.getZ() - event.getFrom().getZ()
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
