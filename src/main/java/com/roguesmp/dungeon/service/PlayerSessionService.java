package com.roguesmp.dungeon.service;

import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.dungeon.actor.scoreboard.DungeonTabLayout;
import com.roguesmp.dungeon.data.runtime.DungeonInstance;
import com.roguesmp.dungeon_party.Party;
import com.roguesmp.dungeon.data.runtime.session.PlayerStatus;
import com.roguesmp.dungeon.manager.DungeonManager;
import com.roguesmp.dungeon.manager.InstanceManager;
import com.roguesmp.dungeon_party.PartyManager;
import com.roguesmp.RogueSmpCore;
import com.roguesmp.utils.Utils;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class PlayerSessionService {

    private final PartyManager partyManager;
    private final InstanceManager instanceManager;
    private final DungeonManager dungeonManager;
    private final PlayerDeathService playerDeathService;
    private final InstanceWorldService worldService;

    public PlayerSessionService(PartyManager partyManager, InstanceManager instanceManager, DungeonManager dungeonManager,
                                PlayerDeathService playerDeathService, InstanceWorldService worldService) {
        this.partyManager = partyManager;
        this.instanceManager = instanceManager;
        this.dungeonManager = dungeonManager;
        this.playerDeathService = playerDeathService;
        this.worldService = worldService;
    }

    public void handlePlayerReconnect(Player player) {
        Party party = partyManager.getPartyByPlayer(player);
        if (party == null) return;
        String iid = party.getInstanceId();
        if (iid == null || iid.isBlank()) return;

        DungeonInstance instance = instanceManager.get(iid);
        if (instance == null) { party.setInstanceId(""); return; }

        PlayerStatus status = instance.getDungeonPlayers().getPlayers().get(player.getUniqueId().toString());
        if (status == null) return;

        if (status.getStatus() == PlayerStatus.Status.OUT) return;

        worldService.load(instance)
                .thenAccept(world -> Utils.runLater(() -> rejoin(player, instance, status, world)))
                .exceptionally(error -> {
                    RogueSmpCore.LOGGER.error("Failed to load dungeon world of instance {} for {}", iid, player.getName(), error);
                    return null;
                });
    }

    private void rejoin(Player player, DungeonInstance instance, PlayerStatus status, World world) {
        if (!player.isOnline()) return;

        if (!player.getWorld().equals(world)) {
            Location target = status.getCheckPoint() != null ? status.getCheckPoint().toBukkit() : worldService.origin(world);
            player.teleportAsync(target);
        }

        SmpPlayer smpPlayer = PlayerManager.getInstance().getSmpPlayer(player);
        if (smpPlayer != null) {
            smpPlayer.getTabLayouts().claim(DungeonTabLayout.claim(dungeonManager.get(instance.getSession().getDungeonId()), instance));
        }

        switch (status.getStatus()) {
            case DISCONNECT -> {
                status.setStatus(PlayerStatus.Status.PLAYING);
                player.setGameMode(GameMode.SURVIVAL);
            }
            case DEAD -> playerDeathService.restoreDeadState(player, status);
            case DEAD_DISCONNECT -> playerDeathService.restoreDeadState(player, status);
        }
    }

    public void handlePlayerDisconnect(Player player) {
        Party party = partyManager.getPartyByPlayer(player);
        if (party == null) return;
        String iid = party.getInstanceId();
        if (iid == null || iid.isBlank()) return;

        DungeonInstance instance = instanceManager.get(iid);
        if (instance == null) return;

        PlayerStatus status = instance.getDungeonPlayers().getPlayers().get(player.getUniqueId().toString());
        if (status == null) return;

        switch (status.getStatus()) {
            case DEAD -> {
                status.setStatus(PlayerStatus.Status.DEAD_DISCONNECT);
                player.setGameMode(GameMode.SURVIVAL);
            }
            case PLAYING -> status.setStatus(PlayerStatus.Status.DISCONNECT);
        }
    }
}
