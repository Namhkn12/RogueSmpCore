package com.roguesmp.dungeon_v2.manager;

import com.roguesmp.dungeon_v2.data.definition.room.Room;
import com.roguesmp.dungeon_v2.repository.IRoomRepository;
import com.roguesmp.dungeon_v2.utils_.Log4Craft_;

import java.util.HashMap;
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
}
