package com.roguesmp.dungeon.service.impl;

import com.roguesmp.dungeon.instance.DungeonInstance;
import com.roguesmp.dungeon.instance.NodeInstance;
import com.roguesmp.dungeon.instance.RoomInstance;
import com.roguesmp.dungeon.presentation.presenter.DungeonPresenter;
import com.roguesmp.dungeon.service.IDungeonFlowService;
import com.roguesmp.dungeon.service.IInstanceService;
import com.roguesmp.dungeon.service.IPartyService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Objects;

public class DungeonFlowService implements IDungeonFlowService {

    private final IPartyService partyService;
    private final DungeonPresenter presenter;
    private final IInstanceService instanceService;

    public DungeonFlowService(IPartyService partyService,
                              DungeonPresenter presenter,
                              IInstanceService instanceService) {
        this.partyService = partyService;
        this.presenter = presenter;
        this.instanceService = instanceService;
    }

    @Override
    public void onRoomCompleted(DungeonInstance dungeon, RoomInstance room) {

        NodeInstance node = room.getNode();

        if (Objects.equals(node.getNodeKey(), "end")) {
            completeDungeon(dungeon);
            return;
        }
    }

    private void completeDungeon(DungeonInstance dungeon) {
        if (dungeon.isCompleted()) return;

        dungeon.setCompleted(true);

        partyService.getPartyById(dungeon.getParty()).ifPresent(p ->
                p.getMembers().forEach(id -> {
                    Player player = Bukkit.getPlayer(id);
                    if (player != null) {
                        presenter.onCompleteDungeon(player);
                    }
                })
        );
        //TO DO
        //chỉnh thời gian lên 10p cuối để player nhận thưởng ở cổng cuối cùng
    }

    private void finishDungeonProcess(DungeonInstance dungeon){
        partyService.getPartyById(dungeon.getParty()).ifPresent(p ->
                p.getMembers().forEach(id -> {
                    Player player = Bukkit.getPlayer(id);
                    if (player != null) {
                        //tp ve the gioi cu
                    }
                })
        );

        instanceService.endDungeonInstance(dungeon.getParty());
    }
}
