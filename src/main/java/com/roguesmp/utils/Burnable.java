package com.roguesmp.utils;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public class Burnable {
    private static final Map<Material, Integer> VANILLA_FUELS = new EnumMap<>(Material.class);

    static {
        VANILLA_FUELS.put(Material.LAVA_BUCKET, 20000);   // Đốt được 100 item
        VANILLA_FUELS.put(Material.COAL_BLOCK, 16000);    // Đốt được 80 item
        VANILLA_FUELS.put(Material.DRIED_KELP_BLOCK, 4000); // Đốt được 20 item
        VANILLA_FUELS.put(Material.BLAZE_ROD, 2400);      // Đốt được 12 item
        VANILLA_FUELS.put(Material.COAL, 1600);           // Đốt được 8 item
        VANILLA_FUELS.put(Material.CHARCOAL, 1600);       // Đốt được 8 item
        VANILLA_FUELS.put(Material.STICK, 100);           // Đốt được 0.5 item
        VANILLA_FUELS.put(Material.BAMBOO, 50);           // Đốt được 0.25 item

        for(Material mat: Tag.LOGS.getValues()){
            VANILLA_FUELS.put(mat, 300); //1.5 items
        }
        for (Material mat : Tag.PLANKS.getValues()) {
            VANILLA_FUELS.put(mat, 300);
        }
        for (Material mat : Tag.WOODEN_DOORS.getValues()) {
            VANILLA_FUELS.put(mat, 200); // 1 item
        }
        for (Material mat : Tag.WOODEN_SLABS.getValues()) {
            VANILLA_FUELS.put(mat, 150); // 0.75 item
        }
    }

    public static int getBurnTime(ItemStack item){
        if (item == null || item.isEmpty()) return 0;

        return VANILLA_FUELS.getOrDefault(item.getType(), 0);
    }

    public static boolean isFuel(ItemStack item) {
        return getBurnTime(item) > 0;
    }

    public static Map<Material, Integer> getAllBurnableItems() {return Collections.unmodifiableMap(VANILLA_FUELS);}
}
