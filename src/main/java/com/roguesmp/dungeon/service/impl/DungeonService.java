package com.roguesmp.dungeon.service.impl;

import com.roguesmp.dungeon.data.definition.Dungeon;
import com.roguesmp.dungeon.data.definition.room.Room;
import com.roguesmp.dungeon.data.definition.room.RoomEntry;
import com.roguesmp.dungeon.data.definition.room.RoomPool;
import com.roguesmp.dungeon.data.definition.room.RoomType;
import com.roguesmp.dungeon.data.runtime.DungeonInstance;
import com.roguesmp.dungeon.manager.DungeonManager;
import com.roguesmp.dungeon.manager.RoomManager;
import com.roguesmp.dungeon.service.IDungeonService;
import com.roguesmp.dungeon.utils.Log4Craft_;
import com.roguesmp.dungeon.utils.Razdon;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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
        Set<String> selectedRoomIds = new LinkedHashSet<>();

        for (RoomPool pool : dungeon.getPools()) {
            if (pool.getRooms() == null || pool.getRooms().isEmpty()) continue;

            int count = pool.getMin() + Razdon.getInstance().nextInt(pool.getMax() - pool.getMin() + 1);
            List<RoomEntry> remaining = pool.getRooms().stream()
                    .filter(entry -> entry != null && entry.getRoomId() != null && !entry.getRoomId().isBlank())
                    .filter(entry -> !selectedRoomIds.contains(entry.getRoomId()))
                    .collect(Collectors.toCollection(ArrayList::new));

            count = Math.min(count, remaining.size());

            for (int i = 0; i < count; i++) {
                if (remaining.isEmpty()) break;

                double totalWeight = remaining.stream()
                        .mapToDouble(RoomEntry::getWeight)
                        .sum();

                double roll = Razdon.getInstance().nextDouble() * totalWeight;
                double cumulative = 0;

                for (int index = 0; index < remaining.size(); index++) {
                    RoomEntry entry = remaining.get(index);
                    cumulative += entry.getWeight();
                    if (roll < cumulative) {
                        String roomId = entry.getRoomId();
                        selectedRoomIds.add(roomId);
                        result.add(roomId);
                        remaining.removeIf(e -> roomId.equals(e.getRoomId()));
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
                .distinct()
                .filter(roomId -> {
                    Room room = roomManager.get(roomId);
                    if (room == null) return false;
                    if (room.getType() == RoomType.SPAWN) return false;
                    if (room.getType() == RoomType.TREASURE) return false;
                    return room.getType() != RoomType.BOSS || completedRooms >= minimum;
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
