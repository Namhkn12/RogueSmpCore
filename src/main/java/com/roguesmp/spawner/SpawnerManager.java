package com.roguesmp.spawner;

import com.roguesmp.RogueSmpCore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpawnerManager {
    private final Map<String, Spawner> cache = new HashMap<>();
    private final SpawnerRepository spawnerRepository;

    public SpawnerManager(SpawnerRepository spawnerRepository) {
        this.spawnerRepository = spawnerRepository;

        load();
    }

    public void load(){
        cache.clear();
        List<Spawner> spawners = spawnerRepository.loadAll();
        if(spawners == null) {
            RogueSmpCore.LOGGER.debug("No data found" );
            return;
        }
        spawners.forEach(spawner -> {
            cache.put(spawner.getId(), spawner);
        });
        RogueSmpCore.LOGGER.info("Loaded data: {} record", cache.size());
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
