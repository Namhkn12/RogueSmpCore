package com.roguesmp.permission;

import com.roguesmp.island.IslandData;
import com.roguesmp.island.IslandManager;
import net.luckperms.api.context.ContextCalculator;
import net.luckperms.api.context.ContextConsumer;
import net.luckperms.api.context.ContextSet;
import net.luckperms.api.context.ImmutableContextSet;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class SmpContextCalculator implements ContextCalculator<Player> {

    public static final String WORLD_KEY = "smp-world";
    public static final String ISLAND_ROLE_KEY = "island-role";

    private static final String WORLD_ISLAND = "island";
    private static final String WORLD_DUNGEON = "dungeon";
    private static final String WORLD_BUILDING = "building";
    private static final String WORLD_OTHER = "other";

    private static final String ROLE_MEMBER = "member";
    private static final String ROLE_VISITOR = "visitor";

    @Override
    public void calculate(@NotNull Player target, @NotNull ContextConsumer consumer) {
        World world = target.getWorld();
        IslandManager islandManager = IslandManager.getInstance();

        UUID islandId = islandManager.getIslandWorldManager().getIslandId(world);
        if (islandId != null) {
            consumer.accept(WORLD_KEY, WORLD_ISLAND);
            IslandData islandData = islandManager.getIslandDataManager().getCachedData(islandId);
            if (islandData != null) {
                consumer.accept(ISLAND_ROLE_KEY, islandData.isMember(target.getUniqueId()) ? ROLE_MEMBER : ROLE_VISITOR);
            }
            return;
        }

        String worldName = world.getName();
        if (worldName.startsWith("dungeon_")) {
            consumer.accept(WORLD_KEY, WORLD_DUNGEON);
        } else if (worldName.startsWith("building")) {
            consumer.accept(WORLD_KEY, WORLD_BUILDING);
        } else {
            consumer.accept(WORLD_KEY, WORLD_OTHER);
        }
    }

    @Override
    public @NotNull ContextSet estimatePotentialContexts() {
        return ImmutableContextSet.builder()
                .add(WORLD_KEY, WORLD_ISLAND)
                .add(WORLD_KEY, WORLD_DUNGEON)
                .add(WORLD_KEY, WORLD_BUILDING)
                .add(WORLD_KEY, WORLD_OTHER)
                .add(ISLAND_ROLE_KEY, ROLE_MEMBER)
                .add(ISLAND_ROLE_KEY, ROLE_VISITOR)
                .build();
    }
}
