package com.roguesmp.dungeon.service.impl;

import com.roguesmp.dungeon.data.runtime.DungeonInstance;
import com.roguesmp.dungeon.data.runtime.Party;
import com.roguesmp.dungeon.data.runtime.RoomInstance;
import com.roguesmp.dungeon.manager.InstanceManager;
import com.roguesmp.dungeon.service.IPartyService;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

public class PlayerTeleportService {

    private final IPartyService partyService;
    private final InstanceManager instanceManager;

    public PlayerTeleportService(IPartyService partyService, InstanceManager instanceManager) {
        this.partyService = partyService;
        this.instanceManager = instanceManager;
    }

    public boolean onPlayerUsingTeleportItem(Player player, Location to){
        Party party = partyService.getPartyByPlayer(player);
        if(party == null) return false;
        DungeonInstance instance = instanceManager.get(party.getInstanceId());
        if(instance == null) return false;
        RoomInstance roomInstance = instance.getProgress().getCurrentRoom();
        BoundingBox roomBound = roomInstance.getBounds().toBukkit();
        return !roomBound.contains(to.toVector());
    }
}
