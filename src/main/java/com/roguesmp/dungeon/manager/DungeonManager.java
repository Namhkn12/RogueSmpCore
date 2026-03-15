package com.roguesmp.dungeon.manager;

import com.roguesmp.dungeon.constraint.PrefixConfig;
import com.roguesmp.dungeon.data.Dungeon;
import com.roguesmp.dungeon.data.Room;
import com.roguesmp.dungeon.repository.IDungeonRepository;
import com.roguesmp.dungeon.ultis.ConsoleLogger;
import com.roguesmp.dungeon.ultis.TimeId;

import java.util.*;

public class DungeonManager {

    private static final String PREFIX = "dungeon";

    private final Map<String, Dungeon> dungeons = new HashMap<>();
    private final IDungeonRepository dungeonRepository;

    public DungeonManager(IDungeonRepository dungeonRepository) {
        this.dungeonRepository = dungeonRepository;

        load();
    }

    /** Load all dungeons from storage into cache. */
    public void load() {
        dungeons.clear();
        dungeonRepository.loadAll().forEach(d -> dungeons.put(d.getDgId(), d));
        ConsoleLogger.info(PrefixConfig.DUNGEON,"Load " + dungeons.size() + " dungeon template file");
    }

    /** Create a new dungeon, persist it, and put it in cache. */
    public Dungeon create(String name) {
        String dgId = PREFIX + "_" + TimeId.generateTimeId();
        Dungeon dungeon = new Dungeon(dgId, name, "", 0);

        dungeons.put(dgId, dungeon);
        dungeonRepository.save(dungeon);

        return dungeon;
    }

    /** Update an existing dungeon in cache and persist it. */
    public boolean update(Dungeon dungeon) {
        if (dungeon == null || dungeon.getDgId() == null) return false;
        if (!dungeons.containsKey(dungeon.getDgId())) return false;

        dungeons.put(dungeon.getDgId(), dungeon);
        dungeonRepository.save(dungeon);
        return true;
    }

    /** Remove a dungeon from cache and storage. */
    public boolean delete(String dgId) {
        if (!dungeons.containsKey(dgId)) return false;

        dungeons.remove(dgId);
        return dungeonRepository.delete(dgId);
    }

    public List<String> getDungeonIdList() {
        return List.copyOf(dungeons.keySet());
    }

    public Optional<Dungeon> getById(String dgId) {
        return Optional.ofNullable(dungeons.get(dgId));
    }

    public Collection<Dungeon> getAll() {
        return Collections.unmodifiableCollection(dungeons.values());
    }

    public boolean exists(String dgId) {
        return dungeons.containsKey(dgId);
    }
}