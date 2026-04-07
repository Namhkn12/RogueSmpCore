package com.roguesmp.dungeon_v2.actor.listener;


import com.roguesmp.dungeon_v2.actor.ui.OpenDoorGui;
import com.roguesmp.dungeon_v2.controller_.DungeonFlowController;
import com.roguesmp.dungeon_v2.manager.InstanceManager;
import com.roguesmp.dungeon_v2.utils.filterchain.EventFilter;
import com.roguesmp.dungeon_v2.utils.filterchain.FilterChain;
import com.roguesmp.dungeon_v2.utils.filterchain.impl.InteractFilters;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Vault;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.List;

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

        Vault vault = (Vault) event.getClickedBlock();
        event.setCancelled(true);

        if (!vault.getRewardedPlayers().isEmpty()) return;

        Player player = event.getPlayer();


        /*Case: end room*/

        /*Case: next room*/
        vault.addRewardedPlayer(player.getUniqueId());
        vault.update();

        new OpenDoorGui(vault, List.of(""), dungeonFlowController);
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

    private void spawnEndGateway(Block doorBlock, Player player) {
        Block center = doorBlock.getRelative(0, 1, 0);
        org.bukkit.util.Vector dir = player.getLocation().getDirection();
        boolean facingZ = Math.abs(dir.getZ()) > Math.abs(dir.getX());

        for (int da = -1; da <= 1; da++) {
            for (int dy = -1; dy <= 1; dy++) {
                Block portalBlock = facingZ
                        ? center.getRelative(da, dy, 0)
                        : center.getRelative(0, dy, da);

                portalBlock.setType(Material.END_GATEWAY);

                if (portalBlock.getState() instanceof org.bukkit.block.EndGateway gateway) {
                    gateway.setExitLocation(portalBlock.getLocation());
                    gateway.setAge(-9223372036854775808L);
                    gateway.update();
                }
            }
        }
    }

    private static final EventFilter<PlayerInteractEvent> VAULT_FILTER =
            FilterChain.of(PlayerInteractEvent.class)
                    .require(InteractFilters.rightClickBlock())
                    .require(InteractFilters.mainHand())
                    .require(InteractFilters.hasBlock())
                    .require(InteractFilters.blockState(Vault.class))
                    .require(InteractFilters.clickedBlockInWorld("dungeon_"))
                    .build();
}