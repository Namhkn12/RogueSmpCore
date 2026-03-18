package com.roguesmp.dungeon.controller;

import com.roguesmp.dungeon.controller.response.ControllerResponse;
import com.roguesmp.dungeon.service.ISpawnerService;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Marker;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class SpawnerController {
    private final ISpawnerService spawnerService;

    public SpawnerController(ISpawnerService spawnerService) {
        this.spawnerService = spawnerService;
    }

    public ControllerResponse<Void> placeSpawnerAction(BlockPlaceEvent event){
        if(event.getBlock().getType() != Material.SPAWNER) return ControllerResponse.failure("Không phải spawner");
        ItemStack item = event.getItemInHand();
        ItemMeta meta = item.getItemMeta();
        if(meta == null){
            return ControllerResponse.failure("Không có meta");
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        NamespacedKey key = new NamespacedKey("_", "spawner_template_id");
        if(!pdc.has(key, PersistentDataType.STRING)){
            return ControllerResponse.failure("Không phải spawner custom");
        }
        String templateId = pdc.get(key, PersistentDataType.STRING);
        Block spawner = event.getBlock();
        World world = spawner.getWorld();
        Location loc = spawner.getLocation().clone().add(0.5, 0, 0.5);

        Marker marker = world.spawn(loc, Marker.class, entity -> {
            entity.setPersistent(true);
            entity.setInvulnerable(true);

            // set PDC templateId vào marker
            NamespacedKey markerKey = new NamespacedKey("_", "spawner_template_id");
            entity.getPersistentDataContainer().set(markerKey, PersistentDataType.STRING, templateId);
        });

        spawnerService.createSpawnerInstance(marker.getUniqueId(), templateId);
        return ControllerResponse.success("Đặt custom spawner thành công");
    }

    public ControllerResponse<Void> spawnerBreakAction(BlockBreakEvent event){
        return ControllerResponse.success("");
    }

    public ControllerResponse<Void> spawnerSpawnAction(SpawnerSpawnEvent event){
        return ControllerResponse.success("");
    }
}
