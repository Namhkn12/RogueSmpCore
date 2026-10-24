package com.roguesmp.entity.boss.hollowheart;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;

public record BarState(Component title, float progress, BossBar.Color color) {

    public static BarState countdown(Component title, long remaining, long total, BossBar.Color color) {
        return new BarState(title, ratio(remaining, total), color);
    }

    public static BarState fill(Component title, long done, long total, BossBar.Color color) {
        return new BarState(title, ratio(done, total), color);
    }

    public BarState withTitle(Component newTitle) {
        return new BarState(newTitle, progress, color);
    }

    private static float ratio(long value, long total) {
        if (total <= 0) return 1f;
        return Math.max(0f, Math.min(1f, (float) value / total));
    }
}
