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

import java.util.List;
import java.util.UUID;

public class SpawnerController {
    private final ISpawnerService spawnerService;
    private final Plugin plugin;

    public SpawnerController(ISpawnerService spawnerService, Plugin plugin) {
        this.spawnerService = spawnerService;
        this.plugin = plugin;
    }

    public ActionResult<Void> handleSpawnerPlace(Block block, String templateId) {
        World world = block.getWorld();
        Location loc = block.getLocation().clone().add(0.5, 0, 0.5);
        CreatureSpawner spawner = (CreatureSpawner) block.getState();

        Marker marker = world.spawn(loc, Marker.class, entity -> {
            entity.setPersistent(true);
            entity.setInvulnerable(true);
            entity.getPersistentDataContainer().set(NameSpaceKeys.SPAWNER_TID_KEY, PersistentDataType.STRING, templateId);
        });

        try {
            String iid = marker.getUniqueId().toString();
            spawnerService.createInstance(iid, templateId);
            spawner.getPersistentDataContainer().set(NameSpaceKeys.SPAWNER_IID_KEY, PersistentDataType.STRING, iid);
            spawner.update();
            return ActionResult.ok("Spawner applied successfully");
        } catch (SpawnerNotFoundException e) {
            marker.remove();
            return ActionResult.failed("Spawner template not found: " + templateId);
        } catch (BaseException e) {
            marker.remove();
            GlobalException.handle(e);
            return ActionResult.failed("System error");
        }
    }

    public ActionResult<Void> handleSpawnerAppear(Marker marker, String templateId) {
        Block blockBelow = marker.getLocation().getBlock();
        if (blockBelow.getType() != Material.SPAWNER) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> retryRegisterSpawner(marker, templateId), 10L);
            return ActionResult.invalid("Spawner block not ready, scheduled retry");
        }
        return registerSpawner(marker, blockBelow, templateId);
    }

    public ActionResult<Void> handleSpawnerBreak(Block block, BlockBreakEvent event) {
        if (!(block.getState() instanceof CreatureSpawner spawner))
            return ActionResult.invalid("Not a spawner block");

        PersistentDataContainer pdc = spawner.getPersistentDataContainer();
        if (!pdc.has(NameSpaceKeys.SPAWNER_IID_KEY, PersistentDataType.STRING))
            return ActionResult.failed("Not a custom spawner");

        String iid = pdc.get(NameSpaceKeys.SPAWNER_IID_KEY, PersistentDataType.STRING);
        try {
            SpawnerInstance instance = spawnerService.getInstance(iid);
            boolean shouldCancel = instance.getBehaviors().stream()
                    .map(b -> b.onBreak(event))
                    .anyMatch(broken -> !broken);
            event.setCancelled(shouldCancel);
            return ActionResult.ok("Spawner break handled");
        } catch (SpawnerNotFoundException e) {
            return ActionResult.failed("Spawner instance not found");
        } catch (BaseException e) {
            GlobalException.handle(e);
            return ActionResult.failed("System error");
        }
    }

    public ActionResult<Void> handleMarkerLoad(List<Entity> entities, String worldName) {
        for (Entity entity : entities) {
            if (entity.getType() != EntityType.MARKER) continue;

            PersistentDataContainer pdc = entity.getPersistentDataContainer();
            if (!pdc.has(NameSpaceKeys.SPAWNER_TID_KEY, PersistentDataType.STRING)) continue;

            try {
                String templateId = pdc.get(NameSpaceKeys.SPAWNER_TID_KEY, PersistentDataType.STRING);
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

                spawnerService.createInstance(iid, templateId);
                spawnerService.applyTemplate(templateId, creatureSpawner);
            } catch (BaseException e) {
                GlobalException.handle(e);
            }
        }
        return ActionResult.ok("Markers loaded");
    }

    private ActionResult<Void> registerSpawner(Marker marker, Block block, String templateId) {
        try {
            String iid = UUID.randomUUID().toString();
            CreatureSpawner creatureSpawner = (CreatureSpawner) block.getState();
            creatureSpawner.getPersistentDataContainer().set(NameSpaceKeys.SPAWNER_IID_KEY, PersistentDataType.STRING, iid);
            creatureSpawner.update();
            spawnerService.createInstance(iid, templateId);
            spawnerService.applyTemplate(templateId, creatureSpawner);
            return ActionResult.ok("Spawner registered");
        } catch (SpawnerNotFoundException e) {
            return ActionResult.failed("Template not found");
        } catch (BaseException e) {
            GlobalException.handle(e);
            return ActionResult.failed("System error");
        }
    }

    private void retryRegisterSpawner(Marker marker, String templateId) {
        if (!marker.isValid()) return;
        Block blockBelow = marker.getLocation().getBlock();
        if (blockBelow.getType() != Material.SPAWNER) {
            marker.remove();
            return;
        }
        registerSpawner(marker, blockBelow, templateId);
    }
}
