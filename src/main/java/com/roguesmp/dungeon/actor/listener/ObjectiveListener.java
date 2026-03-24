package com.roguesmp.dungeon.actor.listener;

import com.roguesmp.dungeon.controller.DungeonController;
import com.roguesmp.dungeon.controller.PartyController;
import com.roguesmp.dungeon.data.Party;
import com.roguesmp.dungeon.instance.DungeonInstance;
import com.roguesmp.dungeon.instance.RoomInstance;
import com.roguesmp.dungeon.objective.obj.SpawnerBreakObj;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import java.util.UUID;

public class ObjectiveListener implements Listener {

    private final DungeonController dungeonController;
    private final PartyController partyController;

    public ObjectiveListener(DungeonController dungeonController, PartyController partyController) {
        this.dungeonController = dungeonController;
        this.partyController = partyController;
    }

    @EventHandler
    public void onSpawnerBreak(BlockBreakEvent e) {
        if (e.getBlock().getType() != Material.SPAWNER) return;

        Player player = e.getPlayer();
        Location loc = e.getBlock().getLocation();

        Party party = partyController.getPartyByPlayer(player).getData();
        DungeonInstance dungeon = dungeonController.getInstanceByParty(party.getPartyId()).getData();
        if (dungeon == null) return;

        RoomInstance room = dungeon.getActiveRoom();
        if (room == null) return;

        if (!room.getRoomBounds().contains(loc.toVector())) return;

        room.getObjective().forEach(obj -> {
            if (obj instanceof SpawnerBreakObj spawnerObj) {
                spawnerObj.onSpawnerBreak(e);
            }
        });
    }
}