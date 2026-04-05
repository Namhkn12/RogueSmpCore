package com.roguesmp.dungeon_v2.repository.impl;

import com.google.gson.Gson;
import com.roguesmp.dungeon_v2.config.DataFolderConfig;
import com.roguesmp.dungeon_v2.data.definition.objective.IObjective;
import com.roguesmp.dungeon_v2.data.definition.objective.factory.ObjectiveFactory;
import com.roguesmp.dungeon_v2.data.runtime.DungeonInstance;
import com.roguesmp.dungeon_v2.data.runtime.RoomInstance;
import com.roguesmp.dungeon_v2.data.runtime.session.DungeonProgress;
import com.roguesmp.dungeon_v2.repository.IInstanceRepository;
import com.roguesmp.dungeon_v2.utils.Log4Craft_;
import org.bukkit.plugin.Plugin;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class InstanceRepository implements IInstanceRepository {

    private final Log4Craft_ logger;
    private final Gson gson;
    private final File dataFolder;

    public InstanceRepository(Plugin plugin, Gson gson, Log4Craft_ logger) {
        this.logger = logger;
        this.gson = gson;
        this.dataFolder = new File(
               plugin.getDataFolder(), DataFolderConfig.getDungeonSessionFolder()
        );
        if (!dataFolder.exists()) dataFolder.mkdirs();
    }

    @Override
    public void save(DungeonInstance instance) {
        File file = getFile(instance.getSession().getSessionId());
        try (Writer writer = new FileWriter(file)) {
            gson.toJson(instance, writer);
        } catch (IOException e) {
            logger.warn(this.getClass(), "Failed to save instance: " + instance.getSession().getSessionId());
        }
    }

    @Override
    public void delete(UUID sessionId) {
        File file = getFile(sessionId);
        if (file.exists() && !file.delete()) {
            logger.error(this.getClass(),"Failed to delete instance file: " + sessionId);
        }
    }

    @Override
    public List<DungeonInstance> loadAll() {
        List<DungeonInstance> result = new ArrayList<>();
        File[] files = dataFolder.listFiles((dir, name) -> name.endsWith(DataFolderConfig.JSON_TYPE));
        if (files == null) return result;

        for (File file : files) {
            try (Reader reader = new FileReader(file)) {
                DungeonInstance instance = gson.fromJson(reader, DungeonInstance.class);
                if (instance == null) continue;
                restoreObjectives(instance);
                result.add(instance);
            } catch (IOException e) {
                logger.error(this.getClass(), "Failed to load instance file: " + file.getName());
            }
        }
        return result;
    }

    /**
     * Restore active objectives từ objectiveStates đã serialize.
     * Không cần callback — ObjectiveFactory.restore() tự xử lý hoàn toàn.
     */
    private void restoreObjectives(DungeonInstance instance) {
        DungeonProgress progress = instance.getProgress();
        if (progress == null) return;

        RoomInstance currentRoom = progress.getCurrentRoom();
        if (currentRoom == null) return;

        List<Map<String, Object>> states = currentRoom.getObjectiveStates();
        if (states == null || states.isEmpty()) return;

        List<IObjective> restored = new ArrayList<>();
        for (Map<String, Object> state : states) {
            try {
                restored.add(ObjectiveFactory.restore(state));
            } catch (Exception e) {
                logger.warn(this.getClass(), "Failed to restore objective type: " + state.get("type"));
            }
        }

        currentRoom.setActiveObjectives(restored);
    }

    private File getFile(UUID sessionId) {
        return new File(dataFolder, DataFolderConfig.DUNGEON_INSTANCE_FILE + sessionId + DataFolderConfig.JSON_TYPE);
    }
}