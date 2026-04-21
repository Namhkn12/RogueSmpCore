package com.roguesmp.dungeon.controller;

import com.roguesmp.dungeon.service.impl.*;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class PlayerActionController {

    private final ObjectiveDispatchService objectiveDispatchService;
    private final PlayerDeathService playerDeathService;
    private final SpectatorBoundaryService spectatorBoundaryService;
    private final PlayerSessionService playerSessionService;
    private final PlayerTeleportService playerTeleportService;

    public PlayerActionController(ObjectiveDispatchService objectiveDispatchService,
                                  PlayerDeathService playerDeathService,
                                  SpectatorBoundaryService spectatorBoundaryService,
                                  PlayerSessionService playerSessionService, PlayerTeleportService playerTeleportService) {
        this.objectiveDispatchService = objectiveDispatchService;
        this.playerDeathService = playerDeathService;
        this.spectatorBoundaryService = spectatorBoundaryService;
        this.playerSessionService = playerSessionService;
        this.playerTeleportService = playerTeleportService;
    }

    public void handlePlayerKillMob(LivingEntity entity, Player player) {
        objectiveDispatchService.handlePlayerKillMob(entity, player);
    }

    public void handlePlayerBreakSpawner(Block spawner, Player player) {
        objectiveDispatchService.handlePlayerBreakSpawner(spawner, player);
    }

    public void handlePlayerCollectItem(ItemStack item, Player player) {
        objectiveDispatchService.handlePlayerCollectItem(item, player);
    }

    public void handlePlayerDead(Player player) {
        playerDeathService.handlePlayerDead(player);
    }

    public void handlePlayerMoveInDeadMode(Player player) {
        spectatorBoundaryService.handlePlayerMoveInDeadMode(player);
    }

    public void handlePlayerReconnect(Player player) {
        playerSessionService.handlePlayerReconnect(player);
    }

    public void handlePlayerDisconnect(Player player) {
        playerSessionService.handlePlayerDisconnect(player);
    }

    public boolean handlePlayerTeleport(Player player, Location to){
        return playerTeleportService.onPlayerUsingTeleportItem(player, to);
    }
}
