package com.roguesmp.dungeon.manager;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.dungeon.data.definition.room.Room;
import com.roguesmp.dungeon.repository.IRoomRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RoomManager {

    private final Map<String, Room> cache = new HashMap<>();

    private final IRoomRepository roomRepository;

    public RoomManager(IRoomRepository roomRepository) {
        this.roomRepository = roomRepository;

        loadAll();
    }

    public void loadAll(){
        cache.clear();
        roomRepository.loadAll().forEach(r -> cache.put(r.getId(), r));
        RogueSmpCore.LOGGER.info("Loaded data: {} record", cache.size());
    }

    public boolean delete(String rid){
        if(!cache.containsKey(rid)) return false;
        boolean result = roomRepository.delete(rid);
        if(result) cache.remove(rid);
        return result;
    }

    public Room create(Room room){
        Room saved = roomRepository.save(room);
        if(saved == null) return null;
        cache.put(saved.getId(), saved);
        return saved;
    }

    public Room get(String rid){
        return cache.get(rid);
    }

    public List<String> getAllIds() {
        return cache.keySet().stream()
                .sorted()
                .toList();
    }
}
