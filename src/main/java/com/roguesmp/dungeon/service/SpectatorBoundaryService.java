package com.roguesmp.dungeon.service;

import com.roguesmp.dungeon.data.runtime.DungeonInstance;
import com.roguesmp.dungeon_party.Party;
import com.roguesmp.dungeon.manager.InstanceManager;
import com.roguesmp.dungeon_party.PartyManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

public class SpectatorBoundaryService {

    private final PartyManager partyManager;
    private final InstanceManager instanceManager;

    public SpectatorBoundaryService(PartyManager partyManager, InstanceManager instanceManager) {
        this.partyManager = partyManager;
        this.instanceManager = instanceManager;
    }

    public void handlePlayerMoveInDeadMode(Player player) {
        Party party = partyManager.getPartyByPlayer(player);
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