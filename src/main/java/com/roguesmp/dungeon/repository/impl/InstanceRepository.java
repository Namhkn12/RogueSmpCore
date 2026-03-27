package com.roguesmp.dungeon.repository.impl;

import com.google.gson.Gson;
import com.roguesmp.RogueSmpCore;
import com.roguesmp.dungeon.constant.DataConfig;
import com.roguesmp.dungeon.instance.DungeonInstance;
import com.roguesmp.dungeon.instance.RoomInstance;
import com.roguesmp.dungeon.objective_.RestoreObjCallBack;
import com.roguesmp.dungeon.repository.IInstanceRepository;
import com.roguesmp.dungeon.utils.Log4Craft;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class InstanceRepository implements IInstanceRepository {

    private final Gson gson;
    private final File runtimeFolder;

    public InstanceRepository(Gson gson) {
        this.gson = gson;

        this.runtimeFolder = new File(
                RogueSmpCore.getInstance().getDataFolder(),
                DataConfig.getRuntimeFolder()
        );

        if (!runtimeFolder.exists()) {
            runtimeFolder.mkdirs();
        }
    }

    @Override
    public void save(DungeonInstance instance) {
        File file = getFile(instance.getParty());
        try (Writer writer = new FileWriter(file)) {
            gson.toJson(instance, writer);
        } catch (IOException e) {
            RogueSmpCore.getInstance().getLogger().log(
                    Level.SEVERE,
                    "Failed to save instance for party: " + instance.getParty(), e
            );
        }
    }

    @Override
    public void delete(UUID partyId) {
        File file = getFile(partyId);
        if (file.exists() && !file.delete()) {
            RogueSmpCore.getInstance().getLogger().warning(
                    "Failed to delete instance file for party: " + partyId
            );
        }
    }

    @Override
    public List<DungeonInstance> loadAll(RestoreObjCallBack callback) {
        List<DungeonInstance> result = new ArrayList<>();
        File[] files = runtimeFolder.listFiles(
                (dir, name) -> name.endsWith(DataConfig.JSON_TYPE)
        );

        if (files == null) return result;

        for (File file : files) {
            try (Reader reader = new FileReader(file)) {
                DungeonInstance instance = gson.fromJson(reader, DungeonInstance.class);
                if (instance != null) {
                    restoreObjectives(instance, callback);
                    result.add(instance);
                }
                Log4Craft.success("Restore instance to cache: " + result.size() + " instance");
            } catch (IOException e) {
                Log4Craft.fire("Failed to load instance file: " + file.getName(), e);
            }
        }
        return result;
    }

    private void restoreObjectives(DungeonInstance instance, RestoreObjCallBack onComplete) {
        RoomInstance activeRoom = instance.getActiveRoom();
        if (activeRoom == null || activeRoom.getObjective() == null) return;

        activeRoom.getObjective().forEach(obj -> {
            obj.callBack(completed -> {
                onComplete.provide(instance, activeRoom, obj);
            });
        });
    }

    // tên file = partyId.json
    private File getFile(UUID partyId) {
        return new File(runtimeFolder, DataConfig.DUNGEON_INSTANCE_FILE + partyId + DataConfig.JSON_TYPE);
    }
}
