package com.roguesmp.dungeon.manager;

import com.roguesmp.dungeon.constant.PrefixConfig;
import com.roguesmp.dungeon.data.Spawner;
import com.roguesmp.dungeon.dto.DataResult;
import com.roguesmp.dungeon.exception.impl.spawner.SpawnerNotFoundException;
import com.roguesmp.dungeon.repository.ISpawnerRepository;
import com.roguesmp.dungeon.utils.ConsoleLogger;
import com.roguesmp.dungeon.utils.Log4Craft;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SpawnerManager {
    private final Map<String, Spawner> spawners = new HashMap<>();
    private final ISpawnerRepository spawnerRepository;

    public SpawnerManager(ISpawnerRepository spawnerRepository) {
        this.spawnerRepository = spawnerRepository;
        load();
    }

    public void load(){
        spawners.clear();
        DataResult<Map<String, Spawner>> result = spawnerRepository.loadAll();
        spawners.putAll(result.getResult());

        if(result.hasLogs()){
            result.getLogs().forEach(Log4Craft::warn);
        }
    }

    public Spawner get(String id) {
        Spawner spawner = spawners.get(id);
        if (spawner == null) throw new SpawnerNotFoundException(id, null);
        return spawner;
    }
    public Map<String, Spawner> getAllTemplate(){
        return spawners;
    }

    public Spawner create(String name){
        String id = UUID.randomUUID().toString();
        Spawner spawner = new Spawner(id, name,new HashMap<>(), 0, 0, 0, 0, 0, 0, 0);
        Spawner saved = spawnerRepository.save(spawner);
        spawners.put(saved.getId(), saved);
        return saved;
    }

    public boolean delete(String id){
        if(!spawners.containsKey(id)) return false;
        boolean result = spawnerRepository.delete(id);
        spawners.remove(id);
        return result;
    }
}
