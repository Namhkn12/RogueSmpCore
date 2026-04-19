package com.roguesmp.dungeon_v2.service.impl;

import com.roguesmp.dungeon_v2.data.runtime.DeadEntry;
import com.roguesmp.dungeon_v2.data.runtime.DungeonInstance;
import com.roguesmp.dungeon_v2.data.runtime.Party;
import com.roguesmp.dungeon_v2.data.runtime.session.DungeonPlayer;
import com.roguesmp.dungeon_v2.data.runtime.session.PlayerStatus;
import com.roguesmp.dungeon_v2.manager.InstanceManager;
import com.roguesmp.dungeon_v2.manager.ReviveManager;
import com.roguesmp.dungeon_v2.presentation.presenter.DungeonPresenter;
import com.roguesmp.dungeon_v2.service.IPartyService;
import com.roguesmp.dungeon_v2.service.IReviveService;
import com.roguesmp.dungeon_v2.utils.DungeonEcho;
import com.roguesmp.dungeon_v2.utils.Teleporter;
import com.roguesmp.dungeon_v2.utils.UuidUtil;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.boss.BarColor;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class ReviveService implements IReviveService {

    private static final double REVIVE_RADIUS = 3.0;
    private static final double REVIVE_RADIUS_SQUARED = REVIVE_RADIUS * REVIVE_RADIUS;
    private static final int PROGRESS_PER_TICK = 20;  // +1s mỗi lần check

    private final ReviveManager reviveManager;
    private final IPartyService partyService;
    private final InstanceManager instanceManager;
    private final DungeonPresenter dungeonPresenter;

    public ReviveService(ReviveManager reviveManager, IPartyService partyService, InstanceManager instanceManager, DungeonPresenter dungeonPresenter) {
        this.reviveManager = reviveManager;
        this.partyService  = partyService;
        this.instanceManager = instanceManager;
        this.dungeonPresenter = dungeonPresenter;

        reviveManager.startTask(this::processTick);
    }


    @Override
    public void registerPlayerDead(Player player) {
        Location deathLocation = resolveDeadLocation(player);
        reviveManager.addEntry(player, deathLocation);
        reviveManager.createBossBar(player.getUniqueId(), player);
    }


    @Override
    public void processTick() {
        List<DeadEntry> revives = reviveManager.getAllEntries();
        revives.forEach(deadEntry -> {
            Player player = Bukkit.getPlayer(deadEntry.getDeadID());
            if(player == null) return;

            Party party = partyService.getPartyByPlayer(player);
            if (party == null) return;

            String instanceId = party.getInstanceId();
            if (instanceId == null || instanceId.isBlank()) return;

            DungeonInstance dungeonInstance = instanceManager.get(instanceId);
            if (dungeonInstance == null) return;

            DungeonPlayer dungeonPlayer = dungeonInstance.getDungeonPlayers();
            if (dungeonPlayer == null) return;

            Location deadLocation = resolveDeadLocation(dungeonPlayer, player.getUniqueId(), deadEntry);
            if (deadLocation == null || deadLocation.getWorld() == null) return;

            List<Player> rescuers = findRescuer(dungeonPlayer, player, deadLocation);
            if (rescuers.isEmpty()) {
                deadEntry.decayProgress();
                reviveManager.clearRescuersFromBossBar(player.getUniqueId(), player);
            } else {
                deadEntry.incrementProgress(PROGRESS_PER_TICK);
                syncViewers(deadEntry, rescuers, player);
            }
            if(deadEntry.isReviveComplete()){
                revivePlayer(deadEntry.getDeadID(), dungeonPlayer);
            }
        });
    }


    @Override
    public void revivePlayer(UUID deadUUID, DungeonPlayer dungeonPlayer) {
        DeadEntry entry = reviveManager.getEntry(deadUUID).orElse(null);
        if(entry == null) return;
        reviveManager.removeEntry(deadUUID);
        reviveManager.removeBossBar(deadUUID);

        PlayerStatus status = dungeonPlayer.getPlayers().get(UuidUtil.toStringOrNull(deadUUID));
        if (status == null) return;
        Player player = Bukkit.getPlayer(deadUUID);
        if(player != null){
            status.setStatus(PlayerStatus.Status.PLAYING);
            if (status.getCheckPoint() != null) {
                Teleporter.teleport(player, status.getCheckPoint());
            }
            player.setGameMode(GameMode.SURVIVAL);
            player.setHealth(5);
            dungeonPresenter.onPlayerRevive(player, player.getLocation());
            DungeonEcho.success(player, "Fate grants you another chance!");
        }
        else status.setStatus(PlayerStatus.Status.DISCONNECT);
    }

    @Override
    public void forceRemoveDeadEntry(UUID uuid) {
        reviveManager.removeEntry(uuid);
        reviveManager.removeBossBar(uuid);
    }


    @Override
    public void reviveAll() {
        // 1. Lấy snapshot UUID từ manager.getAllEntries()
        // 2. Gọi revivePlayer(uuid) cho từng UUID
    }


    @Override
    public boolean isDead(UUID uuid) {
        return reviveManager.hasEntry(uuid);
    }

    private List<Player> findRescuer(DungeonPlayer teammates, Player dead, Location deadLocation) {
        if (deadLocation == null || deadLocation.getWorld() == null) return List.of();

        Party party = partyService.getPartyByPlayer(dead);
        if (party == null) return List.of();

        return partyService.getOnlineMembers(party).stream()
                .filter(player -> !player.equals(dead))
                .filter(player -> {
                    PlayerStatus s = teammates.getPlayers().get(player.getUniqueId().toString());
                    return s != null && s.getStatus().equals(PlayerStatus.Status.PLAYING);
                })
                .filter(player -> player.getWorld().equals(deadLocation.getWorld()))
                .filter(Player::isSneaking)
                .filter(player -> player.getLocation().distanceSquared(deadLocation) <= REVIVE_RADIUS_SQUARED)
                .collect(Collectors.toList());
    }

    private void syncViewers(DeadEntry entry, List<Player> rescuers, Player dead) {
        UUID deadUUID = dead.getUniqueId();

        Set<Player> currentViewers = reviveManager.getBossBar(deadUUID)
                .map(bar -> new HashSet<>(bar.getPlayers()))
                .orElse(new HashSet<>());

        Set<Player> expectedRescuers = new HashSet<>(rescuers);

        expectedRescuers.forEach(rescuer -> {
            if (!currentViewers.contains(rescuer)) {
                reviveManager.addViewerToBossBar(deadUUID, rescuer);
            }
        });

        currentViewers.forEach(viewer -> {
            if (!viewer.equals(dead) && !expectedRescuers.contains(viewer)) {
                reviveManager.removeRescuerFromBossBar(deadUUID, viewer);
            }
        });

        double progress = entry.getProgressPercent();
        String title = "Reviving " + dead.getName() + " — " + rescuers.size() + " rescuer(s)";
        reviveManager.updateBossBar(deadUUID, title, progress, BarColor.GREEN);
    }

    private Location resolveDeadLocation(Player deadPlayer) {
        if (deadPlayer == null) return null;

        Party party = partyService.getPartyByPlayer(deadPlayer);
        if (party == null) return deadPlayer.getLocation();

        String instanceId = party.getInstanceId();
        if (instanceId == null || instanceId.isBlank()) return deadPlayer.getLocation();

        DungeonInstance instance = instanceManager.get(instanceId);
        if (instance == null || instance.getDungeonPlayers() == null) return deadPlayer.getLocation();

        PlayerStatus status = instance.getDungeonPlayers().getPlayers().get(deadPlayer.getUniqueId().toString());
        if (status == null || status.getCheckPoint() == null) return deadPlayer.getLocation();

        Location checkpoint = status.getCheckPoint().toBukkit();
        if (checkpoint == null || checkpoint.getWorld() == null) return deadPlayer.getLocation();
        return checkpoint;
    }

    private Location resolveDeadLocation(DungeonPlayer dungeonPlayer, UUID deadUUID, DeadEntry deadEntry) {
        if (dungeonPlayer != null && deadUUID != null) {
            PlayerStatus status = dungeonPlayer.getPlayers().get(deadUUID.toString());
            if (status != null && status.getCheckPoint() != null) {
                Location checkpoint = status.getCheckPoint().toBukkit();
                if (checkpoint != null && checkpoint.getWorld() != null) {
                    return checkpoint;
                }
            }
        }
        return deadEntry.getDeathLocation();
    }
}
