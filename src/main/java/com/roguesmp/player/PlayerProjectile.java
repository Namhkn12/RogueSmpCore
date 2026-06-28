package com.roguesmp.player;

import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent;
import com.roguesmp.constant.Attributes;
import com.roguesmp.constant.ComponentKeys;
import com.roguesmp.constant.Enchants;
import com.roguesmp.constant.EquipSlot;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.item.SmpItem;
import com.roguesmp.item.component.impl.EnchantComponent;
import com.roguesmp.item.component.impl.EquipAttributeComponent;
import com.roguesmp.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ThrowableProjectile;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * For tracking projectiles (snapshot stat,...)
 */
public class PlayerProjectile {

    private final UUID uuid; // Arrow entity uuid
    private final SmpPlayer smpPlayer;
    private final Map<Enchants, Integer> activeEnchants = new EnumMap<>(Enchants.class);
    private final Map<Attributes, Double> activeAttributes = new EnumMap<>(Attributes.class);

    private int tickAlive = 0;

    public PlayerProjectile(SmpPlayer player, Projectile projectile, Map<Enchants, Integer> enchants, Map<Attributes, Double> attributes) {
        this.uuid = projectile.getUniqueId();
        this.smpPlayer = player;
        this.activeEnchants.putAll(enchants);
        this.activeAttributes.putAll(attributes);

    }

    public void onDamageEntity(DamageEvent event) {
        activeAttributes.forEach((attributes, aDouble) -> {
            attributes.getAttribute().onDamageEntity(event, aDouble, smpPlayer);
        });
        activeEnchants.forEach((enchants, integer) -> {
            enchants.getEnchant().onDamageEntity(event, integer, smpPlayer);
        });
    }

    public void onProjectileHit(ProjectileHitEvent event) {
        activeAttributes.forEach((attributes, aDouble) -> {
            attributes.getAttribute().onProjectileHit(event, aDouble, smpPlayer);
        });
        activeEnchants.forEach((enchants, integer) -> {
            enchants.getEnchant().onProjectileHit(event, integer, smpPlayer);
        });
    }

    public void onProjectileLaunch(PlayerLaunchProjectileEvent event) {
        activeAttributes.forEach((attributes, aDouble) -> {
            attributes.getAttribute().onProjectileLaunch(event, aDouble, smpPlayer);
        });
        activeEnchants.forEach((enchants, integer) -> {
            enchants.getEnchant().onProjectileLaunch(event, integer, smpPlayer);
        });
    }

    public void onShootArrow(EntityShootBowEvent event) {
        activeAttributes.forEach((attributes, aDouble) -> {
            attributes.getAttribute().onShootArrow(event, aDouble, smpPlayer);
        });
        activeEnchants.forEach((enchants, integer) -> {
            enchants.getEnchant().onShootArrow(event, integer, smpPlayer);
        });
    }

    public void onCombustEntity(EntityCombustByEntityEvent event) {
        activeAttributes.forEach((attributes, aDouble) -> {
            attributes.getAttribute().onCombustEntity(event, aDouble, smpPlayer);
        });
        activeEnchants.forEach((enchants, integer) -> {
            enchants.getEnchant().onCombustEntity(event, integer, smpPlayer);
        });
    }

    public Map<Enchants, Integer> getActiveEnchants() {
        return activeEnchants;
    }

    public Map<Attributes, Double> getActiveAttributes() {
        return activeAttributes;
    }

    public @Nullable Projectile getProjectile() {
        return (Projectile) Bukkit.getEntity(uuid);
    }

    public boolean shouldRemove() {
        return tickAlive > 200;
    }

    public int getTickAlive() {
        return tickAlive;
    }

    public void incrementTickAlive(int periodIncrement) {
        tickAlive = tickAlive + periodIncrement;
    }
}
