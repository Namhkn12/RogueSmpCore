package com.roguesmp.dungeon.actor.listener;

import com.roguesmp.dungeon.actor.ui.NextRoomGui;
import com.roguesmp.dungeon.controller.BuildingController;
import com.roguesmp.dungeon.controller.DungeonController;
import com.roguesmp.dungeon.controller.PartyController;
import com.roguesmp.dungeon.data.Party;
import com.roguesmp.dungeon.instance.DungeonInstance;
import org.bukkit.block.Block;
import org.bukkit.block.Vault;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.EquipmentSlot;

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
        if(!block.getWorld().getName().startsWith("dungeon_")) return;
        event.setCancelled(true);

        //
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
        if(!instance.getActiveRoom().isCompleted()) return;

        // Đánh dấu vault đang bị dùng
        vault.addRewardedPlayer(player.getUniqueId());
        vault.update();

        new NextRoomGui(instance, dungeonController, buildingController, partyController, block)
                .showInventory(player);
    }
}