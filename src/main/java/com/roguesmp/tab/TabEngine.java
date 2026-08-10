package com.roguesmp.tab;

import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.TabPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/** Manages the set of live {@link TabView}s: ticks each one once per server tick and tears them down on quit. */
public final class TabEngine implements Listener {

    private static TabEngine INSTANCE;

    private final Plugin plugin;
    private final List<TabView> active = new CopyOnWriteArrayList<>();
    private final Map<UUID, List<TabView>> viewsByPlayer = new ConcurrentHashMap<>();
    private BukkitTask task;

    private TabEngine(Plugin plugin) {
        this.plugin = plugin;
    }

    public static void init(Plugin plugin) {
        if (INSTANCE == null) {
            INSTANCE = new TabEngine(plugin);
            Bukkit.getPluginManager().registerEvents(INSTANCE, plugin);
            INSTANCE.start();
        }
    }

    public static TabEngine getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("TabEngine not initialized.");
        }
        return INSTANCE;
    }

    public static boolean isReady(Player player) {
        return TabAPI.getInstance().getPlayer(player.getUniqueId()) != null;
    }

    public static TabPlayer tabPlayer(Player player) {
        return TabAPI.getInstance().getPlayer(player.getUniqueId());
    }

    public void register(TabView view) {
        active.add(view);
        viewsByPlayer.computeIfAbsent(view.playerId(), id -> new CopyOnWriteArrayList<>()).add(view);
    }

    public void unregister(TabView view) {
        active.remove(view);
        viewsByPlayer.computeIfPresent(view.playerId(), (id, views) -> {
            views.remove(view);
            return views.isEmpty() ? null : views;
        });
    }

    private void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (TabView view : active) {
                view.tick();
            }
        }, 0L, 1L);
    }

    public void shutdown() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        for (TabView view : active) {
            view.destroy();
        }
        active.clear();
        viewsByPlayer.clear();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        List<TabView> views = viewsByPlayer.remove(event.getPlayer().getUniqueId());
        if (views == null) return;
        for (TabView view : List.copyOf(views)) {
            active.remove(view);
            view.destroy();
        }
    }
}
