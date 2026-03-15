package com.roguesmp.dungeon.manager;

import com.roguesmp.dungeon.instance.DungeonInstance;
import com.roguesmp.dungeon.repository.IInstanceRepository;

import java.util.*;

/**
 * Quản lý state in-memory của tất cả DungeonInstance đang active.
 * Key = partyId để lookup nhanh từ player event.
 */
public class InstanceManager {

    private final IInstanceRepository repository;

    private final Map<UUID, DungeonInstance> instances = new LinkedHashMap<>();

    public InstanceManager(IInstanceRepository repository) {
        this.repository = repository;
        loadAll();
    }

    /**
     * Load tất cả instance từ file khi server restart
     */
    public void loadAll() {
        instances.clear();
        repository.loadAll().forEach(instance ->
                instances.put(instance.getParty(), instance)
        );
    }

    /**
     * Lưu tất cả instance khi server close
     */
    public void saveAll() {
        instances.values().forEach(repository::save);
    }

    /**
     * Thêm instance mới vào memory và ghi file ngay
     */
    public void add(DungeonInstance instance) {
        instances.put(instance.getParty(), instance);
        repository.save(instance);
    }

    /**
     * Xóa instance khỏi memory và xóa file
     */
    public void remove(UUID partyId) {
        instances.remove(partyId);
        repository.delete(partyId);
    }

    /**
     * Lưu lại instance đã thay đổi (dùng sau checkpoint, clear room, ...)
     */
    public void save(UUID partyId) {
        DungeonInstance instance = instances.get(partyId);
        if (instance != null) {
            repository.save(instance);
        }
    }

    public Optional<DungeonInstance> getByParty(UUID partyId) {
        return Optional.ofNullable(instances.get(partyId));
    }

    public Collection<DungeonInstance> getAll() {
        return Collections.unmodifiableCollection(instances.values());
    }

    public boolean hasActiveInstance(UUID partyId) {
        return instances.containsKey(partyId);
    }
}
