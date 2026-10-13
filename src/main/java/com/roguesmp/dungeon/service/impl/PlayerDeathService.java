package com.roguesmp.dungeon.service.impl;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.Component;
import com.roguesmp.dungeon.data.runtime.DungeonInstance;
import com.roguesmp.dungeon_party.Party;
import com.roguesmp.dungeon.data.runtime.session.DungeonProgress;
import com.roguesmp.dungeon.data.runtime.session.PlayerStatus;
import com.roguesmp.dungeon.helper.SerializableLocation;
import com.roguesmp.dungeon.manager.InstanceManager;
import com.roguesmp.dungeon.presentation.presenter.DungeonPresenter;
import com.roguesmp.dungeon_party.PartyManager;
import com.roguesmp.dungeon.service.IReviveService;
import com.roguesmp.dungeon.utils.Teleporter;
import com.roguesmp.dungeon.utils.UuidUtil;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class PlayerDeathService {

    private final PartyManager partyManager;
    private final InstanceManager instanceManager;
    private final IReviveService reviveService;
    private final DungeonLifecycleService dungeonLifecycleService;
    private final DungeonPresenter presenter;

    public PlayerDeathService(PartyManager partyManager, InstanceManager instanceManager,
                              IReviveService reviveService, DungeonLifecycleService dungeonLifecycleService, DungeonPresenter presenter) {
        this.partyManager = partyManager;
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
                    p.sendMessage(Component.text("Your teammate " + player.getName() + " died at " + coords, NamedTextColor.RED));
                    p.playSound(p.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);
                }
            }
        });

        player.sendMessage(Component.text("You die! Wait for your teammates complete the room, you will be revived", NamedTextColor.RED));
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
        Party party = partyManager.getPartyByPlayer(player);
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
