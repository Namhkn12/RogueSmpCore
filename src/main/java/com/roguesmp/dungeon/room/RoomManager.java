package com.roguesmp.dungeon.room;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.roguesmp.RogueSmpCore;
import com.roguesmp.dungeon.schemeta.SchemetaManager;
import com.roguesmp.dungeon.ultis.TimeId;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RoomManager {

    private final String prefix = "room";
    private static RoomManager INSTANCE = null;
    private final Map<String, Room> rooms = new HashMap<>();
    private final File folder;
    private final Gson gson;
    private final RogueSmpCore plugin;

    public static void init(RogueSmpCore plugin) {
        if(INSTANCE != null){
            throw new IllegalStateException("PartyManager already initialized!");
        }
        INSTANCE = new RoomManager(plugin);

        INSTANCE.loadRooms();
    }

    public static RoomManager getInstance(){
        if (INSTANCE == null) {
            throw new RuntimeException(RoomManager.class.getSimpleName() + "is null when getInstance() is called.");
        }
        return INSTANCE;
    }

    public void registerCommand(){
        new RoomCommand(this).register();
    }

    public RoomManager(RogueSmpCore plugin) {
        this.plugin = plugin;
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

        plugin.getLogger().info("Loaded " + this.getRooms().size() + " rooms successfully");
    }

    public Room createRoom(String roomName) {
        String timeId = TimeId.generateTimeId();
        Room room = new Room();
        room.setRoomId(timeId);
        room.setRoomName(roomName);
        room.setRoomType(RoomType.MONSTER);
        room.setSchemetaList(new ArrayList<>(List.of("schemdemoid")));

        rooms.put(timeId, room);
        return room;
    }

    public void saveRoom(String roomId) {
        Room room = rooms.get(roomId);
        if (room == null) return;

        File file = new File(folder, prefix + '_' + roomId + ".json");

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