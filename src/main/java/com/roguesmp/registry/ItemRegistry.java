package com.roguesmp.registry;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.constant.Attributes;
import com.roguesmp.constant.Enchants;
import com.roguesmp.constant.EquipSlot;
import com.roguesmp.item.BaseItem;
import com.roguesmp.item.component.impl.*;
import com.roguesmp.utils.Utils;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.Nullable;
import java.io.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemRegistry {

    private static ItemRegistry INSTANCE = null;

    public static final String FOLDER_NAME = "items";

    private final RogueSmpCore plugin;
    private final Map<String, BaseItem> dataMap = new HashMap<>();

    private ItemRegistry(RogueSmpCore plugin) {
        this.plugin = plugin;
        //Test item for file gen
        dataMap.put("test",
                new BaseItem("test", Material.STONE_HOE,
                        Map.of("name", new NameComponent("<blue>Test"),
                                "durability", new DurabilityComponent(250),
                                "enchant", new EnchantComponent(
                                        Map.of(Enchants.GREED, 4)
                                ),
                                "attribute", new EquipAttributeComponent(
                                        Map.of(Attributes.SPEED_FLAT, 0.05d, Attributes.MELEE_DAMAGE_BASE, 4d), EquipSlot.MAINHAND))));

        dataMap.put("fallback_item",
                new BaseItem("fallback_item", Material.REDSTONE_BLOCK,
                        Map.of("name", new NameComponent("<red>ERROR"),
                                "description", new DescriptionComponent(List.of("<b><red><!i>Something went wrong if you see this item.")))));
    }

    public @Nullable BaseItem getBaseItem(@NotNull String id) {
        return dataMap.get(id);
    }

    public @Unmodifiable Map<String, BaseItem> getRegistry() {
        return Collections.unmodifiableMap(dataMap);
    }

    public static void init(RogueSmpCore core) {
        INSTANCE = new ItemRegistry(core);
    }

    public static ItemRegistry getInstance() {
        return INSTANCE;
    }

    private File getDataFolder() {
        return new File(plugin.getDataFolder(), FOLDER_NAME);
    }

    public void saveToFile(boolean override) {
        File file = getDataFolder();

        if (!file.exists() && !file.mkdirs()) {
            plugin.getLogger().severe("Failed to create " + FOLDER_NAME +  " directory");
            return;
        }

        plugin.getLogger().info("Saving item registry...");

        for (Map.Entry<String, BaseItem> entry : dataMap.entrySet()) {
            String id = entry.getKey();
            BaseItem item = entry.getValue();

            File child = new File(file, id + ".json");
            if (child.exists() && !override) {
                return;
            }

            try (Writer writer = new FileWriter(child)) {
                Utils.GSON.toJson(item, writer);
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to save item: " + id);
            }
        }
    }

    public void loadFromFile() {
        File itemsDir = new File(plugin.getDataFolder(), FOLDER_NAME);

        if (!itemsDir.exists() || !itemsDir.isDirectory()) {
            plugin.getLogger().info("No items folder found, starting with empty registry");
            return;
        }

        File[] files = itemsDir.listFiles((dir, name) -> name.endsWith(".json"));

        if (files == null || files.length == 0) {
            plugin.getLogger().info("Items folder is empty");
            return;
        }

        for (File file : files) {
            try (Reader reader = new FileReader(file)) {
                BaseItem item = Utils.GSON.fromJson(reader, BaseItem.class);

                if (item == null || item.getId() == null) {
                    plugin.getLogger().warning("Invalid item file: " + file.getName());
                    continue;
                }

                dataMap.put(item.getId(), item);

            } catch (Exception e) {
                plugin.getLogger().severe("Failed to load item file: " + file.getName());
                e.printStackTrace();
            }
        }

        plugin.getLogger().info("Loaded item registry (" + dataMap.size() + " entries)");
    }


}
