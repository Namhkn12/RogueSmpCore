package com.roguesmp.utils;

import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class AbilityUtils {
    public static Item spawnAbilityItem(World world, Location loc, Material mat, String name, boolean dropNaturally, double velocity, boolean glow, boolean invulnerable) {
        return spawnAbilityItem(world, loc, mat, name, dropNaturally, velocity, glow, invulnerable, ChatColor.WHITE);
    }

    public static Item spawnAbilityItem(World world, Location loc, Material mat, String name, boolean dropNaturally, double velocity, boolean glow, boolean invulnerable, ChatColor glowColor) {
        ItemStack stack = ItemStack.of(mat);
        stack.setData(DataComponentTypes.CUSTOM_NAME, Component.text(name, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
        Item droppedItem;
        if (dropNaturally) {
            droppedItem = world.dropItemNaturally(loc, stack);
        } else {
            droppedItem = world.dropItem(loc, stack);
            Vector vel = loc.getDirection().normalize().multiply(velocity);
            droppedItem.setVelocity(vel);
        }
        droppedItem.setPickupDelay(Integer.MAX_VALUE);
        if (glow) {
            GlowUtils.glow(droppedItem, glowColor);
        }

        if (invulnerable) {
            droppedItem.setInvulnerable(true);
        }
        droppedItem.setPersistent(false);
        return droppedItem;
    }
}
