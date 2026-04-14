package com.roguesmp.dungeon_v2.repository.impl;

import com.google.gson.Gson;
import com.roguesmp.dungeon_v2.config.DataFolderConfig;
import com.roguesmp.dungeon_v2.data.definition.objective.IObjective;
import com.roguesmp.dungeon_v2.data.definition.objective.factory.ObjectiveFactory;
import com.roguesmp.dungeon_v2.data.definition.room.roomevent.RoomEvent;
import com.roguesmp.dungeon_v2.data.definition.room.roomevent.factory.RoomEventFactory;
import com.roguesmp.dungeon_v2.data.runtime.DungeonInstance;
import com.roguesmp.dungeon_v2.data.runtime.RoomInstance;
import com.roguesmp.dungeon_v2.data.runtime.session.DungeonProgress;
import com.roguesmp.dungeon_v2.repository.IInstanceRepository;
import com.roguesmp.dungeon_v2.utils.Log4Craft_;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class InstanceRepository implements IInstanceRepository {

    private final Log4Craft_ logger;
    private final Gson gson;
    private final File dataFolder;

    public InstanceRepository(Plugin plugin, Gson gson, Log4Craft_ logger) {
        this.logger = logger;
        this.gson = gson;
        this.dataFolder = new File(plugin.getDataFolder(), DataFolderConfig.getDungeonSessionFolder());
        if (!dataFolder.exists()) dataFolder.mkdirs();
    }

    @Override
    public void save(DungeonInstance instance) {
        if (instance == null || instance.getSession() == null) return;
        String sessionId = instance.getSession().getSessionId();
        if (sessionId == null || sessionId.isBlank()) return;

        File file = getFile(sessionId);
        try (Writer writer = new FileWriter(file)) {
            gson.toJson(instance, writer);
        } catch (IOException e) {
            logger.warn(this.getClass(), "Failed to save instance: " + sessionId);
        }
    }

    @Override
    public boolean delete(String sessionId) {
        File file = getFile(sessionId);
        if (file.exists() && !file.delete()) {
            logger.error(this.getClass(), "Failed to delete instance file: " + sessionId);
            return false;
        }
        return true;
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
                if (instance.getSession() == null || instance.getSession().getSessionId() == null || instance.getSession().getSessionId().isBlank()) {
                    logger.warn(this.getClass(), "Skip instance file with invalid session id: " + file.getName());
                    continue;
                }
                restoreRoomRuntime(instance);
                result.add(instance);
            } catch (IOException e) {
                logger.error(this.getClass(), "Failed to load instance file: " + file.getName());
            }
        }
        return result;
    }

    private void restoreRoomRuntime(DungeonInstance instance) {
        DungeonProgress progress = instance.getProgress();
        if (progress == null) return;

        RoomInstance currentRoom = progress.getCurrentRoom();
        if (currentRoom == null) return;

        List<IObjective> restoredObjectives = restoreObjectives(currentRoom);
        if (!restoredObjectives.isEmpty()) {
            currentRoom.setActiveObjectives(restoredObjectives);
        }

        List<RoomEvent> restoredEvents = restoreRoomEvents(currentRoom);
        if (!restoredEvents.isEmpty()) {
            currentRoom.setActiveRoomEvents(restoredEvents);
        }
    }

    private List<IObjective> restoreObjectives(RoomInstance room) {
        List<Map<String, Object>> states = room.getObjectiveStates();
        if (states == null || states.isEmpty()) return List.of();

        List<IObjective> restored = new ArrayList<>();
        for (Map<String, Object> state : states) {
            try {
                restored.add(ObjectiveFactory.restore(state));
            } catch (Exception e) {
                logger.warn(this.getClass(), "Failed to restore objective type: " + state.get("type"));
            }
        }
        return restored;
    }

    private List<RoomEvent> restoreRoomEvents(RoomInstance room) {
        List<Map<String, Object>> states = room.getRoomEventStates();
        if (states == null || states.isEmpty()) return List.of();

        List<RoomEvent> restored = new ArrayList<>();
        for (Map<String, Object> state : states) {
            try {
                restored.add(RoomEventFactory.restore(state));
            } catch (Exception e) {
                logger.warn(this.getClass(), "Failed to restore room event type: " + state.get("type"));
            }
        }
        return restored;
    }

    private File getFile(String sessionId) {
        return new File(dataFolder, DataFolderConfig.DUNGEON_INSTANCE_FILE + sessionId + DataFolderConfig.JSON_TYPE);
    }
}
