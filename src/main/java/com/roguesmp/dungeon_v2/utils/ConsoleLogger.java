package com.roguesmp.dungeon_v2.utils;

import com.roguesmp.RogueSmpCore;

import java.util.logging.Logger;

public class ConsoleLogger {

    private static Logger getLogger() {
        return RogueSmpCore.getInstance().getLogger();
    }

    // ANSI colors
    private static final String RESET  = "\u001B[0m";
    private static final String RED    = "\u001B[31m";
    private static final String GREEN  = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String CYAN   = "\u001B[36m";
    private static final String GRAY   = "\u001B[90m";
    private static final String BOLD   = "\u001B[1m";

    // Prefix
    public enum Module {
        DUNGEON (CYAN   + "[Dungeon]"  + RESET),
        ECONOMY (YELLOW + "[Economy]"  + RESET),
        DATABASE(RED    + "[Database]" + RESET),
        COMBAT  (BOLD   + "[Combat]"   + RESET),
        CORE    (GREEN  + "[Core]"     + RESET);

        private final String prefix;
        Module(String prefix) { this.prefix = prefix; }
    }

    public static void info(Module module, String message) {
        getLogger().info(module.prefix + " " + CYAN + message + RESET);
    }

    public static void warn(Module module, String message) {
        getLogger().warning(module.prefix + " " + YELLOW + message + RESET);
    }

    public static void success(Module module, String message) {
        getLogger().info(module.prefix + " " + GREEN + BOLD + message + RESET);
    }

    public static void error(Module module, String message) {
        getLogger().severe(module.prefix + " " + RED + BOLD + message + RESET);
    }

    public static void debug(Module module, String message) {
        if (!RogueSmpCore.getInstance().getConfig().getBoolean("debug", false)) return;
        getLogger().info(module.prefix + " " + GRAY + "[debug] " + message + RESET);
    }
}