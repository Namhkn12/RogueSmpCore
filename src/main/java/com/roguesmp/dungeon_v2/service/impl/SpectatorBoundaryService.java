package com.roguesmp.dungeon_v2.service.impl;

import com.roguesmp.dungeon_v2.data.runtime.DungeonInstance;
import com.roguesmp.dungeon_v2.data.runtime.Party;
import com.roguesmp.dungeon_v2.manager.InstanceManager;
import com.roguesmp.dungeon_v2.service.IPartyService;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

public class SpectatorBoundaryService {

    private final IPartyService partyService;
    private final InstanceManager instanceManager;

    public SpectatorBoundaryService(IPartyService partyService, InstanceManager instanceManager) {
        this.partyService = partyService;
        this.instanceManager = instanceManager;
    }

    public void handlePlayerMoveInDeadMode(Player player) {
        Party party = partyService.getPartyByPlayer(player);
        if (party == null || party.getInstanceId() == null || party.getInstanceId().isBlank()) return;
        DungeonInstance instance = instanceManager.get(party.getInstanceId());
        if (instance == null) return;

        BoundingBox bounder = instance.getProgress().getCurrentRoom().getBounds().toBukkit();
        double shrink = 3;

        Location loc = player.getLocation();
        double clampedX = clamp(loc.getX(), bounder.getMinX() + shrink, bounder.getMaxX() - shrink);
        double clampedY = clamp(loc.getY(), bounder.getMinY() + shrink, bounder.getMaxY() - shrink);
        double clampedZ = clamp(loc.getZ(), bounder.getMinZ() + shrink, bounder.getMaxZ() - shrink);

        if (clampedX != loc.getX() || clampedY != loc.getY() || clampedZ != loc.getZ()) {
            loc.setX(clampedX);
            loc.setY(clampedY);
            loc.setZ(clampedZ);
            player.teleport(loc);
        }
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}