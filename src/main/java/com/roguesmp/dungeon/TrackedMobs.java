package com.roguesmp.dungeon;

import com.roguesmp.constant.Keys;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class TrackedMobs {

    private final Map<UUID, Map<String, Set<UUID>>> byWorld = new HashMap<>();

    public void track(String tag, LivingEntity mob) {
        mob.setRemoveWhenFarAway(false);
        mob.setPersistent(true);
        mob.getPersistentDataContainer().set(Keys.DUNGEON_MOB_TAG, PersistentDataType.STRING, tag);
        add(mob);
    }

    public void add(Entity entity) {
        String tag = tagOf(entity);
        if (tag == null) return;
        byWorld.computeIfAbsent(entity.getWorld().getUID(), world -> new HashMap<>())
                .computeIfAbsent(tag, key -> new HashSet<>())
                .add(entity.getUniqueId());
    }

    public void remove(Entity entity) {
        String tag = tagOf(entity);
        if (tag == null) return;
        Map<String, Set<UUID>> tags = byWorld.get(entity.getWorld().getUID());
        if (tags == null) return;
        Set<UUID> mobs = tags.get(tag);
        if (mobs == null) return;
        mobs.remove(entity.getUniqueId());
        if (mobs.isEmpty()) tags.remove(tag);
        if (tags.isEmpty()) byWorld.remove(entity.getWorld().getUID());
    }

    public void clear(World world) {
        byWorld.remove(world.getUID());
    }

    public int aliveCount(World world, String tag) {
        Map<String, Set<UUID>> tags = byWorld.get(world.getUID());
        if (tags == null) return 0;
        Set<UUID> mobs = tags.get(tag);
        return mobs == null ? 0 : mobs.size();
    }

    public static @Nullable String tagOf(Entity entity) {
        return entity.getPersistentDataContainer().get(Keys.DUNGEON_MOB_TAG, PersistentDataType.STRING);
    }
}
