package com.roguesmp.teleport;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.teleport.destination.LocationDestination;
import com.roguesmp.utils.Utils;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.mvplugins.multiverse.core.MultiverseCoreApi;
import org.mvplugins.multiverse.core.teleportation.PassengerModes;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class TeleportService {

    private static TeleportService INSTANCE;

    private final Set<UUID> pending = new HashSet<>();

    private TeleportService() {
    }

    public static void init() {
        INSTANCE = new TeleportService();
    }

    public static TeleportService getInstance() {
        return INSTANCE;
    }

    public boolean isPending(Player player) {
        return pending.contains(player.getUniqueId());
    }

    public CompletableFuture<Boolean> teleport(Player player, Location location, TeleportOptions options) {
        return teleport(player, LocationDestination.of(location), options);
    }

    public CompletableFuture<Boolean> teleport(Player player, TeleportDestination destination, TeleportOptions options) {
        if (!new SmpTeleportEvent(player, SmpTeleportEvent.Phase.WARMUP, destination, options, null).callEvent()) {
            return CompletableFuture.completedFuture(false);
        }

        CompletableFuture<Boolean> result = new CompletableFuture<>();
        if (options.warmupTicks() > 0) {
            if (!pending.add(player.getUniqueId())) return CompletableFuture.completedFuture(false);
            result.whenComplete((teleported, error) -> pending.remove(player.getUniqueId()));
        }

        options.effect().onWarmupStart(player, options.warmupTicks());

        Runnable execute = () -> execute(player, destination, options, result);
        if (options.warmupTicks() > 0) Utils.runLater(execute, options.warmupTicks());
        else execute.run();
        return result;
    }

    private void execute(Player player, TeleportDestination destination, TeleportOptions options, CompletableFuture<Boolean> result) {
        if (!player.isOnline()) {
            result.complete(false);
            return;
        }

        try {
            Location target = destination.resolve(player);
            if (target == null || target.getWorld() == null) {
                RogueSmpCore.LOGGER.warn("Teleport destination for {} could not be resolved: {}", player.getName(), destination.format());
                fail(player, options, result);
                return;
            }

            if (!new SmpTeleportEvent(player, SmpTeleportEvent.Phase.DEPART, destination, options, target).callEvent()) {
                options.effect().onFailed(player);
                result.complete(false);
                return;
            }

            options.effect().onDepart(player, player.getLocation());
            CompletableFuture<Boolean> move = options.safe() ? safeTeleport(player, target) : player.teleportAsync(target);
            move.whenComplete((teleported, error) -> Utils.runLater(() -> {
                if (error != null || !Boolean.TRUE.equals(teleported)) {
                    fail(player, options, result);
                    return;
                }
                if (player.isOnline()) options.effect().onArrive(player);
                result.complete(true);
            }));
        } catch (RuntimeException e) {
            RogueSmpCore.LOGGER.error("Teleport of {} to {} failed", player.getName(), destination.format(), e);
            fail(player, options, result);
        }
    }

    private void fail(Player player, TeleportOptions options, CompletableFuture<Boolean> result) {
        if (player.isOnline()) {
            options.effect().onFailed(player);
            if (options.hasEffect()) player.sendMessage(Utils.fromString("<red>Không thể dịch chuyển tới điểm này."));
        }
        result.complete(false);
    }

    private CompletableFuture<Boolean> safeTeleport(Player player, Location target) {
        CompletableFuture<Boolean> result = new CompletableFuture<>();
        MultiverseCoreApi.get().getSafetyTeleporter()
                .to(target)
                .passengerMode(PassengerModes.RETAIN_ALL)
                .teleportSingle(player)
                .onSuccess(() -> result.complete(true))
                .onFailure(attempts -> {
                    attempts.forEach(attempt -> RogueSmpCore.LOGGER.warn("Multiverse could not teleport {} ({}), falling back to a plain teleport",
                            player.getName(), attempt.getFailureMessage()));
                    Utils.runLater(() -> player.teleportAsync(target).whenComplete((teleported, error) -> result.complete(error == null && Boolean.TRUE.equals(teleported))));
                });
        return result;
    }
}
