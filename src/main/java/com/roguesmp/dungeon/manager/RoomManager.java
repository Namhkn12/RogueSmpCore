package com.roguesmp.dungeon.manager;

import com.roguesmp.dungeon.data.definition.room.Room;
import com.roguesmp.dungeon.repository.IRoomRepository;
import com.roguesmp.dungeon.utils.Log4Craft_;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RoomManager {

    private final Map<String, Room> cache = new HashMap<>();

    private final IRoomRepository roomRepository;
    private final Log4Craft_ logger;

    public RoomManager(IRoomRepository roomRepository, Log4Craft_ logger) {
        this.roomRepository = roomRepository;
        this.logger = logger;

        loadAll();
    }

    public void loadAll(){
        cache.clear();
        roomRepository.loadAll().forEach(r -> cache.put(r.getId(), r));
        logger.info(this.getClass(),"Loaded data" + cache.size() + " record");
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
