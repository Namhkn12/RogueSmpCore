package com.roguesmp.dungeon.script;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.dungeon.DungeonEvent;
import com.roguesmp.dungeon.FeatureContext;
import com.roguesmp.dungeon.script.expr.ScriptText;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// The countdown boss bars of one run. Not saved: countdowns stop on restart.
public final class ScriptCountdowns {

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

    private final Map<String, Running> running = new HashMap<>();

    public void start(ScriptContext script, String id, int ticks, ScriptText title, BossBar.Color color) {
        FeatureContext<?> context = script.feature();
        stop(id);
        if (ticks <= 0) {
            context.fire(new DungeonEvent.CountdownEnded(id));
            return;
        }

        Running countdown = new Running(BossBar.bossBar(Utils.fromString(""), 1f, color, BossBar.Overlay.PROGRESS), ticks);
        countdown.task = Bukkit.getScheduler().runTaskTimer(RogueSmpCore.getInstance(), () -> {
            if (!context.manager().isLive(context.instance())) {
                stop(id);
                return;
            }
            if (countdown.left <= 0) {
                stop(id);
                context.fire(new DungeonEvent.CountdownEnded(id));
                return;
            }

            int seconds = (countdown.left + 19) / 20;
            if (seconds != countdown.shownSeconds) {
                countdown.shownSeconds = seconds;
                countdown.bar.name(Utils.fromString(title.format(script.withPlayer(null)).replace("{time}", formatTime(seconds))));
                updateViewers(countdown, context.world());
            }
            countdown.bar.progress(Math.clamp((float) countdown.left / ticks, 0f, 1f));
            countdown.left--;
        }, 0, 1);
        running.put(id, countdown);
    }

    public void stop(String id) {
        Running countdown = running.remove(id);
        if (countdown == null) return;
        countdown.task.cancel();
        countdown.hideAll();
    }

    public void stopAll() {
        new ArrayList<>(running.keySet()).forEach(this::stop);
    }

    private static void updateViewers(Running countdown, World world) {
        List<Player> present = world == null ? List.of() : world.getPlayers();
        Set<UUID> presentIds = new HashSet<>();
        for (Player player : present) {
            presentIds.add(player.getUniqueId());
            if (countdown.viewers.add(player.getUniqueId())) player.showBossBar(countdown.bar);
        }
        countdown.viewers.removeIf(viewer -> {
            if (presentIds.contains(viewer)) return false;
            Player player = Bukkit.getPlayer(viewer);
            if (player != null) player.hideBossBar(countdown.bar);
            return true;
        });
    }

    private static String formatTime(int seconds) {
        return seconds >= 60 ? "%d:%02d".formatted(seconds / 60, seconds % 60) : String.valueOf(seconds);
    }
}
