package com.roguesmp.fx;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/** Ticks every active {@link FxEffect} once per server tick. */
public final class FxEngine {

    private static FxEngine INSTANCE;

    private final Plugin plugin;
    private final Set<FxEffect> active = new CopyOnWriteArraySet<>();
    private BukkitTask task;

    private FxEngine(Plugin plugin) {
        this.plugin = plugin;
    }

    public static void init(Plugin plugin) {
        if (INSTANCE == null) {
            INSTANCE = new FxEngine(plugin);
        }
    }

    public static FxEngine getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("FxEngine not initialized.");
        }
        return INSTANCE;
    }

    public FxHandle play(FxEffect effect) {
        active.add(effect);
        ensureRunning();
        return new FxHandle(effect);
    }

    void stop(FxEffect effect) {
        effect.stop();
        active.remove(effect);
    }

    public void shutdown() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        active.forEach(FxEffect::stop);
        active.clear();
    }

    private void ensureRunning() {
        if (task != null) return;
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (FxEffect effect : active) {
                effect.tick();
                if (effect.isRemoved()) active.remove(effect);
            }
        }, 0L, 1L);
    }
}
