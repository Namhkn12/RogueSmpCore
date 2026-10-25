package com.roguesmp.entity.spell;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public final class ChargeUpManager {

    private final BossBar bar;
    private final Supplier<? extends Collection<Player>> audience;
    private final Set<UUID> viewers = new HashSet<>();
    private final Set<UUID> excluded = new HashSet<>();
    private int time;
    private int chargeTime;

    public ChargeUpManager(Supplier<? extends Collection<Player>> audience, int chargeTime, Component title,
                           BossBar.Color color, BossBar.Overlay overlay) {
        this.audience = audience;
        this.chargeTime = chargeTime;
        this.bar = BossBar.bossBar(title, 0f, color, overlay);
    }

    public static ChargeUpManager around(Entity anchor, double range, int chargeTime, Component title,
                                         BossBar.Color color, BossBar.Overlay overlay) {
        return new ChargeUpManager(() -> anchor.isValid() ? playersNear(anchor.getLocation(), range) : List.of(),
                chargeTime, title, color, overlay);
    }

    public static ChargeUpManager around(Location location, double range, int chargeTime, Component title,
                                         BossBar.Color color, BossBar.Overlay overlay) {
        Location center = location.clone();
        return new ChargeUpManager(() -> playersNear(center, range), chargeTime, title, color, overlay);
    }

    private static List<Player> playersNear(Location center, double range) {
        return center.getWorld().getPlayers().stream()
                .filter(player -> player.getLocation().distanceSquared(center) <= range * range)
                .toList();
    }

    public boolean nextTick(int ticks) {
        time = Math.min(chargeTime, time + ticks);
        update();
        return time >= chargeTime;
    }

    public boolean previousTick(int ticks) {
        time = Math.max(0, time - ticks);
        update();
        return time <= 0;
    }

    public void update() {
        bar.progress(getProgress());
        syncViewers();
    }

    public void show(Component title, float progress, BossBar.Color color) {
        bar.name(title);
        bar.color(color);
        setProgress(progress);
        syncViewers();
    }

    public void reset() {
        time = 0;
        bar.progress(0f);
        hide();
    }

    public void hide() {
        viewers.forEach(this::hideFrom);
        viewers.clear();
    }

    public void exclude(Player player) {
        excluded.add(player.getUniqueId());
        if (viewers.remove(player.getUniqueId())) player.hideBossBar(bar);
    }

    private void syncViewers() {
        Set<UUID> current = new HashSet<>();
        for (Player player : audience.get()) {
            UUID uuid = player.getUniqueId();
            if (!player.isValid() || excluded.contains(uuid)) continue;
            current.add(uuid);
            if (viewers.add(uuid)) player.showBossBar(bar);
        }
        viewers.removeIf(uuid -> {
            if (current.contains(uuid)) return false;
            hideFrom(uuid);
            return true;
        });
    }

    private void hideFrom(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) player.hideBossBar(bar);
    }

    public float getProgress() {
        return chargeTime <= 0 ? 1f : Math.max(0f, Math.min(1f, (float) time / chargeTime));
    }

    public void setProgress(float progress) {
        float clamped = Math.max(0f, Math.min(1f, progress));
        time = Math.round(clamped * chargeTime);
        bar.progress(clamped);
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = Math.max(0, Math.min(chargeTime, time));
    }

    public int getChargeTime() {
        return chargeTime;
    }

    public void setChargeTime(int chargeTime) {
        this.chargeTime = chargeTime;
    }

    public Component getTitle() {
        return bar.name();
    }

    public void setTitle(Component title) {
        bar.name(title);
    }

    public BossBar.Color getColor() {
        return bar.color();
    }

    public void setColor(BossBar.Color color) {
        bar.color(color);
    }

    public BossBar.Overlay getOverlay() {
        return bar.overlay();
    }

    public void setOverlay(BossBar.Overlay overlay) {
        bar.overlay(overlay);
    }
}
