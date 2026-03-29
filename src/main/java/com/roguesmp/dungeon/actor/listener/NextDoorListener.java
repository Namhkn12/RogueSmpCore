package com.roguesmp.dungeon.actor.listener;

import com.roguesmp.dungeon.actor.ui.NextRoomGui;
import com.roguesmp.dungeon.controller.BuildingController;
import com.roguesmp.dungeon.controller.DungeonController;
import com.roguesmp.dungeon.controller.PartyController;
import com.roguesmp.dungeon.data.Party;
import com.roguesmp.dungeon.instance.DungeonInstance;
import com.roguesmp.dungeon.utils.NameSpaceKeys;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.block.Vault;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class NextDoorListener implements Listener {

    private final DungeonController dungeonController;
    private final PartyController partyController;
    private final BuildingController buildingController;

    public NextDoorListener(DungeonController dungeonController, PartyController partyController, BuildingController buildingController) {
        this.dungeonController = dungeonController;
        this.partyController = partyController;
        this.buildingController = buildingController;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Block block = event.getClickedBlock();
        if (block == null) return;
        if (!(block.getState() instanceof Vault vault)) return;
        if (!block.getWorld().getName().startsWith("dungeon_")) return;
        event.setCancelled(true);

        if (!vault.getRewardedPlayers().isEmpty()) return;

        Player player = event.getPlayer();

        var partyResponse = partyController.getPartyByPlayer(player);
        if (partyResponse == null || partyResponse.getData() == null) {
            player.sendMessage("Bạn không có party!");
            return;
        }
        Party party = partyResponse.getData();

        var instanceResponse = dungeonController.getInstanceByParty(party.getPartyId());
        if (instanceResponse == null || instanceResponse.getData() == null) {
            player.sendMessage("Party bạn không có dungeon!");
            return;
        }
        DungeonInstance instance = instanceResponse.getData();
        if (!instance.getActiveRoom().isCompleted()) return;

        if(instance.isCompleted()){
            Location doorLoc = block.getLocation();
            World world = block.getWorld();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    world.getBlockAt(doorLoc.clone().add(dx, dy, 0)).setType(Material.NETHER_PORTAL);
                }
            }
            return;
        }

        vault.addRewardedPlayer(player.getUniqueId());
        vault.update();

        new NextRoomGui(instance, dungeonController, buildingController, partyController, block)
                .showInventory(player);
    }

    @EventHandler
    public void onPlayerEnterEndGateway(PlayerTeleportEvent e) {
        if (e.getCause() != PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) return;
        if (!e.getPlayer().getWorld().getName().startsWith("dungeon_")) return;

        e.setCancelled(true);

        Player player = e.getPlayer();

        var partyResponse = partyController.getPartyByPlayer(player);
        if (partyResponse == null || partyResponse.getData() == null) return;
        Party party = partyResponse.getData();

        var instanceResponse = dungeonController.getInstanceByParty(party.getPartyId());
        if (instanceResponse == null || instanceResponse.getData() == null) return;
        DungeonInstance instance = instanceResponse.getData();

        // TODO: xử lý thoát dungeon — teleport về lobby, trao thưởng, cleanup instance...
        Location baseLoc = instance.getRegion().getLocation();
        int offsetY = instance.getRewardRoomCount() * 50;
        Location buildLoc = baseLoc.clone().add(0, offsetY, 0);

        buildingController.buildSchematicById(
                "schemeta_20260329202750",
                buildLoc
        );

        player.teleport(buildLoc);
        player.sendMessage("§aBạn đã hoàn thành dungeon!");
    }

//    @EventHandler
//    public void onPlayerInteractWithEndDoor(PlayerInteractEvent e) {
//        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
//        if (e.getHand() != EquipmentSlot.HAND) return;
//
//        Block block = e.getClickedBlock();
//        if (block == null) return;
//
//        if (block.getType() != Material.LODESTONE) return;
//
//        if (!block.getWorld().getName().startsWith("dungeon_")) return;
//
//        if (!(block.getState() instanceof TileState ts)) {
//            return;
//        }
//
//        e.getPlayer().sendMessage("Has END_DOOR_KEY: " + ts.getPersistentDataContainer().has(NameSpaceKeys.END_DOOR_KEY, PersistentDataType.STRING));
//
//        if (!ts.getPersistentDataContainer().has(NameSpaceKeys.END_DOOR_KEY, PersistentDataType.STRING)) return;
//
//        e.setCancelled(true);
//        spawnEndGateway(block, e.getPlayer());
//    }
//
//    @EventHandler
//    public void onBlockPlace(BlockPlaceEvent e) {
//        ItemStack inHand = e.getItemInHand();
//        if (inHand.getItemMeta() == null) return;
//
//        PersistentDataContainer itemPdc = inHand.getItemMeta().getPersistentDataContainer();
//        Block placed = e.getBlockPlaced();
//        if (!(placed.getState() instanceof TileState ts)) return;
//        PersistentDataContainer blockPdc = ts.getPersistentDataContainer();
//
//        if (itemPdc.has(NameSpaceKeys.NEXT_DOOR_KEY, PersistentDataType.STRING)) {
//            blockPdc.set(NameSpaceKeys.NEXT_DOOR_KEY, PersistentDataType.STRING,
//                    itemPdc.get(NameSpaceKeys.NEXT_DOOR_KEY, PersistentDataType.STRING));
//            ts.update();
//
//        } else if (itemPdc.has(NameSpaceKeys.END_DOOR_KEY, PersistentDataType.STRING)) {
//            blockPdc.set(NameSpaceKeys.END_DOOR_KEY, PersistentDataType.STRING,
//                    itemPdc.get(NameSpaceKeys.END_DOOR_KEY, PersistentDataType.STRING));
//            ts.update();
//        }
//    }
//
//    @EventHandler
//    public void onBlockBreak(BlockBreakEvent e) {
//        Block block = e.getBlock();
//        if (!(block.getState() instanceof TileState ts)) return;
//
//        PersistentDataContainer pdc = ts.getPersistentDataContainer();
//        if (pdc.has(NameSpaceKeys.NEXT_DOOR_KEY, PersistentDataType.STRING)
//                || pdc.has(NameSpaceKeys.END_DOOR_KEY, PersistentDataType.STRING)) {
//            e.setCancelled(true);
//            e.getPlayer().sendMessage("§cKhông thể phá block này!");
//        }
//    }
//
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
}