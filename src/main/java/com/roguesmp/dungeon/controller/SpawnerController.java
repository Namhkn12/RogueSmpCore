package com.roguesmp.dungeon.controller;

import com.roguesmp.dungeon.controller.response.ControllerResponse;
import com.roguesmp.dungeon.controller.response.Rsc;
import com.roguesmp.dungeon.service.ISpawnerService;
import com.roguesmp.dungeon.ultis.ConsoleLogger;
import com.roguesmp.dungeon.ultis.NameSpaceKeys;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
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

    public ControllerResponse<Void> placeSpawnerAction(BlockPlaceEvent event){
        if(event.getBlock().getType() != Material.SPAWNER) return ControllerResponse.failure("Không phải spawner");
        ItemStack item = event.getItemInHand();
        ItemMeta meta = item.getItemMeta();
        ConsoleLogger.info("XXX", "1");

        if(meta == null){
            ConsoleLogger.info("XXX", "Không tìm thấy meta in hand");
            return ControllerResponse.response(Rsc.NOT_FOUND);
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        NamespacedKey key = NameSpaceKeys.SPAWNER_IID_KEY;
        if(!pdc.has(key, PersistentDataType.STRING)){
            ConsoleLogger.info("XXX", "Không phải spawner custom");
            return ControllerResponse.failure("Không phải spawner custom");
        }
        String templateId = pdc.get(key, PersistentDataType.STRING);
        Block spawner = event.getBlock();
        World world = spawner.getWorld();
        Location loc = spawner.getLocation().clone().add(0.5, 0, 0.5);
        ConsoleLogger.info("XXX", "Bắt đầu tạo marker");

        Marker marker = world.spawn(loc, Marker.class, entity -> {
            entity.setPersistent(true);
            entity.setInvulnerable(true);

            // set PDC templateId vào marker
            NamespacedKey markerKey = NameSpaceKeys.SPAWNER_IID_KEY;
            entity.getPersistentDataContainer().set(markerKey, PersistentDataType.STRING, templateId);
        });

        spawnerService.createSpawnerInstance(marker.getUniqueId(), templateId);
        ConsoleLogger.info("XXX", "Tạo marker kết thúc");

        return ControllerResponse.response(Rsc.SUCCESS);
    }

    public ControllerResponse<Void> spawnerBreakAction(BlockBreakEvent event){
        return ControllerResponse.success("");
    }

    public ControllerResponse<Void> spawnerSpawnAction(SpawnerSpawnEvent event){
        return ControllerResponse.success("");
    }

    public ControllerResponse<Void> spawnerMarkerLoad(EntitiesLoadEvent event){
        if(!event.getWorld().getName().startsWith("dungeon_"))
            return ControllerResponse.response(Rsc.NOT_FOUND);
//        ConsoleLogger.info("[Marker]", "Marker load start");

        for (Entity e : event.getEntities()){
            if(e.getType() == EntityType.MARKER){
                //check psd id
                ConsoleLogger.info("[Marker]", "Tìm thấy 1 marker");

                NamespacedKey nsp = NameSpaceKeys.SPAWNER_IID_KEY;
                PersistentDataContainer pdc = e.getPersistentDataContainer();

                if(pdc.has(nsp, PersistentDataType.STRING)){
                    ConsoleLogger.info("[Marker]", "Bắt đầu apply data to spawner");

                    String templateId = pdc.get(nsp, PersistentDataType.STRING);

                    Block blockBelow = e.getLocation().getBlock();

                    if (blockBelow.getType() != Material.SPAWNER) {
                        ConsoleLogger.info("[Marker]", "Block bên dưới ko phải spawner");
                        e.remove();
                        return ControllerResponse.response(Rsc.BAD_REQUEST);
                    }
                    CreatureSpawner creatureSpawner = (CreatureSpawner) blockBelow.getState();
                    spawnerService.createSpawnerInstance(UUID.randomUUID(), templateId);
                    ConsoleLogger.info("[Marker]", "Create Instance fnishhhh");

                    spawnerService.applyTemplateToSpawner(templateId, creatureSpawner);
                }
            }
        }
        return ControllerResponse.response(Rsc.SUCCESS);
    }
}
