package com.roguesmp.dungeon.service;

import java.util.Objects;
import com.roguesmp.utils.Utils;
import com.roguesmp.RogueSmpCore;
import org.bukkit.World;
import java.util.UUID;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.dungeon.actor.scoreboard.DungeonTabLayout;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.Component;
import com.roguesmp.dungeon.data.definition.Dungeon;
import com.roguesmp.dungeon.data.runtime.DungeonInstance;
import com.roguesmp.dungeon_party.Party;
import com.roguesmp.dungeon.data.runtime.RoomInstance;
import com.roguesmp.dungeon.data.runtime.session.DungeonProgress;
import com.roguesmp.dungeon.data.runtime.session.PlayerStatus;
import com.roguesmp.dungeon.helper.SerializableLocation;
import com.roguesmp.dungeon.manager.DungeonManager;
import com.roguesmp.dungeon.manager.InstanceManager;
import com.roguesmp.dungeon_spawner.manager.SpawnerInstanceManager;
import com.roguesmp.dungeon.presentation.presenter.DungeonPresenter;
import com.roguesmp.dungeon_party.PartyManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.*;

import java.util.Map;

public class DungeonLifecycleService {

    private final InstanceService instanceService;
    private final InstanceManager instanceManager;
    private final PartyManager partyManager;
    private final InstanceWorldService worldService;
    private final DungeonManager dungeonManager;
    private final DungeonPresenter presenter;
    private final RoomRuntimeService roomRuntimeService;
    private final ReviveService reviveService;

    public DungeonLifecycleService(InstanceService instanceService, InstanceManager instanceManager, PartyManager partyManager, InstanceWorldService worldService, DungeonManager dungeonManager, SpawnerInstanceManager spawnerInstanceManager, DungeonPresenter presenter, RoomRuntimeService roomRuntimeService, ReviveService reviveService) {
        this.instanceService = instanceService;
        this.instanceManager = instanceManager;
        this.partyManager = partyManager;
        this.worldService = worldService;
        this.dungeonManager = dungeonManager;
        this.presenter = presenter;
        this.roomRuntimeService = roomRuntimeService;
        this.reviveService = reviveService;
    }


    public void onStartDungeon(String did, Player player) {
        Party party = partyManager.getPartyByPlayer(player);
        if (party == null) return;
        if (party.getInstanceId() != null && !party.getInstanceId().isBlank()) return;
        Dungeon dungeon = dungeonManager.get(did);
        if (dungeon == null) return;

        UUID sessionId = UUID.randomUUID();
        party.setInstanceId(sessionId.toString());

        worldService.create(sessionId).thenAccept(world -> Utils.runLater(() -> enterDungeon(dungeon, party, sessionId.toString(), world)))
                .exceptionally(error -> {
                    RogueSmpCore.LOGGER.error("Failed to create dungeon world for party {}", party.getPartyId(), error);
                    Utils.runLater(() -> {
                        party.setInstanceId(null);
                        player.sendMessage(Component.text("Không thể tạo dungeon, vui lòng thử lại.", NamedTextColor.RED));
                    });
                    return null;
                });
    }

    private void enterDungeon(Dungeon dungeon, Party party, String sessionId, World world) {
        DungeonInstance instance = instanceService.createDungeonInstance(dungeon, party, sessionId, world);
        instanceManager.add(instance);
        instanceService.startDungeonInstance(instance, world);

        roomRuntimeService.bindCurrentRoomRuntime(instance, party);
        RoomInstance startRoom = instance.getProgress().getCurrentRoom();
        roomRuntimeService.triggerRoomStart(startRoom);
        if (startRoom != null && startRoom.isCompleted()) {
            roomRuntimeService.triggerRoomEnd(startRoom);
        }

        party.getMembers().stream().map(id -> Bukkit.getPlayer(UUID.fromString(id))).filter(Objects::nonNull).forEach(member -> member.teleport(worldService.origin(world)));
        partyManager.getOnlineMembers(party).forEach(p -> {
            SmpPlayer smpPlayer = PlayerManager.getInstance().getSmpPlayer(p);
            if (smpPlayer != null) smpPlayer.getTabLayouts().claim(DungeonTabLayout.claim(dungeon, instance));
            presenter.onEnterDungeon(p, dungeon.getName());
        });
    }

    public void onLeaveDungeon(Player player) {
        Party party = partyManager.getPartyByPlayer(player);
        if (party == null) { player.sendMessage(Component.text("Is you still in party?", NamedTextColor.YELLOW)); return; }
        DungeonInstance instance = instanceManager.get(party.getInstanceId());
        if (instance == null) { player.sendMessage(Component.text("Is your party still in dungeon?", NamedTextColor.YELLOW)); return; }

        Map<String, PlayerStatus> players = instance.getDungeonPlayers().getPlayers();
        players.remove(player.getUniqueId().toString());

        if (instance.getProgress().getStatus() == DungeonProgress.Status.IN_PROGRESS) {
            partyManager.forceKick(player);
            instance.getDungeonPlayers().setPlayerStatus(
                    player.getUniqueId().toString(), PlayerStatus.Status.OUT);
            player.sendMessage(Component.text("Bạn đã rời khỏi dungeon khi đang chơi, đồng thời cũng sẽ rời khỏi party!", NamedTextColor.GRAY));
        }

        player.teleport(new Location(Bukkit.getWorld("building"), 0, 0, 0));
        releaseTabLayout(player);

        if (players.isEmpty()) {
            instanceService.removeDungeonInstance(instance);
            party.setInstanceId("");
        }
    }

    public void onEndUpDungeon(DungeonInstance instance) {
        instance.getDungeonPlayers().getPlayers().forEach((uuid, playerStatus) -> {
            Player player = Bukkit.getPlayer(UUID.fromString(uuid));
            if (player == null) return;
            if (instance.getProgress().getStatus() == DungeonProgress.Status.FAILED) {
                player.sendMessage(Component.text("The dungeon has collapsed! You barely escaped...", NamedTextColor.YELLOW));
            } else {
                player.sendMessage(Component.text("The dungeon has been conquered! Glory to your party!", NamedTextColor.GREEN));
            }
            releaseTabLayout(player);
            player.teleport(new Location(Bukkit.getWorld("building"), 0, 0, 0));
        });

        instanceService.removeDungeonInstance(instance);
        instanceManager.remove(instance.getSession().getSessionId());
        Party party = partyManager.getPartyById(instance.getSession().getPartyId());
        party.setInstanceId("");
        party.getMembers().forEach(s -> {
            Player p = Bukkit.getPlayer(UUID.fromString(s));
            if(p != null) reviveService.forceRemoveDeadEntry(p.getUniqueId());
        });
    }

    public void onDungeonTimeExpired(DungeonInstance instance) {
        if (instance == null || instance.getProgress() == null) return;
        DungeonProgress.Status status = instance.getProgress().getStatus();
        if (status == DungeonProgress.Status.COMPLETED) return;
        instance.getProgress().setStatus(DungeonProgress.Status.FAILED);
        onEndUpDungeon(instance);
    }

    public void onDungeonFinish(DungeonInstance instance) {
        roomRuntimeService.clearEntitiesInDungeon(instance);
        long newEndTime = System.currentTimeMillis() + 10 * 1000L;
        instance.getTimer().setEndTime(newEndTime);
        instance.getDungeonPlayers().getPlayers().forEach((s, playerStatus) -> {
            Player p = Bukkit.getPlayer(UUID.fromString(s));
            if(p == null) return;
            p.sendMessage(Component.text("Your entire party has fallen — the dungeon claims victory. " +
                    "It will collapse in 10 seconds!", NamedTextColor.RED));
        });
    }



    private void releaseTabLayout(Player player) {
        SmpPlayer smpPlayer = PlayerManager.getInstance().getSmpPlayer(player);
        if (smpPlayer != null) smpPlayer.getTabLayouts().release(DungeonTabLayout.KEY);
    }
}
