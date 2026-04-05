package com.roguesmp.dungeon_v2.utils;

import com.roguesmp.RogueSmpCore;
import org.bukkit.Bukkit;

public class Log4Craft {

    private static final String PREFIX     = "§b[Dungeon]§r";

    private static final String COLOR_INFO    = "§7";
    private static final String COLOR_SUCCESS = "§a";
    private static final String COLOR_WARN    = "§e";
    private static final String COLOR_ERROR   = "§c";
    private static final String COLOR_DEBUG   = "§8";
    private static final String COLOR_RESET   = "§r";

    public static void info(String message) {
        send(COLOR_INFO + message);
    }

    public static void success(String message) {
        send(COLOR_SUCCESS + message);
    }

    public static void warn(String message) {
        send(COLOR_WARN + message);
    }

    public static void error(String message) {
        send(COLOR_ERROR + message);
    }

    public static void debug(String message) {
        if (!RogueSmpCore.getInstance().getConfig().getBoolean("debug", false)) return;
        send(COLOR_DEBUG + "[DEBUG] " + message);
    }

    public static void fire(String message, Throwable t) {
        RogueSmpCore.getInstance().getLogger()
                .log(java.util.logging.Level.SEVERE, "[Dungeon] " + message, t);
    }

    private static void send(String message) {
        Bukkit.getConsoleSender().sendMessage(PREFIX + " " + message + COLOR_RESET);
    }
}