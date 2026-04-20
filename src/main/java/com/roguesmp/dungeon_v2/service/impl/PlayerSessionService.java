package com.roguesmp.dungeon_v2.service.impl;

import com.roguesmp.dungeon_v2.data.runtime.DungeonInstance;
import com.roguesmp.dungeon_v2.data.runtime.Party;
import com.roguesmp.dungeon_v2.data.runtime.session.PlayerStatus;
import com.roguesmp.dungeon_v2.manager.DungeonManager;
import com.roguesmp.dungeon_v2.manager.InstanceManager;
import com.roguesmp.dungeon_v2.manager.ScoreBoardManager;
import com.roguesmp.dungeon_v2.service.IPartyService;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

public class PlayerSessionService {

    private final IPartyService partyService;
    private final InstanceManager instanceManager;
    private final ScoreBoardManager scoreBoardManager;
    private final DungeonManager dungeonManager;
    private final PlayerDeathService playerDeathService;

    public PlayerSessionService(IPartyService partyService, InstanceManager instanceManager,
                                ScoreBoardManager scoreBoardManager, DungeonManager dungeonManager,
                                PlayerDeathService playerDeathService) {
        this.partyService = partyService;
        this.instanceManager = instanceManager;
        this.scoreBoardManager = scoreBoardManager;
        this.dungeonManager = dungeonManager;
        this.playerDeathService = playerDeathService;
    }

    public void handlePlayerReconnect(Player player) {
        Party party = partyService.getPartyByPlayer(player);
        if (party == null) return;
        String iid = party.getInstanceId();
        if (iid == null || iid.isBlank()) return;

        DungeonInstance instance = instanceManager.get(iid);
        if (instance == null) { party.setInstanceId(""); return; }

        PlayerStatus status = instance.getDungeonPlayers().getPlayers().get(player.getUniqueId().toString());
        if (status == null) return;

        if (status.getStatus() == PlayerStatus.Status.OUT) return;

        scoreBoardManager.createBoard(player, instance,
                dungeonManager.get(instance.getSession().getDungeonId()));

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
        Party party = partyService.getPartyByPlayer(player);
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
