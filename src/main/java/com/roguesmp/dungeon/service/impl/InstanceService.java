package com.roguesmp.dungeon.service.impl;

import com.roguesmp.dungeon.data.Dungeon;
import com.roguesmp.dungeon.data.Node;
import com.roguesmp.dungeon.data.RoomEntry;
import com.roguesmp.dungeon.instance.DungeonInstance;
import com.roguesmp.dungeon.instance.NodeInstance;
import com.roguesmp.dungeon.instance.RegionInstance;
import com.roguesmp.dungeon.manager.DungeonManager;
import com.roguesmp.dungeon.manager.InstanceManager;
import com.roguesmp.dungeon.service.IInstanceService;

import java.util.*;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public class InstanceService implements IInstanceService {

    private final DungeonManager dungeonManager;
    private final InstanceManager instanceManager;

    public InstanceService(DungeonManager dungeonManager, InstanceManager instanceManager) {
        this.dungeonManager = dungeonManager;
        this.instanceManager = instanceManager;
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @Override
    public void onServerStart() {
        instanceManager.loadAll();
    }

    @Override
    public void onServerStop() {
        instanceManager.saveAll();
    }

    // -------------------------------------------------------------------------
    // Instance management
    // -------------------------------------------------------------------------

    @Override
    public DungeonInstance createDungeonInstance(String dungeonId, UUID party, RegionInstance region) {
        Map<UUID, NodeInstance> nodes = rollNodeData(dungeonId);

        DungeonInstance instance = new DungeonInstance(
                UUID.randomUUID(),
                dungeonId,
                party,
                region,
                nodes
        );

        instanceManager.add(instance);
        return instance;
    }

    @Override
    public void endDungeonInstance(UUID partyId) {
        instanceManager.remove(partyId);
    }

    @Override
    public void saveInstance(UUID partyId) {
        instanceManager.save(partyId);
    }

    @Override
    public Optional<DungeonInstance> getInstance(UUID partyId) {
        return instanceManager.getByParty(partyId);
    }

    @Override
    public boolean hasActiveInstance(UUID partyId) {
        return instanceManager.hasActiveInstance(partyId);
    }

    // -------------------------------------------------------------------------
    // Next rooms
    // -------------------------------------------------------------------------

    @Override
    public void rollNextRooms(DungeonInstance instance, int nextRoomCount) {
        Map<UUID, NodeInstance> remaining = instance.getNodes();
        if (remaining == null || remaining.isEmpty()) return;

        Map<UUID, Integer> nextRooms = new LinkedHashMap<>();

        // Lấy tối đa nextRoomCount node từ nodes còn lại
        List<UUID> keys = new ArrayList<>(remaining.keySet());
        int count = Math.min(nextRoomCount, keys.size());

        // Shuffle để không luôn lấy theo thứ tự cố định
        Collections.shuffle(keys);

        Set<Integer> usedSlots = new HashSet<>();
        for (int i = 0; i < count; i++) {
            UUID nodeId = keys.get(i);
            int slot = rollUniqueSlot(usedSlots);
            usedSlots.add(slot);
            nextRooms.put(nodeId, slot);
        }

        instance.setNextRooms(nextRooms);
    }

    @Override
    public NodeInstance selectNextRoom(DungeonInstance instance, UUID nodeInstanceId) {
        // Xóa khỏi nextRooms
        Map<UUID, Integer> nextRooms = instance.getNextRooms();
        if (nextRooms != null) {
            nextRooms.remove(nodeInstanceId);
        }

        // Lấy NodeInstance rồi xóa khỏi nodes
        Map<UUID, NodeInstance> nodes = instance.getNodes();
        if (nodes == null) return null;

        NodeInstance selected = nodes.remove(nodeInstanceId);

        // Clear nextRooms hoàn toàn — lần sau roll mới
        instance.setNextRooms(new LinkedHashMap<>());

        return selected;
    }

    // -------------------------------------------------------------------------
    // Roll node data
    // -------------------------------------------------------------------------

    /**
     * Roll toàn bộ nodes từ dungeon template theo thứ tự:
     * start (100%) → middle nodes (theo chance) → end (100%)
     */
    private Map<UUID, NodeInstance> rollNodeData(String dungeonId) {
        Optional<Dungeon> optional = dungeonManager.getById(dungeonId);
        if (optional.isEmpty()) return new LinkedHashMap<>();

        Map<String, Node> nodes = optional.get().getNodes();
        Map<UUID, NodeInstance> result = new LinkedHashMap<>();

        // 1. start — chance 100%, number = 1
        Node start = nodes.get("start");
        if (start != null) {
            rollNode(start).forEach(n -> result.put(n.getId(), n));
        }

        // 2. Middle nodes — bỏ start và end, roll theo chance
        for (Map.Entry<String, Node> entry : nodes.entrySet()) {
            String key = entry.getKey();
            if (key.equals("start") || key.equals("end")) continue;

            Node node = entry.getValue();
            double roll = ThreadLocalRandom.current().nextDouble(100);
            if (roll <= node.getChance() * 100) {
                rollNode(node).forEach(n -> result.put(n.getId(), n));
            }
        }

        // 3. end — chance 100%, number = 1
        Node end = nodes.get("end");
        if (end != null) {
            rollNode(end).forEach(n -> result.put(n.getId(), n));
        }

        return result;
    }

    /**
     * Roll 1 node ra number NodeInstance, mỗi cái chứa list schemeta được chọn.
     * Dùng weighted random để chọn schemeta từ RoomEntry pool.
     */
    private List<NodeInstance> rollNode(Node node) {
        List<NodeInstance> result = new ArrayList<>();
        List<RoomEntry> pool = node.getRooms();

        if (pool == null || pool.isEmpty()) {
            // Không có room nào trong pool → tạo NodeInstance rỗng
            for (int i = 0; i < node.getNumber(); i++) {
                result.add(new NodeInstance(
                        UUID.randomUUID(),
                        node.getKey(),
                        node.getName(),
                        node.getIcon(),
                        new ArrayList<>()
                ));
            }
            return result;
        }

        double totalWeight = pool.stream()
                .mapToDouble(RoomEntry::getWeight)
                .sum();

        for (int i = 0; i < node.getNumber(); i++) {
            List<String> schemetas = new ArrayList<>();
            double roll = ThreadLocalRandom.current().nextDouble(totalWeight);
            double cumulative = 0;

            for (RoomEntry entry : pool) {
                cumulative += entry.getWeight();
                if (roll <= cumulative) {
                    schemetas.add(entry.getFile());
                    break;
                }
            }

            result.add(new NodeInstance(
                    UUID.randomUUID(),
                    node.getKey(),
                    node.getName(),
                    node.getIcon(),
                    schemetas
            ));
        }

        return result;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Roll 1 số nguyên dương unique chưa dùng, dùng cho slot UI
     */
    private int rollUniqueSlot(Set<Integer> usedSlots) {
        int slot;
        do {
            slot = ThreadLocalRandom.current().nextInt(1, 100);
        } while (usedSlots.contains(slot));
        return slot;
    }
}