package com.roguesmp.dungeon_v2.manager;

import com.roguesmp.dungeon_v2.data.definition.Dungeon;
import com.roguesmp.dungeon_v2.repository.IDungeonRepository;
import com.roguesmp.dungeon_v2.utils.Log4Craft_;

import java.util.HashMap;
import java.util.Map;

public class DungeonManager {

    private final Map<String, Dungeon> cache = new HashMap<>();

    private final IDungeonRepository dungeonRepository;
    private final Log4Craft_ logger;

    public DungeonManager(IDungeonRepository dungeonRepository, Log4Craft_ logger) {
        this.dungeonRepository = dungeonRepository;
        this.logger = logger;

        loadAll();
    }

    public void loadAll(){
        cache.clear();
        dungeonRepository.loadAll().forEach(d -> cache.put(d.getId(), d));
        logger.info(this.getClass(),"Loaded data" + cache.size() + " record");
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
}
