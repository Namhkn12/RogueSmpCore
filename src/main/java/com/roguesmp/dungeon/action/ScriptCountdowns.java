package com.roguesmp.dungeon.action;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.dungeon.DungeonEvent;
import com.roguesmp.dungeon.DungeonInstance;
import com.roguesmp.dungeon.FeatureContext;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class ScriptCountdowns {

    private static final class Running {
        private final BossBar bar;
        private final Set<UUID> viewers = new HashSet<>();
        private BukkitTask task;
        private int left;
        private int shownSeconds = -1;

        private Running(BossBar bar, int left) {
            this.bar = bar;
            this.left = left;
        }

        private void hideAll() {
            for (UUID viewer : viewers) {
                Player player = Bukkit.getPlayer(viewer);
                if (player != null) player.hideBossBar(bar);
            }
            viewers.clear();
        }
    }

    private static final Map<UUID, Map<String, Running>> RUNNING = new HashMap<>();

    private ScriptCountdowns() {
    }

    static void start(FeatureContext<?> context, String id, int ticks, String title, BossBar.Color color) {
        DungeonInstance instance = context.instance();
        stop(instance, id);

        Running running = new Running(BossBar.bossBar(Utils.fromString(""), 1f, color, BossBar.Overlay.PROGRESS), ticks);
        running.task = Bukkit.getScheduler().runTaskTimer(RogueSmpCore.getInstance(), () -> {
            if (!context.manager().isLive(instance)) {
                stop(instance, id);
                return;
            }
            if (running.left <= 0) {
                stop(instance, id);
                context.fire(new DungeonEvent.CountdownEnded(id));
                return;
            }

            int seconds = (running.left + 19) / 20;
            if (seconds != running.shownSeconds) {
                running.shownSeconds = seconds;
                running.bar.name(Utils.fromString(DungeonActions.format(context, null, title).replace("{time}", formatTime(seconds))));
                updateViewers(running, context.world());
            }
            running.bar.progress(Math.clamp((float) running.left / ticks, 0f, 1f));
            running.left--;
        }, 0, 1);

        RUNNING.computeIfAbsent(instance.id(), key -> new HashMap<>()).put(id, running);
    }

    static void stop(DungeonInstance instance, String id) {
        Map<String, Running> countdowns = RUNNING.get(instance.id());
        Running running = countdowns == null ? null : countdowns.remove(id);
        if (running == null) return;

        running.task.cancel();
        running.hideAll();
        if (countdowns.isEmpty()) RUNNING.remove(instance.id());
    }

    private static void updateViewers(Running running, World world) {
        List<Player> present = world == null ? List.of() : world.getPlayers();
        Set<UUID> presentIds = new HashSet<>();
        for (Player player : present) {
            presentIds.add(player.getUniqueId());
            if (running.viewers.add(player.getUniqueId())) player.showBossBar(running.bar);
        }
        running.viewers.removeIf(viewer -> {
            if (presentIds.contains(viewer)) return false;
            Player player = Bukkit.getPlayer(viewer);
            if (player != null) player.hideBossBar(running.bar);
            return true;
        });
    }

    private static String formatTime(int seconds) {
        return seconds >= 60 ? "%d:%02d".formatted(seconds / 60, seconds % 60) : String.valueOf(seconds);
    }
}
