package com.roguesmp.dungeon.utils;

import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Collection;

public class DungeonEcho {

    private static final String COLOR_SUCCESS = "§a";
    private static final String COLOR_WARN    = "§e";
    private static final String COLOR_ERROR   = "§c";
    private static final String COLOR_INFO    = "§7";
    private static final String COLOR_RESET   = "§r";

    private static final String PREFIX = "§b[Dungeon]§r ";

    /*Private message*/

    public static void success(Player player, String message) {
        player.sendMessage(PREFIX + COLOR_SUCCESS + message + COLOR_RESET);
    }

    public static void warn(Player player, String message) {
        player.sendMessage(PREFIX + COLOR_WARN + message + COLOR_RESET);
    }

    public static void error(Player player, String message) {
        player.sendMessage(PREFIX + COLOR_ERROR + message + COLOR_RESET);
    }

    public static void info(Player player, String message) {
        player.sendMessage(PREFIX + COLOR_INFO + message + COLOR_RESET);
    }

    /*Group message*/

    public static void success(Collection<? extends Player> players, String message) {
        send(players, COLOR_SUCCESS + message);
    }

    public static void warn(Collection<? extends Player> players, String message) {
        send(players, COLOR_WARN + message);
    }

    public static void error(Collection<? extends Player> players, String message) {
        send(players, COLOR_ERROR + message);
    }

    public static void info(Collection<? extends Player> players, String message) {
        send(players, COLOR_INFO + message);
    }

    /*Global message*/

    public static void success(World world, String message) {
        send(world.getPlayers(), COLOR_SUCCESS + message);
    }

    public static void warn(World world, String message) {
        send(world.getPlayers(), COLOR_WARN + message);
    }

    public static void error(World world, String message) {
        send(world.getPlayers(), COLOR_ERROR + message);
    }

    public static void info(World world, String message) {
        send(world.getPlayers(), COLOR_INFO + message);
    }

    /*Internal*/

    private static void send(Collection<? extends Player> players, String message) {
        String formatted = PREFIX + message + COLOR_RESET;
        for (Player player : players) {
            player.sendMessage(formatted);
        }
    }
}