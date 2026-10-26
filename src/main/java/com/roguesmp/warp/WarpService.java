package com.roguesmp.warp;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.codec.DataResult;
import com.roguesmp.registry.Registries;
import com.roguesmp.teleport.TeleportDestination;
import com.roguesmp.teleport.TeleportService;
import com.roguesmp.teleport.destination.PlayerDestination;
import com.roguesmp.teleport.destination.WarpDestination;
import com.roguesmp.utils.AsyncUtils;
import com.roguesmp.utils.Utils;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

public final class WarpService {

    public static final Set<String> RESERVED_NAMES = Set.of("create", "set", "edit", "delete");
    private static final Pattern VALID_ID = Pattern.compile("[a-z0-9_\\-]{1,32}");

    private static WarpService INSTANCE;

    private final RogueSmpCore plugin;
    private final Map<String, CompletableFuture<Void>> writes = new ConcurrentHashMap<>();

    private WarpService(RogueSmpCore plugin) {
        this.plugin = plugin;
    }

    public static void init(RogueSmpCore plugin) {
        INSTANCE = new WarpService(plugin);
    }

    public static WarpService getInstance() {
        return INSTANCE;
    }

    public List<Map.Entry<String, Warp>> usableWarps(Player player) {
        return Registries.WARP.getAll().entrySet().stream()
                .filter(entry -> entry.getValue().canUse(player))
                .sorted(Comparator.<Map.Entry<String, Warp>>comparingInt(entry -> entry.getValue().order()).thenComparing(Map.Entry::getKey))
                .toList();
    }

    public void teleport(Player player, String id) {
        Warp warp = Registries.WARP.get(id);
        if (warp == null || !warp.canUse(player)) {
            player.sendMessage(Utils.fromString("<red>Không tìm thấy điểm dịch chuyển '" + Utils.MINI_MESSAGE.escapeTags(id) + "'."));
            return;
        }

        TeleportService.getInstance().teleport(player, warp.destination(), warp.options());
    }

    public DataResult<Warp> create(String id, TeleportDestination destination) {
        if (RESERVED_NAMES.contains(id) || !VALID_ID.matcher(id).matches()) {
            return DataResult.error("Invalid warp name. Use lowercase letters, digits, _ or -, and not " + RESERVED_NAMES + ".");
        }
        if (Registries.WARP.get(id) != null) return DataResult.error("Warp '" + id + "' already exists.");

        DataResult<TeleportDestination> checked = checkDestination(destination);
        if (!checked.isSuccess()) return DataResult.error(checked.error());

        Warp warp = Warp.create(id, destination);
        store(id, warp);
        return DataResult.success(warp);
    }

    public DataResult<Warp> update(String id, UnaryOperator<Warp> change) {
        Warp current = Registries.WARP.get(id);
        if (current == null) return DataResult.error("Unknown warp '" + id + "'.");

        Warp updated = change.apply(current);
        DataResult<TeleportDestination> checked = checkDestination(updated.destination());
        if (!checked.isSuccess()) return DataResult.error(checked.error());

        store(id, updated);
        return DataResult.success(updated);
    }

    public boolean delete(String id) {
        if (!Registries.WARP.unregister(id)) return false;
        enqueueWrite(id, () -> Registries.WARP.get(id) != null
                ? CompletableFuture.completedFuture(null)
                : Registries.WARP.removeAndDeleteFilesAsync(plugin, id).thenAccept(deleted -> { }));
        return true;
    }

    private void store(String id, Warp warp) {
        Registries.WARP.register(id, warp);
        enqueueWrite(id, () -> {
            Warp latest = Registries.WARP.get(id);
            return latest == null
                    ? CompletableFuture.completedFuture(null)
                    : Registries.WARP.registerAndSaveAsync(plugin, id, latest).thenAccept(saved -> { });
        });
    }

    private void enqueueWrite(String id, Supplier<CompletableFuture<Void>> write) {
        CompletableFuture<Void> previous = writes.getOrDefault(id, CompletableFuture.completedFuture(null));
        CompletableFuture<Void> next = previous.exceptionally(error -> null).thenCompose(ignored -> AsyncUtils.onMainThread(write));
        writes.put(id, next);
        next.whenComplete((ignored, error) -> {
            if (error != null) RogueSmpCore.LOGGER.error("Failed to write warp '{}'", id, error);
            writes.remove(id, next);
        });
    }

    private static DataResult<TeleportDestination> checkDestination(TeleportDestination destination) {
        if (destination instanceof WarpDestination || destination instanceof PlayerDestination) {
            return DataResult.error("A warp cannot point to another warp or a player.");
        }
        return DataResult.success(destination);
    }
}
