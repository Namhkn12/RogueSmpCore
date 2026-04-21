package com.roguesmp.dungeon.service.impl;

import com.roguesmp.dungeon.data.definition.objective.BaseObjective;
import com.roguesmp.dungeon.data.definition.objective.CompletionScope;
import com.roguesmp.dungeon.data.definition.objective.IObjective;
import com.roguesmp.dungeon.data.definition.objective.factory.ObjectiveConfig;
import com.roguesmp.dungeon.data.definition.objective.factory.ObjectiveFactory;
import com.roguesmp.dungeon.data.definition.room.Room;
import com.roguesmp.dungeon.data.definition.room.roomevent.BaseRoomEvent;
import com.roguesmp.dungeon.data.definition.room.roomevent.RoomEvent;
import com.roguesmp.dungeon.data.definition.room.roomevent.RoomEventContext;
import com.roguesmp.dungeon.data.definition.room.roomevent.factory.RoomEventConfig;
import com.roguesmp.dungeon.data.definition.room.roomevent.factory.RoomEventFactory;
import com.roguesmp.dungeon.data.runtime.DungeonInstance;
import com.roguesmp.dungeon.data.runtime.Party;
import com.roguesmp.dungeon.data.runtime.RoomInstance;
import com.roguesmp.dungeon.manager.InstanceManager;
import com.roguesmp.dungeon.manager.RoomManager;
import com.roguesmp.dungeon.manager.SpawnerInstanceManager;
import com.roguesmp.dungeon.service.IPartyService;
import com.roguesmp.dungeon.service.IRegionService;
import com.roguesmp.dungeon.utils.PdcUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.*;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.BoundingBox;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.roguesmp.dungeon.utils.NameSpaceKeys.SPAWNER_IID_KEY;
import static com.roguesmp.dungeon.utils.NameSpaceKeys.SPAWNER_TID_KEY;

public class RoomRuntimeService {

    private final RoomManager roomManager;
    private final InstanceManager instanceManager;
    private final IPartyService partyService;
    private RoomCompletionService completionService;
    private final IRegionService regionService;
    private final SpawnerInstanceManager spawnerInstanceManager;

    public RoomRuntimeService(RoomManager roomManager, InstanceManager instanceManager,
                              IPartyService partyService, IRegionService regionService, SpawnerInstanceManager spawnerInstanceManager) {
        this.roomManager = roomManager;
        this.instanceManager = instanceManager;
        this.partyService = partyService;
        this.regionService = regionService;
        this.spawnerInstanceManager = spawnerInstanceManager;
    }

    /** Setter injection để phá vòng tròn RoomRuntime <-> RoomCompletion */
    public void setCompletionService(RoomCompletionService completionService) {
        this.completionService = completionService;
    }

    public void bindCurrentRoomRuntime(DungeonInstance instance, Party party) {
        if (instance == null || instance.getProgress() == null || party == null) return;
        RoomInstance roomInstance = instance.getProgress().getCurrentRoom();
        if (roomInstance == null) return;
        Room roomTemplate = roomManager.get(roomInstance.getRoomId());
        if (roomTemplate == null) return;
        setupRoomRuntime(roomTemplate, roomInstance, party);
    }

    public void setupRoomRuntime(Room roomTemplate, RoomInstance roomInstance, Party party) {
        prepareObjectives(roomTemplate, roomInstance, party);
        prepareRoomEvents(roomTemplate, roomInstance, party);
        if (roomInstance.getActiveObjectives() == null || roomInstance.getActiveObjectives().isEmpty()) {
            roomInstance.setCompleted(true);
        }
    }

    public void triggerRoomStart(RoomInstance roomInstance) {
        if (roomInstance == null || roomInstance.getActiveRoomEvents() == null) return;
        roomInstance.getActiveRoomEvents().forEach(RoomEvent::onRoomStart);
    }

    public void triggerRoomTick(RoomInstance roomInstance) {
        if (roomInstance == null || roomInstance.getActiveRoomEvents() == null) return;
        roomInstance.getActiveRoomEvents().forEach(RoomEvent::onRoomPlay);
    }

    public void triggerRoomEnd(RoomInstance roomInstance) {
        if (roomInstance == null || roomInstance.getActiveRoomEvents() == null) return;
        roomInstance.getActiveRoomEvents().forEach(RoomEvent::onRoomEnd);
    }

    private void prepareObjectives(Room roomTemplate, RoomInstance roomInstance, Party party) {
        List<IObjective> objectives = roomInstance.getActiveObjectives();
        if (objectives == null || objectives.isEmpty()) {
            objectives = new ArrayList<>();
            for (ObjectiveConfig config : roomTemplate.getObjectives()) {
                objectives.add(ObjectiveFactory.create(config));
            }
            roomInstance.setActiveObjectives(objectives);
        }

        for (IObjective objective : objectives) {
            if (objective instanceof BaseObjective base) {
                base.setCallback(o -> {
                    DungeonInstance instance = instanceManager.get(party.getInstanceId());
                    if (instance == null) return;
                    int gained = base.getScore();
                    instance.getProgress().setScore(instance.getProgress().getScore() + gained);

                    if (base.getCompletionScope() == CompletionScope.DUNGEON) {
                        completionService.checkDungeonCompletion(instance, party);
                    } else {
                        completionService.checkRoomCompletion(instance, roomInstance, party);
                    }
                    clearEntitiesInDungeon(instance);
                });
            }
        }
    }

    private void prepareRoomEvents(Room roomTemplate, RoomInstance roomInstance, Party party) {
        List<RoomEvent> events = roomInstance.getActiveRoomEvents();
        if (events == null || events.isEmpty()) {
            events = new ArrayList<>();
            for (RoomEventConfig config : roomTemplate.getRoomEvents()) {
                events.add(RoomEventFactory.create(config));
            }
            roomInstance.setActiveRoomEvents(events);
        }

        for (RoomEvent event : events) {
            if (event instanceof BaseRoomEvent base) {
                base.setContext(new RoomEventContext(roomInstance, party, partyService));
                base.setCallback((roomEvent, phase) -> {
                    /*Hook if needed*/
                });
            }
        }
    }

    public void clearEntitiesInDungeon(DungeonInstance instance) {
        BoundingBox box = instance.getProgress().getCurrentRoom().getBounds().toBukkit();
        String worldName = regionService.getRegionById(instance.getSession().getRegionId()).getWorldName();
        World world = Bukkit.getWorld(worldName);
        if (world == null) return;

        for (Entity entity : world.getNearbyEntities(box)) {
            if (entity instanceof Player) continue;
            if (entity instanceof Item) continue;
            if (entity instanceof ExperienceOrb) continue;

            if (entity instanceof Interaction || entity.getType() == EntityType.MARKER) {
                PdcUtil.get(entity, SPAWNER_TID_KEY, PersistentDataType.STRING).ifPresent(tid -> {
                    CreatureSpawner spawner = findNearbySpawner(entity.getLocation().getBlock());
                    if (spawner == null) return;

                    PdcUtil.get(spawner.getPersistentDataContainer(), SPAWNER_IID_KEY, PersistentDataType.STRING)
                            .ifPresent(spawnerInstanceManager::remove);

                    spawner.setSpawnedType(null);
                    spawner.setSpawnCount(0);
                    spawner.setMaxNearbyEntities(0);
                    spawner.setDelay(-1);
                    spawner.getPotentialSpawns().clear();

                    PersistentDataContainer spawnerPdc = spawner.getPersistentDataContainer();
                    spawnerPdc.remove(SPAWNER_IID_KEY);
                    spawnerPdc.remove(SPAWNER_TID_KEY);
                    spawner.update(true, false);
                });
            }

            entity.remove();
        }
    }

    private CreatureSpawner findNearbySpawner(Block origin) {
        if (origin.getType() == Material.SPAWNER) {
            return (CreatureSpawner) origin.getState();
        }

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    Block nearby = origin.getRelative(dx, dy, dz);
                    if (nearby.getType() == Material.SPAWNER) {
                        return (CreatureSpawner) nearby.getState();
                    }
                }
            }
        }

        return null;
    }
}
