package com.roguesmp.dungeon.service.impl;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.audience.Audience;
import com.roguesmp.dungeon.data.definition.objective.BaseObjective;
import com.roguesmp.dungeon.data.runtime.DungeonInstance;
import com.roguesmp.dungeon.data.runtime.Party;
import com.roguesmp.dungeon.data.runtime.RoomInstance;
import com.roguesmp.dungeon.data.runtime.session.DungeonProgress;
import com.roguesmp.dungeon.data.runtime.session.PlayerStatus;
import com.roguesmp.dungeon.presentation.presenter.DungeonPresenter;
import com.roguesmp.dungeon.service.IPartyService;
import com.roguesmp.dungeon.service.IReviveService;
import com.roguesmp.dungeon.service.IRoomService;
import com.roguesmp.dungeon.utils.UuidUtil;
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
        Audience.audience(partyService.getOnlineMembers(party)).sendMessage(Component.text("Room clear!", NamedTextColor.GREEN));
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
        Audience.audience(partyService.getOnlineMembers(party)).sendMessage(Component.text("Boss defeated!!!", NamedTextColor.GREEN));
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
                player.sendMessage(Component.text("Fate grants you another chance!", NamedTextColor.GREEN));
            }
        });
    }
}