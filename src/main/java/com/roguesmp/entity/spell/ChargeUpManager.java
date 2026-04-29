package com.roguesmp.entity.spell;

import com.roguesmp.RogueSmpCore;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ChargeUpManager {
    private int time;
    private int chargeTime;
    private Component title;
    private final @Nullable Entity entity;
    private Location location;
    private final boolean requireEntity; //If this is true then boss bar is hidden if entity = null;
    private @Nullable BukkitRunnable runnable;
    private int refresh;

    private final int range;
    private final BossBar bar;

    private final Set<UUID> excludedPlayers = new HashSet<>();

    public ChargeUpManager(Entity boss, int chargeTime, Component title, BossBar.Color color, BossBar.Overlay style, int range) {
        this(boss.getLocation(), boss, chargeTime, title, color, style, range);
    }

    public ChargeUpManager(Location loc, @Nullable Entity boss, int chargeTime, Component title, BossBar.Color color, BossBar.Overlay style, int range) {
        location = loc;
        this.chargeTime = chargeTime;
        entity = boss;
        requireEntity = boss != null;
        this.title = title;
        time = 0;
        this.range = range;
        bar = BossBar.bossBar(this.title, 0, color, style);
        bar.progress(0);
        refresh = 0;
        runnable = null;
    }

    public void reset() {
        bar.progress(0);
        location.getWorld().hideBossBar(bar);
        time = 0;
        runnable = null;
    }


    public boolean nextTick() {
        return nextTick(1);
    }

    public boolean nextTick(int time) {
        this.time += time;
        update();
        return this.time >= chargeTime;
    }

    public boolean previousTick() {
        return previousTick(1);
    }

    public boolean previousTick(int time) {
        this.time -= time;
        update();
        return this.time <= 0;
    }

    public void update() {
        if (entity != null) {
            location = entity.getLocation();
        }

        refresh = 0;
        if (runnable == null) {
            runnable = new BukkitRunnable() {
                @Override
                public void run() {
                    refresh++;

                    if (refresh >= 20) {
                        this.cancel();
                        location.getWorld().hideBossBar(bar);
                        refresh = 0;
                        runnable = null;
                    }
                }
            };

            runnable.runTaskTimer(RogueSmpCore.getInstance(), 0, 1);
        }

        for (Player player : location.getWorld().getPlayers()) {
            if ((!requireEntity || (entity != null && entity.isValid())) && player.getLocation().distanceSquared(location) < range * range && !excludedPlayers.contains(player.getUniqueId())) {
                player.showBossBar(bar);
            } else {
                player.hideBossBar(bar);
            }
        }

        float progress = (float) time / (float) chargeTime;
        if (progress > 1) {
            progress = 1;
        } else if (progress < 0) {
            progress = 0;
        }
        bar.progress(progress);
    }

    public void setProgress(double progress) {
        setProgress((float) progress);
    }

    public float getProgress() {
        return (float) time / (float) chargeTime;
    }

    public void setProgress(float progress) {
        if (progress > 1) progress = 1;
        if (progress < 0) progress = 0;

        // Cập nhật biến time để đồng bộ với hàm update()
        this.time = (int) (progress * chargeTime);

        bar.progress(progress);
        refresh = 0;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public int getTime() {
        return time;
    }

    public void setChargeTime(int chargeTime) {
        this.chargeTime = chargeTime;
    }

    public int getChargeTime() {
        return chargeTime;
    }

    public void setTitle(Component title) {
        bar.name(title);
        this.title = title;
    }

    public Component getTitle() {
        return bar.name();
    }

    public void setColor(BossBar.Color color) {
        bar.color(color);
    }

    public BossBar.Color getColor() {
        return bar.color();
    }

    public void setOverlay(BossBar.Overlay overlay) {
        bar.overlay(overlay);
    }

    public BossBar.Overlay getOverlay() {
        return bar.overlay();
    }

    public void remove() {
        location.getWorld().hideBossBar(bar);
    }

    public void excludePlayer(Player player) {
        excludedPlayers.add(player.getUniqueId());
    }
}