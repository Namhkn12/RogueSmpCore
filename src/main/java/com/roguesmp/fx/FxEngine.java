package com.roguesmp.fx;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Ticks every active {@link FxEffect} once per server tick. Main-thread only, like the rest of the plugin. */
public final class FxEngine {

    private static FxEngine INSTANCE;

    private final Plugin plugin;
    private final List<FxEffect> active = new ArrayList<>();
    private final List<FxEffect> pending = new ArrayList<>();
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
        // Buffered, not added straight to `active` — play() can be called reentrantly from inside
        // the tick loop itself (e.g. an onComplete spawning a follow-up effect), which would
        // otherwise mutate `active` while it's being iterated below.
        pending.add(effect);
        ensureRunning();
        return new FxHandle(effect);
    }

    void stop(FxEffect effect) {
        // Only marks the effect removed/cleans up its parts here — `active` itself is swept lazily
        // by the tick loop below, so a reentrant stop() from inside that loop can't corrupt it either.
        effect.stop();
    }

    public void shutdown() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        pending.forEach(FxEffect::stop);
        pending.clear();
        active.forEach(FxEffect::stop);
        active.clear();
    }

    private void ensureRunning() {
        if (task != null) return;
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!pending.isEmpty()) {
                active.addAll(pending);
                pending.clear();
            }

            Iterator<FxEffect> it = active.iterator();
            while (it.hasNext()) {
                FxEffect effect = it.next();
                effect.tick();
                if (effect.isRemoved()) it.remove();
            }
        }, 0L, 1L);
    }
}
