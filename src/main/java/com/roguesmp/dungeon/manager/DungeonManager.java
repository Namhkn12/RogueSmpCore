package com.roguesmp.dungeon.manager;

import com.roguesmp.dungeon.repository.DungeonRepository;
import com.roguesmp.RogueSmpCore;
import com.roguesmp.dungeon.data.definition.Dungeon;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DungeonManager {

    private final Map<String, Dungeon> cache = new HashMap<>();

    private final DungeonRepository dungeonRepository;

    public DungeonManager(DungeonRepository dungeonRepository) {
        this.dungeonRepository = dungeonRepository;

        loadAll();
    }

    public void loadAll(){
        cache.clear();
        dungeonRepository.loadAll().forEach(d -> cache.put(d.getId(), d));
        RogueSmpCore.LOGGER.info("Loaded data: {} record", cache.size());
    }

    public boolean delete(String did){
        if(!cache.containsKey(did)) return false;
        boolean result = dungeonRepository.delete(did);
        if(result) cache.remove(did);
        return result;
    }

    public Dungeon create(Dungeon dungeon){
        Dungeon saved = dungeonRepository.save(dungeon);
        if(saved == null) return null;
        cache.put(saved.getId(), dungeon);
        return saved;
    }

    public Dungeon get(String did){
        return cache.get(did);
    }

    public List<String> getAllIds() {
        return cache.keySet().stream()
                .sorted()
                .toList();
    }
}
