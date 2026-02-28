package com.roguesmp.registry;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.entity.BaseEntity;
import com.roguesmp.entity.SmpEntity;
import com.roguesmp.utils.Utils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.StringArgument;
import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

public class EntityRegistry {
    private static EntityRegistry INSTANCE;
    public static final String FOLDER_NAME = "entities";

    private final Map<String, BaseEntity> dataMap = new HashMap<>();
    private final RogueSmpCore plugin;

    private EntityRegistry(RogueSmpCore plugin) {
        this.plugin = plugin;
    }

    public @Nullable SmpEntity spawnEntity(String id, Location location) {
        BaseEntity base = dataMap.get(id);
        if (base == null) return null;
        return base.spawn(location);
    }

    public BaseEntity getBaseEntity(String id) {
        return dataMap.get(id);
    }

    public void loadFromFile() {
        File itemsDir = new File(plugin.getDataFolder(), FOLDER_NAME);

        if (!itemsDir.exists() || !itemsDir.isDirectory()) {
            plugin.getLogger().info("No entity folder found, starting with empty registry");
            return;
        }

        File[] files = itemsDir.listFiles((dir, name) -> name.endsWith(".json"));

        if (files == null || files.length == 0) {
            plugin.getLogger().info("Entity folder is empty");
            return;
        }

        for (File file : files) {
            try (Reader reader = new FileReader(file)) {
                BaseEntity entity = Utils.GSON.fromJson(reader, BaseEntity.class);

                if (entity == null || entity.getId() == null) {
                    plugin.getLogger().warning("Invalid entity file: " + file.getName());
                    continue;
                }

                dataMap.put(entity.getId(), entity);

            } catch (Exception e) {
                plugin.getLogger().severe("Failed to load entity file: " + file.getName());
                e.printStackTrace();
            }
        }

        plugin.getLogger().info("Loaded entity registry (" + dataMap.size() + " entries)");
    }

    public static void init(RogueSmpCore plugin) {
        INSTANCE = new EntityRegistry(plugin);
    }

    public static EntityRegistry getInstance() {
        if (INSTANCE == null) {
            throw new RuntimeException("EntityRegistry is null");
        }
        return INSTANCE;
    }

    public static void registerCommand() {
        new CommandAPICommand("smpentity")
                .withArguments(new StringArgument("entity_id"))
                .executesPlayer((player, commandArguments) -> {
                    String id = (String) commandArguments.get("entity_id");
                    BaseEntity base = EntityRegistry.getInstance().getBaseEntity(id);
                    if (base == null) {
                        player.sendMessage("Id not found");
                        return;
                    }
                    base.spawn(player.getLocation());
                })
                .register(RogueSmpCore.getInstance());
    }
}
