package com.roguesmp.dungeon_v2.controller_;


import com.roguesmp.dungeon_v2.data.runtime.SpawnerInstance;
import com.roguesmp.dungeon_v2.exception.BaseException;
import com.roguesmp.dungeon_v2.exception.GlobalException;
import com.roguesmp.dungeon_v2.exception.impl.spawner.SpawnerNotFoundException;
import com.roguesmp.dungeon_v2.manager.SpawnerInstanceManager;
import com.roguesmp.dungeon_v2.manager.SpawnerManager;
import com.roguesmp.dungeon_v2.service.ISpawnerService;
import com.roguesmp.dungeon_v2.task.TaskScheduler;
import com.roguesmp.dungeon_v2.utils.NameSpaceKeys;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Marker;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.UUID;

public class SpawnerEventController {

    private final ISpawnerService spawnerService;
    private final SpawnerManager spawnerManager;
    private final SpawnerInstanceManager spawnerInstanceManager;
    private final TaskScheduler taskScheduler;

    public SpawnerEventController(ISpawnerService spawnerService, SpawnerManager spawnerManager, SpawnerInstanceManager spawnerInstanceManager, TaskScheduler taskScheduler) {
        this.spawnerService = spawnerService;
        this.spawnerManager = spawnerManager;
        this.spawnerInstanceManager = spawnerInstanceManager;
        this.taskScheduler = taskScheduler;
    }

    public boolean handleSpawnerBreak(Block block, Player player){
        if (!(block.getState() instanceof CreatureSpawner spawner))
            return true;

        PersistentDataContainer pdc = spawner.getPersistentDataContainer();
        if (!pdc.has(com.roguesmp.dungeon.utils.NameSpaceKeys.SPAWNER_IID_KEY, PersistentDataType.STRING))
            return true;

        String iid = pdc.get(com.roguesmp.dungeon.utils.NameSpaceKeys.SPAWNER_IID_KEY, PersistentDataType.STRING);
        try {
            SpawnerInstance instance = spawnerInstanceManager.get(iid);
            return instance.getPipeline().runBreak(player);
        } catch (SpawnerNotFoundException e) {
            return true;
        } catch (BaseException e) {
            GlobalException.handle(e);
            return true;
        }
    }

    public void handleSpawnerLoad(List<Entity> entities, String worldName){
        for (Entity entity : entities) {
            if (entity.getType() != EntityType.MARKER) continue;

            PersistentDataContainer pdc = entity.getPersistentDataContainer();
            if (!pdc.has(NameSpaceKeys.SPAWNER_TID_KEY, PersistentDataType.STRING)) continue;

            try {
                String siid = pdc.get(NameSpaceKeys.SPAWNER_TID_KEY, PersistentDataType.STRING);
                Block blockBelow = entity.getLocation().getBlock();

                if (blockBelow.getType() != Material.SPAWNER) {
                    entity.remove();
                    continue;
                }

                String iid = pdc.has(NameSpaceKeys.SPAWNER_IID_KEY, PersistentDataType.STRING)
                        ? pdc.get(NameSpaceKeys.SPAWNER_IID_KEY, PersistentDataType.STRING)
                        : UUID.randomUUID().toString();

                entity.getPersistentDataContainer().set(NameSpaceKeys.SPAWNER_IID_KEY, PersistentDataType.STRING, iid);
                CreatureSpawner creatureSpawner = (CreatureSpawner) blockBelow.getState();
                creatureSpawner.getPersistentDataContainer().set(NameSpaceKeys.SPAWNER_IID_KEY, PersistentDataType.STRING, iid);
                creatureSpawner.update();

                spawnerService.createInstance(iid, siid);
                spawnerService.applyTemplate(siid, creatureSpawner);
            } catch (BaseException e) {
                GlobalException.handle(e);
            }
        }
    }

    public void handleSpawnerAppear(Marker marker){
        PersistentDataContainer pdc = marker.getPersistentDataContainer();
        String sid = pdc.get(NameSpaceKeys.SPAWNER_TID_KEY, PersistentDataType.STRING);
        if(sid == null || sid.isEmpty()) return;
        Block block = marker.getLocation().getBlock();
        if(block.getType() != Material.SPAWNER){
            taskScheduler.runLater(10L, () -> retryRegisterSpawner(marker, sid));
        }else{
            registerSpawner(block, sid);
        }
    }

    public void handleSpawnerSpawn(){

    }

    /*Admin api*/
    public void handleGetSpawner(){

    }

    public void handlePlaceSpawner(Block block, String sid){
        World world = block.getWorld();
        Location loc = block.getLocation().clone().add(0.5, 0, 0.5);

        /*Spawn a marker at the middle of spawner block*/
        world.spawn(loc, Marker.class, entity -> {
            entity.setPersistent(true);
            entity.setInvulnerable(true);
            entity.getPersistentDataContainer().set(NameSpaceKeys.SPAWNER_TID_KEY, PersistentDataType.STRING, sid);
        });
    }

    /*Helper*/
    private void registerSpawner(Block block, String templateId) {
        try {
            String iid = UUID.randomUUID().toString();
            CreatureSpawner creatureSpawner = (CreatureSpawner) block.getState();
            creatureSpawner.getPersistentDataContainer().set(NameSpaceKeys.SPAWNER_IID_KEY, PersistentDataType.STRING, iid);
            creatureSpawner.update();
            spawnerService.createInstance(iid, templateId);
            spawnerService.applyTemplate(templateId, creatureSpawner);
        } catch (BaseException e) {
            GlobalException.handle(e);
        }
    }

    private void retryRegisterSpawner(Marker marker, String templateId) {
        if (!marker.isValid()) return;
        Block blockBelow = marker.getLocation().getBlock();
        if (blockBelow.getType() != Material.SPAWNER) {
            marker.remove();
            return;
        }
        registerSpawner(blockBelow, templateId);
    }
}
