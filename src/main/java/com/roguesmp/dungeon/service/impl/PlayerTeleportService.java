package com.roguesmp.dungeon.service.impl;

import com.roguesmp.dungeon.data.runtime.DungeonInstance;
import com.roguesmp.dungeon_party.Party;
import com.roguesmp.dungeon.data.runtime.RoomInstance;
import com.roguesmp.dungeon.manager.InstanceManager;
import com.roguesmp.dungeon_party.PartyManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

public class PlayerTeleportService {

    private final PartyManager partyManager;
    private final InstanceManager instanceManager;

    public PlayerTeleportService(PartyManager partyManager, InstanceManager instanceManager) {
        this.partyManager = partyManager;
        this.instanceManager = instanceManager;
    }

    public boolean onPlayerUsingTeleportItem(Player player, Location to){
        Party party = partyManager.getPartyByPlayer(player);
        if(party == null) return false;
        DungeonInstance instance = instanceManager.get(party.getInstanceId());
        if(instance == null) return false;
        RoomInstance roomInstance = instance.getProgress().getCurrentRoom();
        BoundingBox roomBound = roomInstance.getBounds().toBukkit();
        return !roomBound.contains(to.toVector());
    }
}
