package com.roguesmp.entity;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.utils.EntityUtils;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;

public class BossBarManager {
    @FunctionalInterface
    public interface BossHealthAction {
        void run(LivingEntity boss);
    }

    private final LivingEntity entity;
    private final int range;
    private final PriorityQueue<Map.Entry<Integer, BossHealthAction>> events;
    private final BossBar bar;
    private final boolean capDamage;
    private final Function<LivingEntity, Location> locationalFunction;

    public BossBarManager(LivingEntity boss, int range, BossBar.Color color, BossBar.Overlay style, @Nullable Map<Integer, BossHealthAction> events) {
        this(boss, range, color, style, events, true);
    }

    public BossBarManager(LivingEntity boss, int range, BossBar.Color color, BossBar.Overlay style, @Nullable Map<Integer, BossHealthAction> events, boolean bossFog) {
        this(boss, range, color, style, events, bossFog, true);
    }

    public BossBarManager(LivingEntity boss, int range, BossBar.Color color, BossBar.Overlay style, @Nullable Map<Integer, BossHealthAction> events, boolean bossFog, boolean capDamage) {
        this(boss, range, color, style, events, bossFog, capDamage, Entity::getLocation);
    }

    public BossBarManager(LivingEntity boss, int range, BossBar.Color color, BossBar.Overlay style, @Nullable Map<Integer, BossHealthAction> events, boolean bossFog, boolean capDamage, Location centerLocation) {
        this(boss, range, color, style, events, bossFog, capDamage, b -> centerLocation);
    }

    public BossBarManager(LivingEntity boss, int range, BossBar.Color color, BossBar.Overlay style, @Nullable Map<Integer, BossHealthAction> events, boolean bossFog, boolean capDamage, Function<LivingEntity, Location> locationFunction) {
        entity = boss;
        this.range = range;
        this.events = new PriorityQueue<>(Math.max(events != null ? events.size() : 1, 1), Comparator.comparing(entry -> -entry.getKey()));
        if (events != null) {
            this.events.addAll(events.entrySet());
        }
        this.capDamage = capDamage;
        locationalFunction = locationFunction;

        bar = BossBar.bossBar(Component.text(entity.getName()), (float) 0, color, style, Set.of(BossBar.Flag.PLAY_BOSS_MUSIC));
        if (bossFog) {
            bar.addFlag(BossBar.Flag.CREATE_WORLD_FOG);
            bar.addFlag(BossBar.Flag.DARKEN_SCREEN);
        }

        Location loc = locationFunction.apply(boss);
        for (Player player : entity.getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(loc) < range * range) {
                bar.addViewer(player);
            }
        }
    }

    public void setBossFog(boolean value) {
        if (value) {
            bar.addFlag(BossBar.Flag.CREATE_WORLD_FOG);
            bar.addFlag(BossBar.Flag.DARKEN_SCREEN);
        } else {
            bar.removeFlag(BossBar.Flag.CREATE_WORLD_FOG);
            bar.removeFlag(BossBar.Flag.DARKEN_SCREEN);
        }
    }

    public void update() {
        if (entity.getHealth() <= 0) {
            entity.getWorld().hideBossBar(bar);
        }

        Location loc = locationalFunction.apply(entity);
        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            if (player.getWorld().equals(loc.getWorld()) && player.getLocation().distanceSquared(loc) < range * range) {
                bar.addViewer(player);
            } else {
                bar.removeViewer(player);
            }
        }

        double maxHealth = EntityUtils.getMaxHealth(entity);
        OptionalDouble forceProgress = progressEvents();
        double progress = forceProgress.orElse(entity.getHealth() / maxHealth);

        if (!Double.isFinite(progress) || progress > 1.0f || progress < 0f) {
            RogueSmpCore.LOGGER.warn("Boss Entity '{}' has invalid health {} out of max {}", entity.getName(), entity.getHealth(), maxHealth);
        } else {
            forceProgress.ifPresent(p -> entity.setHealth(maxHealth * p));
            bar.progress((float) progress);
        }
    }

    public OptionalDouble progressEvents() {
        double maxHealth = EntityUtils.getMaxHealth(entity);
        double currentPercent = entity.getHealth() / maxHealth * 100;
        if (events == null) {
            return OptionalDouble.empty();
        }
        while (true) {
            @Nullable
            Map.Entry<Integer, BossHealthAction> entry = events.peek();
            if (entry == null || entry.getKey() < currentPercent) {
                return OptionalDouble.empty();
            }

            entry.getValue().run(entity);
            events.remove();
            if (capDamage) {
                return OptionalDouble.of(entry.getKey() / 100.0);
            }
        }
    }

    public void setTitle(String newTitle) {
        bar.name(Component.text(newTitle));
    }

    public void setColor(BossBar.Color barColor) {
        bar.color(barColor);
    }

    public void remove() {
        entity.getWorld().hideBossBar(bar);
    }

    public boolean capsDamage() {
        return capDamage;
    }

    public Optional<Integer> getNextHealthThreshold() {
        return Optional.ofNullable(events.peek()).map(Map.Entry::getKey);
    }

    public boolean removeHealthEvent(int percent) {
        return events != null && events.removeIf(entry -> entry.getKey() == percent);
    }
}
