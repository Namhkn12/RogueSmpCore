package com.roguesmp.dungeon.task;

import com.roguesmp.dungeon.instance.RoomInstance;
import com.roguesmp.dungeon.manager.ScoreBoardManager;
import com.roguesmp.dungeon.objective_.IObjective;
import com.roguesmp.dungeon.service.IInstanceService;
import org.bukkit.scheduler.BukkitRunnable;

public class DungeonTickTask extends BukkitRunnable {

    private final ScoreBoardManager scoreBoardManager;
    private final IInstanceService instanceService; // để lấy tất cả instance đang chạy

    public DungeonTickTask(ScoreBoardManager scoreBoardManager, IInstanceService instanceService) {
        this.scoreBoardManager = scoreBoardManager;
        this.instanceService = instanceService;
    }

    @Override
    public void run() {
        instanceService.getAllInstances().values().forEach(instance -> {
            if (!instance.isPlaying()) return;

            if (instance.isExpired()) {
                instance.setPlaying(false);
                scoreBoardManager.destroyAll(instance);
                return;
            }

            // tick objective của room hiện tại
            RoomInstance room = instance.getActiveRoom();
            if (room != null && room.getObjective() != null) {
                room.getObjective().forEach(IObjective::process);
            }
        });

        // update board một lần cho tất cả
        scoreBoardManager.tickUpdate();
    }
}