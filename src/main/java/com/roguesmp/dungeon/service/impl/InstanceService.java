package com.roguesmp.dungeon.service.impl;

import com.roguesmp.dungeon.data.Dungeon;
import com.roguesmp.dungeon.data.Node;
import com.roguesmp.dungeon.data.RoomEntry;
import com.roguesmp.dungeon.instance.DungeonInstance;
import com.roguesmp.dungeon.instance.NodeInstance;
import com.roguesmp.dungeon.manager.DungeonInstanceManager;
import com.roguesmp.dungeon.manager.DungeonManager;
import com.roguesmp.dungeon.service.IInstanceService;
import org.bukkit.Location;

import java.util.*;

public class InstanceService implements IInstanceService {
    private final DungeonManager dungeonManager;
    private final DungeonInstanceManager dungeonInstanceManager;

    public InstanceService(DungeonManager dungeonManager, DungeonInstanceManager dungeonInstanceManager) {
        this.dungeonManager = dungeonManager;
        this.dungeonInstanceManager = dungeonInstanceManager;
    }

    @Override
    public DungeonInstance createDungeonInstance(String dungeon, UUID party, Location location) {
        DungeonInstance instance = dungeonInstanceManager.createInstance(dungeon, party, location);
        return instance;
    }

    public LinkedHashMap<String, NodeInstance> rollNodeData(String dungeonId) {
        Optional<Dungeon> optional = dungeonManager.getById(dungeonId);
        if (optional.isEmpty()) return new LinkedHashMap<>();

        Map<String, Node> nodes = optional.get().getNodes();
        LinkedHashMap<String, NodeInstance> result = new LinkedHashMap<>();

        // 1. SPAWN trước — chance 100%, chỉ 1 room
        Node spawn = nodes.get("SPAWN");
        if (spawn != null) {
            result.put("SPAWN", rollNode(spawn, 1));
        }

        // 2. Các node còn lại, trừ SPAWN và BOSS
        for (Map.Entry<String, Node> entry : nodes.entrySet()) {
            String key = entry.getKey();
            if (key.equals("SPAWN") || key.equals("BOSS")) continue;

            Node node = entry.getValue();

            // roll chance xem node này có được chọn không
            if (Math.random() * 100 <= node.getChance()) {
                result.put(key, rollNode(node, node.getNumber()));
            }
        }

        // 3. BOSS cuối — chance 100%, chỉ 1 room
        Node boss = nodes.get("BOSS");
        if (boss != null) {
            result.put("BOSS", rollNode(boss, 1));
        }

        return result;
    }

    private NodeInstance rollNode(Node node, int number) {
        List<String> selected = new ArrayList<>();
        List<RoomEntry> pool = node.getRooms();

        if (pool == null || pool.isEmpty()) {
            return new NodeInstance(node.getKey(), node.getName(), node.getIcon(), selected);
        }

        double totalWeight = pool.stream()
                .mapToDouble(RoomEntry::getWeight)
                .sum();

        for (int i = 0; i < number; i++) {
            double roll = Math.random() * totalWeight;
            double cumulative = 0;

            for (RoomEntry entry : pool) {
                cumulative += entry.getWeight();
                if (roll <= cumulative) {
                    selected.add(entry.getFile());
                    break;
                }
            }
        }

        return new NodeInstance(node.getKey(), node.getName(), node.getIcon(), selected);
    }
}
