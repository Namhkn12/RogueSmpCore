package com.roguesmp.dungeon_v2.service.impl;

import com.roguesmp.dungeon_v2.data.definition.Dungeon;
import com.roguesmp.dungeon_v2.data.definition.room.Room;
import com.roguesmp.dungeon_v2.data.definition.room.RoomEntry;
import com.roguesmp.dungeon_v2.data.definition.room.RoomPool;
import com.roguesmp.dungeon_v2.data.definition.room.RoomType;
import com.roguesmp.dungeon_v2.data.runtime.DungeonInstance;
import com.roguesmp.dungeon_v2.manager.DungeonManager;
import com.roguesmp.dungeon_v2.manager.RoomManager;
import com.roguesmp.dungeon_v2.service.IDungeonService;
import com.roguesmp.dungeon_v2.utils.Log4Craft_;
import com.roguesmp.dungeon_v2.utils.Razdon;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class DungeonService implements IDungeonService {

    private final RoomManager roomManager;
    private final DungeonManager dungeonManager;
    private final Log4Craft_ logger;

    public DungeonService(RoomManager roomManager, DungeonManager dungeonManager, Log4Craft_ logger) {
        this.roomManager = roomManager;
        this.dungeonManager = dungeonManager;
        this.logger = logger;
    }

    @Override
    public List<String> rollRoomPool(Dungeon dungeon) {
        List<String> result = new ArrayList<>();

        for (RoomPool pool : dungeon.getPools()) {
            if (pool.getRooms() == null || pool.getRooms().isEmpty()) continue;

            int count = pool.getMin() + Razdon.getInstance().nextInt(pool.getMax() - pool.getMin() + 1);

            double totalWeight = pool.getRooms().stream()
                    .mapToDouble(RoomEntry::getWeight)
                    .sum();

            for (int i = 0; i < count; i++) {
                double roll = Razdon.getInstance().nextDouble() * totalWeight;
                double cumulative = 0;

                for (RoomEntry entry : pool.getRooms()) {
                    cumulative += entry.getWeight();
                    if (roll < cumulative) {
                        result.add(entry.getRoomId());
                        break;
                    }
                }
            }
        }

        return result;
    }

    @Override
    public List<String> rollNextRoomFromPool(List<String> pool, int minimum, int completedRooms) {
        if (pool == null || pool.isEmpty()) return List.of();

        List<String> eligible = pool.stream()
                .filter(roomId -> {
                    Room room = roomManager.get(roomId);
                    if (room == null) return false;
                    if (room.getType() == RoomType.BOSS && completedRooms < minimum) return false;
                    return true;
                })
                .collect(Collectors.toList());

        if (eligible.isEmpty()) return List.of();

        Collections.shuffle(eligible);

        int count = Razdon.getInstance().nextIntInRange(1, 4);
        return eligible.subList(0, Math.min(count, eligible.size()));
    }

    @Override
    public void chooseRoom(DungeonInstance instance, String chosen) {

    }
}
