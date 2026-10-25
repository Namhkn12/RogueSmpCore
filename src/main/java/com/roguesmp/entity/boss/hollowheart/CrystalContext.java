package com.roguesmp.entity.boss.hollowheart;

import org.bukkit.entity.LivingEntity;

public record CrystalContext(LivingEntity crystal, Arena arena, CrystalPhase phase, int empower, boolean overlay) {

    private static final double TIMER_FACTOR = 0.8;
    private static final double WARN_FACTOR = 0.75;
    private static final double HEAL_FACTOR = 1.5;
    private static final double OVERLAY_WARN_FACTOR = 1.25;

    public int timer(int baseTicks) {
        return (int) Math.max(20, baseTicks * Math.pow(TIMER_FACTOR, empower));
    }

    public int warning(int baseTicks) {
        double ticks = baseTicks * Math.pow(WARN_FACTOR, empower) * (overlay ? OVERLAY_WARN_FACTOR : 1);
        return (int) Math.max(10, ticks);
    }

    public double healMultiplier() {
        return Math.pow(HEAL_FACTOR, empower);
    }
}
