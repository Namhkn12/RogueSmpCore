package com.roguesmp.dungeon_v2.service.impl;

import com.roguesmp.dungeon_v2.data.definition.objective.BaseObjective;
import com.roguesmp.dungeon_v2.data.runtime.DungeonInstance;
import com.roguesmp.dungeon_v2.data.runtime.Party;
import com.roguesmp.dungeon_v2.data.runtime.RoomInstance;
import com.roguesmp.dungeon_v2.data.runtime.session.DungeonProgress;
import com.roguesmp.dungeon_v2.data.runtime.session.PlayerStatus;
import com.roguesmp.dungeon_v2.presentation.presenter.DungeonPresenter;
import com.roguesmp.dungeon_v2.service.IPartyService;
import com.roguesmp.dungeon_v2.service.IReviveService;
import com.roguesmp.dungeon_v2.service.IRoomService;
import com.roguesmp.dungeon_v2.utils.DungeonEcho;
import com.roguesmp.dungeon_v2.utils.UuidUtil;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

public class RoomCompletionService {

    private final IPartyService partyService;
    private final IRoomService roomService;
    private final IReviveService reviveService;
    private final DungeonPresenter presenter;
    private final RoomRuntimeService roomRuntimeService;

    public RoomCompletionService(IPartyService partyService, IRoomService roomService,
                                 IReviveService reviveService, DungeonPresenter presenter,
                                 RoomRuntimeService roomRuntimeService) {
        this.partyService = partyService;
        this.roomService = roomService;
        this.reviveService = reviveService;
        this.presenter = presenter;
        this.roomRuntimeService = roomRuntimeService;
    }

    public void checkRoomCompletion(DungeonInstance instance, RoomInstance room, Party party) {
        if (room == null || room.getActiveObjectives() == null) return;

        boolean allDone = room.getActiveObjectives().stream()
                .filter(o -> o instanceof BaseObjective)
                .map(o -> (BaseObjective) o)
                .allMatch(BaseObjective::isCompleted);

        if (!allDone) return;

        room.setCompleted(true);
        roomRuntimeService.triggerRoomEnd(room);
        DungeonEcho.success(partyService.getOnlineMembers(party), "Room clear!");
        reviveAll(instance);

        if (room.getDoor() != null) {
            roomService.openRoomDoor(room.getDoor().toBukkit());
        }
        partyService.getOnlineMembers(party).forEach(presenter::onCompleteRoom);
    }

    public void checkDungeonCompletion(DungeonInstance instance, Party party) {
        instance.getProgress().setStatus(DungeonProgress.Status.BOSS_DEFEATED);
        long newEndTime = System.currentTimeMillis() + 5 * 60 * 1000L;
        instance.getTimer().setEndTime(newEndTime);
        DungeonEcho.success(partyService.getOnlineMembers(party), "Boss defeated!!!");
        reviveAll(instance);
        partyService.getOnlineMembers(party).forEach(presenter::onCompleteDungeon);
    }

    public void reviveAll(DungeonInstance instance) {
        instance.getDungeonPlayers().getPlayers().forEach((uuid, playerStatus) -> {
            if (playerStatus.getStatus() != PlayerStatus.Status.DEAD) return;
            playerStatus.setStatus(PlayerStatus.Status.PLAYING);
            reviveService.forceRemoveDeadEntry(UuidUtil.parseOrNull(uuid));
            Player player = UuidUtil.getPlayerById(uuid);
            if (player != null) {
                player.teleport(playerStatus.getCheckPoint().toBukkit());
                player.setGameMode(GameMode.SURVIVAL);
                presenter.onPlayerRevive(player, playerStatus.getCheckPoint().toBukkit());
                DungeonEcho.success(player, "Fate grants you another chance!");
            }
        });
    }
}