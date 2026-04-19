package com.roguesmp.dungeon_v2.utils;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.logging.Level;

public class Log4Craft_ {

    private final Plugin plugin;
    private final String prefix;

    public Log4Craft_(Plugin plugin, String prefix) {
        this.plugin = plugin;
        this.prefix = prefix;
    }

    public void sucess(Class clazz, String message){send(COLOR_SUCCESS, clazz, message);}

    public void info(Class clazz, String message){
        send(COLOR_INFO, clazz, message);
    }

    public void error(Class clazz, String message){
        send(COLOR_ERROR, clazz, message);
    }

    public void warn(Class clazz, String message){
        send(COLOR_WARN, clazz, message);
    }

    public void debug(Class clazz, String message){
        send(COLOR_DEBUG, clazz, message);
    }

    public void fire(Class clazz, String message, Throwable t){
        plugin.getLogger().log(Level.SEVERE, prefix+ " " + clazz.getName() + ": " + message, t);
    }

    public void send(String message){

    }

    public void send(String color, Class clazz, String message){
        Bukkit.getConsoleSender().sendMessage(color + prefix + " <" + clazz.getSimpleName() + "> " + message + COLOR_RESET);
    }

    private static final String COLOR_INFO    = "§7";
    private static final String COLOR_SUCCESS = "§a";
    private static final String COLOR_WARN    = "§e";
    private static final String COLOR_ERROR   = "§c";
    private static final String COLOR_DEBUG   = "§8";
    private static final String COLOR_RESET   = "§r";
}
