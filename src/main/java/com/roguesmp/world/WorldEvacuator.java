package com.roguesmp.world;

import com.roguesmp.teleport.TeleportOptions;
import com.roguesmp.teleport.TeleportService;
import com.roguesmp.utils.AsyncUtils;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Moves every player out of a world before it is unloaded or deleted. Runs on the main thread; a failed future
 * aborts the operation and leaves the world loaded.
 */
@FunctionalInterface
public interface WorldEvacuator {

    CompletableFuture<Void> evacuate(World world);

    static WorldEvacuator none() {
        return world -> CompletableFuture.completedFuture(null);
    }

    static WorldEvacuator toDefaultWorld() {
        return world -> {
            if (world.getPlayers().isEmpty()) return CompletableFuture.completedFuture(null);

            Location hubSpawn = Hub.spawn();

            if (world.equals(hubSpawn.getWorld())) {
                return CompletableFuture.failedFuture(new IllegalStateException("The default world cannot be evacuated"));
            }

            List<CompletableFuture<Boolean>> teleports = world.getPlayers().stream()
                    .map(player -> TeleportService.getInstance().teleport(player, hubSpawn, TeleportOptions.INSTANT)).toList();

            return CompletableFuture.allOf(teleports.toArray(CompletableFuture[]::new))
                    .thenCompose(v -> AsyncUtils.onMainThread(() -> world.getPlayers().isEmpty()
                            ? CompletableFuture.<Void>completedFuture(null)
                            : CompletableFuture.<Void>failedFuture(new IllegalStateException("Could not evacuate every player from " + world.getName()))));
        };
    }
}
