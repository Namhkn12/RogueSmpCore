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
import java.util.stream.Collectors;

public class InstanceService implements IInstanceService {

    private final DungeonManager dungeonManager;
    private final InstanceManager instanceManager;

    public InstanceService(DungeonManager dungeonManager, InstanceManager instanceManager) {
        this.dungeonManager = dungeonManager;
        this.instanceManager = instanceManager;
    }

    @Override
    public void onServerStop() {
        instanceManager.saveAll();
    }

    @Override
    public DungeonInstance createDungeonInstance(Dungeon dungeon, UUID party, RegionInstance region) {
        Map<UUID, NodeInstance> nodes = rollNodeData(dungeon.getDgId());
        int minToEndSetting = dungeon.getMinRoomToEnd();
        int minToEnd = minToEndSetting == 0 ? nodes.size() * 2/3 : minToEndSetting;

        DungeonInstance instance = new DungeonInstance(
                UUID.randomUUID(),
                dungeon.getDgId(),
                party,
                region,
                nodes,
                minToEnd
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

    private static final List<Integer> VALID_SLOTS = List.of(
            1, 2, 3, 5, 6, 7,
            10, 11, 12, 14, 15, 16,
            19, 20, 21, 23, 24, 25,
            28, 29, 30, 32, 33, 34,
            37, 38, 39, 41, 42, 43
    );
    private static final int END_SLOT = 4;

    @Override
    public void rollNextRooms(DungeonInstance instance) {
        Map<UUID, NodeInstance> remaining = instance.getNodes();
        if (remaining == null || remaining.isEmpty()) {
            instance.setNextRooms(new HashMap<>());
            return;
        }

        int completedCount = instance.getCompletedRooms() == null ? 0 : instance.getCompletedRooms().size();

        // lấy minRoomsToEnd từ dungeon instance
        int minRooms = instance.getMinRoomToEnd();

        boolean canRollEnd = completedCount >= minRooms;

        // chỉ còn end → bắt buộc roll end dù chưa đủ điều kiện
        boolean onlyEndLeft = remaining.values().stream()
                .allMatch(n -> n.getNodeKey().equals("end"));

        List<UUID> pool = remaining.entrySet().stream()
                .filter(e -> onlyEndLeft || canRollEnd || !e.getValue().getNodeKey().equals("end"))
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(ArrayList::new));

        if (pool.isEmpty()) {
            instance.setNextRooms(new HashMap<>());
            return;
        }

        Collections.shuffle(pool);
        int count = ThreadLocalRandom.current().nextInt(1, Math.max(1, (int) Math.floor(pool.size() * 2.0 / 3.0)) + 1);

        instance.setNextRooms(pickSlots(pool.subList(0, Math.min(count, pool.size())), remaining));
    }

    private Map<UUID, Integer> pickSlots(List<UUID> pool, Map<UUID, NodeInstance> remaining) {
        List<Integer> available = new ArrayList<>(VALID_SLOTS);
        Collections.shuffle(available);

        Map<UUID, Integer> result = new LinkedHashMap<>();

        for (UUID nodeId : pool) {
            boolean isEnd = remaining.get(nodeId).getNodeKey().equals("end");
            if (isEnd) {
                result.put(nodeId, END_SLOT);
                continue;
            }
            if (!available.isEmpty()) {
                result.put(nodeId, available.remove(0));
            }
        }

        return result;
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

    @Override
    public Map<UUID, DungeonInstance> getAllInstances() {
        return instanceManager.getInstancesMap();
    }

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
            for (int i = 0; i < node.getNumber(); i++) {
                result.add(new NodeInstance(
                        UUID.randomUUID(),
                        node.getKey(),
                        node.getName(),
                        node.getIcon(),
                        null
                ));
            }
            return result;
        }

        // Copy pool để loại dần schemeta đã chọn
        List<RoomEntry> remaining = new ArrayList<>(pool);

        for (int i = 0; i < node.getNumber(); i++) {
            if (remaining.isEmpty()) break;

            double totalWeight = remaining.stream()
                    .mapToDouble(RoomEntry::getWeight)
                    .sum();

            String schemetas = null;
            double roll = ThreadLocalRandom.current().nextDouble(totalWeight);
            double cumulative = 0;

            RoomEntry chosen = null;
            for (RoomEntry entry : remaining) {
                cumulative += entry.getWeight();
                if (roll <= cumulative) {
                    schemetas = entry.getFile();
                    chosen = entry;
                    break;
                }
            }

            // Loại schemeta đã chọn khỏi pool
            if (chosen != null) remaining.remove(chosen);

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
}