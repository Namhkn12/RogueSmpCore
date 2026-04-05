package com.roguesmp.dungeon_v2.repository.impl;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.roguesmp.dungeon_v2.config.DataFolderConfig;
import com.roguesmp.dungeon_v2.data.definition.room.Room;
import com.roguesmp.dungeon_v2.exception.impl.data.DataDeleteException;
import com.roguesmp.dungeon_v2.exception.impl.data.DataLoadException;
import com.roguesmp.dungeon_v2.exception.impl.data.DataSaveException;
import com.roguesmp.dungeon_v2.repository.IRoomRepository;
import com.roguesmp.dungeon_v2.utils.Log4Craft_;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class RoomRepository implements IRoomRepository {

    private final File dataFolder;
    private final Gson gson;
    private final Log4Craft_ logger;

    public RoomRepository(Plugin plugin, Gson gson, Log4Craft_ logger) {
        this.dataFolder = new File(plugin.getDataFolder(), DataFolderConfig.getRoomTemplateFolder());
        this.gson = gson;
        this.logger = logger;

        if(!dataFolder.exists()) dataFolder.mkdirs();
    }

    @Override
    public List<Room> loadAll() {
        List<Room> rooms = new ArrayList<>();
        if(!dataFolder.exists() || !dataFolder.isDirectory()){
            throw new DataLoadException(dataFolder.getName(), null);
        }

        File[] files = dataFolder.listFiles(((dir, name) -> name.endsWith(DataFolderConfig.JSON_TYPE)));
        if(files == null) return rooms;

        for(File file : files){
            try(Reader reader = Files.newBufferedReader(file.toPath())){
                Room room = gson.fromJson(reader, Room.class);
                if(room != null && room.getId() != null){
                    rooms.add(room);
                }else{
                    logger.error(this.getClass(), "Data is null or invalid, please check " + file.getAbsolutePath());
                }
            }catch (JsonParseException | IOException e){
                logger.error(this.getClass(), "Invalid data in file " + file.getAbsolutePath());
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
            throw new DataSaveException(file.getName(), e);
        }
    }

    @Override
    public boolean delete(String rid) {
        File file = new File(dataFolder, DataFolderConfig.ROOM_TEMPLATE_FILE + rid + DataFolderConfig.JSON_TYPE);
        if(!file.exists()) return false;
        if(!file.delete()){
            throw new DataDeleteException(file.getName(), null);
        }
        return true;
    }
}
