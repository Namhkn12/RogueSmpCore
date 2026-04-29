package com.roguesmp.utils;

import com.roguesmp.RogueSmpCore;
import fr.skytasul.glowingentities.GlowingBlocks;
import fr.skytasul.glowingentities.GlowingEntities;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

@SuppressWarnings("deprecation")
public class GlowUtils {
    private static GlowingEntities glowingEntities;
    private static GlowingBlocks glowingBlocks;

    public static void glow(Entity entity, Player player, ChatColor color, int duration) {
        glow(entity, player, color);
        Utils.runLater(() -> unsetGlow(entity, player), duration);
    }

    public static void glow(Entity entity, ChatColor color, int duration) {
        entity.getWorld().getPlayers().forEach(player -> glow(entity, player, color, duration));
    }

    public static void glow(Entity entity, ChatColor color) {
        entity.getWorld().getPlayers().forEach(player -> glow(entity, player, color));
    }

    public static void glow(Block block, Player player, ChatColor color, int duration) {
        glow(block, player, color);
        Utils.runLater(() -> unsetGlow(block, player), duration);
    }

    public static void glow(Block block, ChatColor color, int duration) {
        block.getWorld().getPlayers().forEach(player -> glow(block, player, color, duration));
    }

    public static void glow(Block block, ChatColor color) {
        block.getWorld().getPlayers().forEach(player -> glow(block, player, color));
    }

    public static void glow(Entity entity, Player player, ChatColor color) {
        try {
            glowingEntities.setGlowing(entity, player, color);
        } catch (ReflectiveOperationException e) {
            RogueSmpCore.LOGGER.warn("Reflective exception while setting glow status for player {}, with entity {}", player.getName(), entity.getUniqueId());
        }
    }

    public static void unsetGlow(Entity entity, Player player) {
        try {
            glowingEntities.unsetGlowing(entity, player);
        } catch (ReflectiveOperationException e) {
            RogueSmpCore.LOGGER.warn("Reflective exception while setting unsetting glow status for player {}, with entity {}", player.getName(), entity.getUniqueId());
        }
    }

    public static void unsetGlow(Entity entity) {
        entity.getWorld().getPlayers().forEach(player -> {
            unsetGlow(entity, player);
        });

    }

    public static void unsetGlow(Block block, Player player) {
        try {
            glowingBlocks.unsetGlowing(block, player);
        } catch (ReflectiveOperationException e) {
            RogueSmpCore.LOGGER.warn("Reflective exception while setting unsetting glow status for player {}, with block {}", player.getName(), block.getLocation());
        }
    }

    public static void unsetGlow(Block block) {
        block.getWorld().getPlayers().forEach(player -> {
            unsetGlow(block, player);
        });
    }

    public static void glow(Block block, Player player, ChatColor color) {
        try {
            glowingBlocks.setGlowing(block, player, color);
        } catch (ReflectiveOperationException e) {
            RogueSmpCore.LOGGER.warn("Reflective exception while setting glow status for player {}, with block {}", player.getName(), block.getLocation());
        }
    }

    public static void init(JavaPlugin plugin) {
        glowingEntities = new GlowingEntities(plugin);
        glowingBlocks = new GlowingBlocks(plugin);
    }
}
