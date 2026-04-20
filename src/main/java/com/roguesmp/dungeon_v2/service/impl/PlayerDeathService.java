package com.roguesmp.dungeon_v2.service.impl;

import com.roguesmp.dungeon_v2.data.runtime.DungeonInstance;
import com.roguesmp.dungeon_v2.data.runtime.Party;
import com.roguesmp.dungeon_v2.data.runtime.session.DungeonProgress;
import com.roguesmp.dungeon_v2.data.runtime.session.PlayerStatus;
import com.roguesmp.dungeon_v2.helper.SerializableLocation;
import com.roguesmp.dungeon_v2.manager.InstanceManager;
import com.roguesmp.dungeon_v2.presentation.presenter.DungeonPresenter;
import com.roguesmp.dungeon_v2.service.IPartyService;
import com.roguesmp.dungeon_v2.service.IReviveService;
import com.roguesmp.dungeon_v2.utils.DungeonEcho;
import com.roguesmp.dungeon_v2.utils.Teleporter;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

public class PlayerDeathService {

    private final IPartyService partyService;
    private final InstanceManager instanceManager;
    private final IReviveService reviveService;
    private final DungeonPresenter presenter;

    public PlayerDeathService(IPartyService partyService, InstanceManager instanceManager,
                              IReviveService reviveService, DungeonPresenter presenter) {
        this.partyService = partyService;
        this.instanceManager = instanceManager;
        this.reviveService = reviveService;
        this.presenter = presenter;
    }

    public void handlePlayerDead(Player player) {
        DungeonInstance instance = resolveInstance(player);
        if (instance == null) return;

        PlayerStatus status = resolveStatus(instance, player);
        if (status == null) return;

        status.setCheckPoint(SerializableLocation.from(player.getLocation()));
        status.setStatus(PlayerStatus.Status.DEAD);
        status.upDead();
        player.setGameMode(GameMode.SPECTATOR);
        reviveService.registerPlayerDead(player);

        if(isAllTeamDead(instance)){
            DungeonEcho.error(player, "You die! Wait for your teammates complete the room, you will be revived");
            instance.getProgress().setStatus(DungeonProgress.Status.FAILED);
            /**/
            return;
        }

        DungeonEcho.error(player, "You die! Wait for your teammates complete the room, you will be revived");
        presenter.onPlayerDead(player, player.getLocation());
    }

    /** Dùng lại trong PlayerSessionService khi reconnect với trạng thái DEAD */
    public void restoreDeadState(Player player, PlayerStatus status) {
        status.setStatus(PlayerStatus.Status.DEAD);
        reviveService.registerPlayerDead(player);
        if (status.getCheckPoint() != null) Teleporter.teleport(player, status.getCheckPoint());
        player.setGameMode(GameMode.SPECTATOR);
    }

    private DungeonInstance resolveInstance(Player player) {
        Party party = partyService.getPartyByPlayer(player);
        if (party == null || party.getInstanceId() == null || party.getInstanceId().isBlank()) return null;
        return instanceManager.get(party.getInstanceId());
    }

    private PlayerStatus resolveStatus(DungeonInstance instance, Player player) {
        return instance.getDungeonPlayers().getPlayers().get(player.getUniqueId().toString());
    }

    private boolean isAllTeamDead(DungeonInstance instance) {
        for (var playerStatus : instance.getDungeonPlayers().getPlayers().values()) {
            PlayerStatus.Status status = playerStatus.getStatus();

            if (status != PlayerStatus.Status.DEAD &&
                    status != PlayerStatus.Status.DEAD_DISCONNECT) {
                return false;
            }
        }

        return true;
    }
}
