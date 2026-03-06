package com.roguesmp.utils;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.roguesmp.RogueSmpCore;
import com.roguesmp.annotation.GsonIgnore;
import com.roguesmp.item.component.ItemComponent;
import com.roguesmp.item.component.serialize.ComponentMapCodec;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class Utils {

    public static final Random RANDOM = Random.from(new SplittableRandom());

    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter( new TypeToken<Map<String, ItemComponent>>() {}.getType(), new ComponentMapCodec())
            .addSerializationExclusionStrategy(new ExclusionStrategy() {
                @Override
                public boolean shouldSkipField(FieldAttributes f) {
                    return f.getAnnotation(GsonIgnore.class) != null;
                }

                @Override
                public boolean shouldSkipClass(Class<?> clazz) {
                    return false;
                }
            })
            .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    public static void runLater(Runnable runnable) {
        new BukkitRunnable() {
            @Override
            public void run() {
                runnable.run();
            }
        }.runTask(RogueSmpCore.getInstance());
    }

    public static void runLater(Runnable runnable, int delay) {
        new BukkitRunnable() {
            @Override
            public void run() {
                runnable.run();
            }
        }.runTaskLater(RogueSmpCore.getInstance(), delay);
    }

    public static void runAsync(Runnable runnable) {
        new BukkitRunnable() {
            @Override
            public void run() {
                runnable.run();
            }
        }.runTaskAsynchronously(RogueSmpCore.getInstance());
    }

    public static void runAsync(Runnable runnable, int delay, int period) {
        new BukkitRunnable() {
            @Override
            public void run() {
                runnable.run();
            }
        }.runTaskTimerAsynchronously(RogueSmpCore.getInstance(), delay, period);
    }

    public static Component fromString(String miniMessage) {
        return MiniMessage.miniMessage().deserialize(miniMessage);
    }

    public static List<Component> fromStrings(List<String> miniMessages) {
        List<Component> components = new ArrayList<>();
        for (String miniMessage : miniMessages) {
            components.add(MiniMessage.miniMessage().deserialize(miniMessage));
        }
        return components;
    }

    public static List<Component> fromStrings(String... miniMessages) {
        List<Component> components = new ArrayList<>();
        for (String miniMessage : miniMessages) {
            components.add(MiniMessage.miniMessage().deserialize(miniMessage));
        }
        return components;
    }

    public static String formatDecimal(double value) {
        return new BigDecimal(value).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private static final double EPSILON = 0.0001;
    public static boolean isEffectiveZero(double value) {
        return value >= -EPSILON && value <= EPSILON;
    }

    private static final int[] VALUES = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
    private static final String[] SYMBOLS = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};

    /**
     * Convert an int to Roman Numeral, if negative return that number as string
     * @param num Value
     * @return String representation of that number using roman numeral
     */
    public static String toRoman(int num) {
        StringBuilder sb = new StringBuilder();

        if (num <= 0) return sb.append(num).toString();

        for (int i = 0; i < VALUES.length; i++) {
            while (num >= VALUES[i]) {
                num -= VALUES[i];
                sb.append(SYMBOLS[i]);
            }
        }

        return sb.toString();
    }

    public static String intToMinuteAndSeconds(int i) {
        int minutes = i / 60;
        int seconds = i % 60;
        if (seconds < 10) {
            return minutes + ":0" + seconds;
        } else {
            return minutes + ":" + seconds;
        }
    }

    public static String toString(Component component) {
        return MiniMessage.miniMessage().serialize(component);
    }

    public static String locationToString(Location loc){
        if(loc == null || loc.getWorld()==null){
            return null;
        }

        return loc.getWorld().getName() + ";" +
                loc.getX() + ";" +
                loc.getY() + ";" +
                loc.getZ() + ";";
    }

    public static Location stringToLocation(String s){
        if(s == null || s.trim().isEmpty()){
            return null;
        }

        String[] parts = s.split(";");

        if(parts.length >=4){
            try{
                World world = Bukkit.getWorld(parts[0]);
                if(world == null){
                    Bukkit.getLogger().warning("Cannot found world: " + parts[0] + " when parsing location!");
                    return null;
                }

                double x = Double.parseDouble(parts[1]);
                double y = Double.parseDouble(parts[2]);
                double z = Double.parseDouble(parts[3]);

                float yaw = parts.length > 4 ? Float.parseFloat(parts[4]) : 0f;
                float pitch = parts.length > 5 ? Float.parseFloat(parts[5]) : 0f;

                return new Location(world, x, y, z, yaw, pitch);
            }
            catch(NumberFormatException e){
                Bukkit.getLogger().severe("Lỗi định dạng số khi parse Location: " + s);
            }
        }

        return null;
    }

}
