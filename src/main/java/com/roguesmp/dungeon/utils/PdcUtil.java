package com.roguesmp.dungeon.utils;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Optional;

public final class PdcUtil {

    private PdcUtil() {}

    // ItemStack
    public static <P, C> Optional<C> get(ItemStack item, NamespacedKey key, PersistentDataType<P, C> type) {
        if (item == null || !item.hasItemMeta()) return Optional.empty();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return Optional.empty();
        return get(meta.getPersistentDataContainer(), key, type);
    }

    public static <P, C> C getOrDefault(ItemStack item, NamespacedKey key, PersistentDataType<P, C> type, C defaultValue) {
        return get(item, key, type).orElse(defaultValue);
    }

    public static <P, C> boolean has(ItemStack item, NamespacedKey key, PersistentDataType<P, C> type) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(key, type);
    }

    // Entity
    public static <P, C> Optional<C> get(Entity entity, NamespacedKey key, PersistentDataType<P, C> type) {
        if (entity == null) return Optional.empty();
        return get(entity.getPersistentDataContainer(), key, type);
    }

    public static <P, C> C getOrDefault(Entity entity, NamespacedKey key, PersistentDataType<P, C> type, C defaultValue) {
        return get(entity, key, type).orElse(defaultValue);
    }

    public static <P, C> boolean has(Entity entity, NamespacedKey key, PersistentDataType<P, C> type) {
        if (entity == null) return false;
        return entity.getPersistentDataContainer().has(key, type);
    }

    // PersistentDataContainer (base)
    public static <P, C> Optional<C> get(PersistentDataContainer pdc, NamespacedKey key, PersistentDataType<P, C> type) {
        if (pdc == null || !pdc.has(key, type)) return Optional.empty();
        return Optional.ofNullable(pdc.get(key, type));
    }

    public static <P, C> C getOrDefault(PersistentDataContainer pdc, NamespacedKey key, PersistentDataType<P, C> type, C defaultValue) {
        return get(pdc, key, type).orElse(defaultValue);
    }

    public static <P, C> boolean has(PersistentDataContainer pdc, NamespacedKey key, PersistentDataType<P, C> type) {
        return pdc != null && pdc.has(key, type);
    }
}