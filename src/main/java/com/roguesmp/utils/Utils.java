package com.roguesmp.utils;

import com.roguesmp.RogueSmpCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.scheduler.BukkitRunnable;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.SplittableRandom;

public class Utils {

    private static final DecimalFormat FORMAT;
    public static final Random RANDOM = Random.from(new SplittableRandom());

    static {
        FORMAT = new DecimalFormat("#.##");
        FORMAT.setRoundingMode(RoundingMode.DOWN);
    }

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

    public static String formatValue(double value) {
        return FORMAT.format(value);
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

}
