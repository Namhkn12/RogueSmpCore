package com.roguesmp.dungeon.service.impl;

import com.roguesmp.dungeon.data.runtime.DungeonInstance;
import com.roguesmp.dungeon.data.runtime.Party;
import com.roguesmp.dungeon.data.runtime.session.DungeonProgress;
import com.roguesmp.dungeon.data.runtime.session.PlayerStatus;
import com.roguesmp.dungeon.helper.SerializableLocation;
import com.roguesmp.dungeon.manager.InstanceManager;
import com.roguesmp.dungeon.presentation.presenter.DungeonPresenter;
import com.roguesmp.dungeon.service.IPartyService;
import com.roguesmp.dungeon.service.IReviveService;
import com.roguesmp.dungeon.utils.DungeonEcho;
import com.roguesmp.dungeon.utils.Teleporter;
import com.roguesmp.dungeon.utils.UuidUtil;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class PlayerDeathService {

    private final IPartyService partyService;
    private final InstanceManager instanceManager;
    private final IReviveService reviveService;
    private final DungeonLifecycleService dungeonLifecycleService;
    private final DungeonPresenter presenter;

    public PlayerDeathService(IPartyService partyService, InstanceManager instanceManager,
                              IReviveService reviveService, DungeonLifecycleService dungeonLifecycleService, DungeonPresenter presenter) {
        this.partyService = partyService;
        this.instanceManager = instanceManager;
        this.reviveService = reviveService;
        this.dungeonLifecycleService = dungeonLifecycleService;
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
            instance.getProgress().setStatus(DungeonProgress.Status.FAILED);
            dungeonLifecycleService.onDungeonFinish(instance);
            return;
        }
        instance.getDungeonPlayers().getPlayers().forEach((s, playerStatus) -> {
            if(!UuidUtil.toStringOrNull(player.getUniqueId()).equals(s)){
                Player p = UuidUtil.getPlayerById(s);
                if(p != null){
                    Location loc = player.getLocation();
                    String coords = "[" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + "]";
                    DungeonEcho.error(p, "Your teammate " + player.getName() + " died at " + coords);
                    p.playSound(p.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);
                }
            }
        });

        DungeonEcho.error(player, "You die! Wait for your teammates complete the room, you will be revived");
        player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);
        presenter.onPlayerDead(player, player.getLocation());
    }

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
