package com.roguesmp.dungeon.controller;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.roguesmp.dungeon.dto.ActionResult;
import com.roguesmp.dungeon.exception.BaseException;
import com.roguesmp.dungeon.exception.GlobalException;
import com.roguesmp.dungeon.exception.impl.spawner.SpawnerNotFoundException;
import com.roguesmp.dungeon.instance.SpawnerInstance;
import com.roguesmp.dungeon.service.ISpawnerService;
import com.roguesmp.dungeon.utils.NameSpaceKeys;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Marker;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

public class SpawnerController {
    private final ISpawnerService spawnerService;
    private final Plugin plugin;

    public SpawnerController(ISpawnerService spawnerService, Plugin plugin) {
        this.spawnerService = spawnerService;
        this.plugin = plugin;
    }

    public ActionResult<Void> placeSpawnerAction(BlockPlaceEvent event){
        if(event.getBlock().getType() != Material.SPAWNER) return ActionResult.invalid(null);
        ItemStack item = event.getItemInHand();
        ItemMeta meta = item.getItemMeta();

        if(meta == null){
            return ActionResult.failed("Meta data is null");
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        NamespacedKey key = NameSpaceKeys.SPAWNER_TID_KEY;
        if(!pdc.has(key, PersistentDataType.STRING)){
            return ActionResult.failed("Spawner placed does not contain template");
        }
        String templateId = pdc.get(key, PersistentDataType.STRING);

        Block block = event.getBlock();
        CreatureSpawner spawner = (CreatureSpawner) block.getState();
        World world = block.getWorld();
        Location loc = block.getLocation().clone().add(0.5, 0, 0.5);

        Marker marker = world.spawn(loc, Marker.class, entity -> {
            entity.setPersistent(true);
            entity.setInvulnerable(true);
            entity.getPersistentDataContainer().set(NameSpaceKeys.SPAWNER_TID_KEY, PersistentDataType.STRING, templateId);
        });

        try {
            String iid = marker.getUniqueId().toString();
            spawnerService.createSpawnerInstance(iid, templateId);
            spawner.getPersistentDataContainer().set(NameSpaceKeys.SPAWNER_IID_KEY, PersistentDataType.STRING, iid);
            spawner.update();
            return ActionResult.ok("Spawner applied data successfully");
        } catch (SpawnerNotFoundException e) {
            marker.remove();
            return ActionResult.failed("Spawner template not found: " + templateId);
        } catch (BaseException e) {
            marker.remove();
            GlobalException.handle(e);
            return ActionResult.failed("System error, cannot apply custom spawner data");
        }
    }

    public ActionResult<Void> spawnerAddToWorld(EntityAddToWorldEvent event){
        if(!event.getWorld().getName().startsWith("dungeon_"))
            return ActionResult.invalid("This action can");
        Entity entity = event.getEntity();
        if(!(entity instanceof Marker marker))
            return ActionResult.invalid("Entity is not instance of Marker");
        NamespacedKey nsp = NameSpaceKeys.SPAWNER_TID_KEY;
        PersistentDataContainer pdc = entity.getPersistentDataContainer();

        String templateId = pdc.get(nsp, PersistentDataType.STRING);
        Block blockBelow = entity.getLocation().getBlock();
        if (blockBelow.getType() != Material.SPAWNER) {
            // Retry sau 10 tick thay vì bỏ qua
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                retryRegisterSpawner(marker, templateId);
            }, 10L);
            return ActionResult.invalid("Spawner block not ready, scheduled retry");
        }

        try {
            String iid = UUID.randomUUID().toString();
            CreatureSpawner creatureSpawner = (CreatureSpawner) blockBelow.getState();
            creatureSpawner.getPersistentDataContainer().set(NameSpaceKeys.SPAWNER_IID_KEY, PersistentDataType.STRING, iid);
            creatureSpawner.update();

            spawnerService.createSpawnerInstance(iid, templateId);
            spawnerService.applyTemplateToSpawner(templateId, creatureSpawner);
            return ActionResult.ok("Spawner custom applied successfully");
        } catch (SpawnerNotFoundException e) {
            return ActionResult.failed("Spawner custom applied fail");
        } catch (BaseException e) {
            GlobalException.handle(e);
            return ActionResult.failed("System error");
        }
    }

    private void retryRegisterSpawner(Marker marker, String templateId) {
        if (!marker.isValid()) return; // marker đã bị remove

        Block blockBelow = marker.getLocation().getBlock();
        if (blockBelow.getType() != Material.SPAWNER) {
            marker.remove(); // sau 10 tick vẫn không có spawner → dọn dẹp
            return;
        }

        try {
            String iid = UUID.randomUUID().toString();
            CreatureSpawner creatureSpawner = (CreatureSpawner) blockBelow.getState();
            creatureSpawner.getPersistentDataContainer()
                    .set(NameSpaceKeys.SPAWNER_IID_KEY, PersistentDataType.STRING, iid);
            creatureSpawner.update();
            spawnerService.createSpawnerInstance(iid, templateId);
            spawnerService.applyTemplateToSpawner(templateId, creatureSpawner);
        } catch (BaseException e) {
            GlobalException.handle(e);
        }
    }

    public ActionResult<Void> spawnerBreakAction(BlockBreakEvent event){
        String worldName = event.getPlayer().getWorld().getName();
        if(!worldName.startsWith("dungeon_")){
            return ActionResult.invalid("This action cannot be executed in this world");
        }
        if(!(event.getBlock().getState() instanceof CreatureSpawner spawner)) {
            return ActionResult.invalid("This block is not spawner block");
        }

        PersistentDataContainer pcd = spawner.getPersistentDataContainer();

        boolean hasKey = pcd.has(NameSpaceKeys.SPAWNER_IID_KEY, PersistentDataType.STRING);
        if (!hasKey) return ActionResult.failed("This spawner is not custom spawner");

        String iid = pcd.get(NameSpaceKeys.SPAWNER_IID_KEY, PersistentDataType.STRING);
        try {
            SpawnerInstance instance = spawnerService.getSpawnerInstance(iid);
            boolean shouldCancel = instance.getBehaviors().stream()
                    .map(behavior -> behavior.onBreak(event))
                    .anyMatch(broken -> !broken);
            event.setCancelled(shouldCancel);
            return ActionResult.ok("Spawner break event passed");
        } catch (SpawnerNotFoundException e) {
            return ActionResult.failed("Spawner instance was not found");
        } catch (BaseException e) {
            GlobalException.handle(e);
            return ActionResult.failed("System error");
        }
    }

    public ActionResult<Void> spawnerSpawnAction(SpawnerSpawnEvent event){
        return ActionResult.ok("");
    }

    public ActionResult<Void> spawnerMarkerLoad(EntitiesLoadEvent event){
        if(!event.getWorld().getName().startsWith("dungeon_"))
            return ActionResult.invalid("This action cannot be executed in this world");

        for (Entity entity : event.getEntities()){
            if(entity.getType() == EntityType.MARKER){
                NamespacedKey nsp = NameSpaceKeys.SPAWNER_TID_KEY;
                PersistentDataContainer pdc = entity.getPersistentDataContainer();

                if(pdc.has(nsp, PersistentDataType.STRING)){
                    try{
                        String templateId = pdc.get(nsp, PersistentDataType.STRING);

                        Block blockBelow = entity.getLocation().getBlock();

                        if (blockBelow.getType() != Material.SPAWNER) {
                            entity.remove();
                            continue;
                        }
                        NamespacedKey iidKey = NameSpaceKeys.SPAWNER_IID_KEY;
                        String iid;
                        if (pdc.has(iidKey, PersistentDataType.STRING)) {
                            iid = pdc.get(iidKey, PersistentDataType.STRING);
                        } else {
                            iid = UUID.randomUUID().toString();
                            entity.getPersistentDataContainer().set(iidKey, PersistentDataType.STRING, iid);
                        }
                        CreatureSpawner creatureSpawner = (CreatureSpawner) blockBelow.getState();
                        creatureSpawner.getPersistentDataContainer().set(NameSpaceKeys.SPAWNER_IID_KEY, PersistentDataType.STRING, iid);
                        creatureSpawner.update();
                        spawnerService.createSpawnerInstance(iid, templateId);
                        spawnerService.applyTemplateToSpawner(templateId, creatureSpawner);
                    } catch (BaseException e){
                        GlobalException.handle(e);
                    }
                }
            }
        }
        return ActionResult.ok("Marker entity all loaded");
    }
}
