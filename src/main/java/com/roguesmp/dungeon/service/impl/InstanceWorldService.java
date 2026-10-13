package com.roguesmp.dungeon.service.impl;

import com.roguesmp.dungeon.data.runtime.DungeonInstance;
import com.roguesmp.world.DungeonWorldManager;
import com.roguesmp.world.PolarTemplateData;
import com.roguesmp.world.PolarWorldKey;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class InstanceWorldService {

    private static final double ORIGIN_Y = 64;

    private final DungeonWorldManager worlds;
    private final PolarWorldKey template;

    public InstanceWorldService(DungeonWorldManager worlds, PolarWorldKey template) {
        this.worlds = worlds;
        this.template = template;
    }

    public CompletableFuture<World> create(UUID instanceId) {
        return worlds.createEmpty(template, instanceId, PolarTemplateData.DEFAULT);
    }

    public CompletableFuture<World> load(DungeonInstance instance) {
        return worlds.load(template, idOf(instance));
    }

    public CompletableFuture<Void> delete(DungeonInstance instance) {
        return worlds.delete(template, idOf(instance));
    }

    public @Nullable World getWorld(DungeonInstance instance) {
        return worlds.getLoaded(template, idOf(instance));
    }

    public @Nullable Location getOrigin(DungeonInstance instance) {
        World world = getWorld(instance);
        return world == null ? null : origin(world);
    }

    public Location origin(World world) {
        return new Location(world, 0, ORIGIN_Y, 0);
    }

    @Blocking
    public int deleteOrphans(Collection<DungeonInstance> instances) {
        Set<UUID> known = instances.stream().map(InstanceWorldService::idOf).collect(Collectors.toSet());
        return worlds.deleteOrphans(known);
    }

    @Blocking
    public void saveAllOnStop() {
        worlds.saveAllOnStop();
    }

    private static UUID idOf(DungeonInstance instance) {
        return UUID.fromString(instance.getSession().getSessionId());
    }
}
