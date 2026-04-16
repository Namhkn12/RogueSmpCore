package com.roguesmp.dungeon_v2.manager;

import com.roguesmp.dungeon_v2.data.definition.spawner.Spawner;
import com.roguesmp.dungeon_v2.repository.ISpawnerRepository;
import com.roguesmp.dungeon_v2.utils.Log4Craft_;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpawnerManager {
    private final Map<String, Spawner> cache = new HashMap<>();
    private final ISpawnerRepository spawnerRepository;
    private final Log4Craft_ logger;

    public SpawnerManager(ISpawnerRepository spawnerRepository, Log4Craft_ logger) {
        this.spawnerRepository = spawnerRepository;
        this.logger = logger;

        load();
    }

    public void load(){
        cache.clear();
        List<Spawner> spawners = spawnerRepository.loadAll();
        if(spawners == null) {
            logger.debug(this.getClass(), "No data found" );
            return;
        }
        spawners.forEach(spawner -> {
            cache.put(spawner.getId(), spawner);
        });
        logger.info(this.getClass(), "Loaded data: " + cache.size() + " record");
    }

    public Spawner getById(String id){
        return cache.get(id);
    }

    public List<String> getAllIds() {
        return cache.keySet().stream()
                .sorted()
                .toList();
    }

    public Spawner create(Spawner spawner){
        Spawner saved = spawnerRepository.save(spawner);
        if(saved == null) return null;
        cache.put(saved.getId(), saved);
        return saved;
    }

    public boolean delete(String id){
        if(!cache.containsKey(id)) return false;
        boolean result = spawnerRepository.delete(id);
        if(result) cache.remove(id);
        return result;
    }
}
