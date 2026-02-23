package com.roguesmp.dungeon.room;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.roguesmp.RogueSmpCore;
import com.roguesmp.dungeon.ultis.TimeId;

import java.io.*;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class RoomManager {

    private final Map<String, Room> rooms = new HashMap<>();
    private final File folder;
    private final Gson gson;

    public RoomManager(RogueSmpCore plugin) {
        this.folder = new File(plugin.getDataFolder(), "rooms");

        if (!folder.exists()) {
            folder.mkdirs();
        }

        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public void loadRooms() {
        rooms.clear();

        File[] files = folder.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) return;

        for (File file : files) {
            try (Reader reader = Files.newBufferedReader(file.toPath())) {
                Room room = gson.fromJson(reader, Room.class);
                if (room != null && room.getRoomId() != null) {
                    rooms.put(room.getRoomId(), room);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public Room createRoom(String roomName) {
        String timeId = TimeId.generateTimeId();
        Room room = new Room();
        room.setRoomId(timeId);
        room.setRoomName(roomName);
        room.setRoomType(RoomType.MONSTER);

        rooms.put(timeId, room);
        return room;
    }

    public void saveRoom(String roomId) {
        Room room = rooms.get(roomId);
        if (room == null) return;

        File file = new File(folder, roomId + ".json");

        try (Writer writer = Files.newBufferedWriter(file.toPath())) {
            gson.toJson(room, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void deleteRoom(String roomId) {
        rooms.remove(roomId);

        File file = new File(folder, roomId + ".json");
        if (file.exists()) {
            file.delete();
        }
    }

    public Room getRoom(String roomId) {
        return rooms.get(roomId);
    }

    public Map<String, Room> getRooms() {
        return rooms;
    }
}