package com.roguesmp.dungeon.manager;

import com.roguesmp.dungeon.constraint.PrefixConfig;
import com.roguesmp.dungeon.data.Spawner;
import com.roguesmp.dungeon.repository.ISpawnerRepository;
import com.roguesmp.dungeon.ultis.ConsoleLogger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SpawnerManager {
    private static final String PREFIX = "spawner";
    private final Map<String, Spawner> spawners = new HashMap<>();
    private final ISpawnerRepository spawnerRepository;

    public SpawnerManager(ISpawnerRepository spawnerRepository) {
        this.spawnerRepository = spawnerRepository;
    }

    public void load(){
        spawners.clear();
        spawners.putAll(spawnerRepository.loadAll());
        ConsoleLogger.info(PrefixConfig.SPAWNER,"Load " + spawners.size() + " spawner template file");
    }

    public Spawner get(String id){
        return spawners.get(id);
    }

    public Map<String, Spawner> getAllTemplate(){
        return spawners;
    }

    public Spawner create(String name){
        String id = PREFIX + "_" + UUID.randomUUID();
        Spawner spawner = new Spawner(id, name,new HashMap<>(), 0, 0, 0, 0, 0, 0, 0);
        return spawnerRepository.save(spawner);
    }

    public boolean delete(String id){
        if(!spawners.containsKey(id)) return false;
        spawners.remove(id);
        return spawnerRepository.delete(id);
    }
}
