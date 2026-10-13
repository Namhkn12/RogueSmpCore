package com.roguesmp.dungeon.repository.impl;

import com.roguesmp.RogueSmpCore;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.roguesmp.dungeon.config.DataFolderConfig;
import com.roguesmp.dungeon.data.definition.room.Room;
import com.roguesmp.dungeon.repository.IRoomRepository;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class RoomRepository implements IRoomRepository {

    private final File dataFolder;
    private final Gson gson;

    public RoomRepository(Plugin plugin, Gson gson) {
        this.dataFolder = new File(plugin.getDataFolder(), DataFolderConfig.getRoomTemplateFolder());
        this.gson = gson;

        if(!dataFolder.exists()) dataFolder.mkdirs();
    }

    @Override
    public List<Room> loadAll() {
        List<Room> rooms = new ArrayList<>();
        if(!dataFolder.exists() || !dataFolder.isDirectory()){
            RogueSmpCore.LOGGER.error("Data folder not found: {}", dataFolder.getPath());
            return rooms;
        }

        File[] files = dataFolder.listFiles(((dir, name) -> name.endsWith(DataFolderConfig.JSON_TYPE)));
        if(files == null) return rooms;

        for(File file : files){
            try(Reader reader = Files.newBufferedReader(file.toPath())){
                Room room = gson.fromJson(reader, Room.class);
                if(room != null && room.getId() != null){
                    rooms.add(room);
                }else{
                    RogueSmpCore.LOGGER.error("Data is null or invalid, please check {}", file.getAbsolutePath());
                }
            }catch (JsonParseException | IOException e){
                RogueSmpCore.LOGGER.error("Invalid data in file {}", file.getAbsolutePath());
            }
        }
        return rooms;
    }

    @Override
    public Room save(Room room) {
        File file = new File(dataFolder, DataFolderConfig.ROOM_TEMPLATE_FILE + room.getId() + DataFolderConfig.JSON_TYPE);
        try (Writer writer = new FileWriter(file, StandardCharsets.UTF_8)){
            gson.toJson(room, writer);
            return room;
        } catch (IOException e){
            RogueSmpCore.LOGGER.error("Failed to save file: {}", file.getName(), e);
            return null;
        }
    }

    @Override
    public boolean delete(String rid) {
        File file = new File(dataFolder, DataFolderConfig.ROOM_TEMPLATE_FILE + rid + DataFolderConfig.JSON_TYPE);
        if(!file.exists()) return false;
        if(!file.delete()){
            RogueSmpCore.LOGGER.error("Failed to delete file: {}", file.getName());
            return false;
        }
        return true;
    }
}
