package com.roguesmp.fishing;

import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.Nullable;

public record FishingMinigameSettings(int barSegments, double barTurnChance, TextColor barColor) {

    private static final int DURATION_TICKS = 80;
    private static final double START_PROGRESS = 0.5;
    private static final double LOSS_MULTIPLIER = 0.6;
    private static final double BAR_MAX_SPEED = 1.6;

    private static final int MIN_BAR_SEGMENTS = 1;
    private static final int MAX_BAR_SEGMENTS = 20;

    public static FishingMinigameSettings of(FishTier tier, @Nullable FishingHotspot hotspot) {
        TextColor color = tier.color();
        if (hotspot == null) return new FishingMinigameSettings(tier.barSegments(), tier.barTurnChance(), color);

        int segments = Math.max(MIN_BAR_SEGMENTS, Math.min(MAX_BAR_SEGMENTS, tier.barSegments() + hotspot.barSegmentsBonus()));
        return new FishingMinigameSettings(segments, tier.barTurnChance() * hotspot.barTurnChanceMultiplier(), color);
    }

    public int durationTicks() {
        return DURATION_TICKS;
    }

    public double startProgress() {
        return START_PROGRESS;
    }

    public double lossMultiplier() {
        return LOSS_MULTIPLIER;
    }

    public double barMaxSpeed() {
        return BAR_MAX_SPEED;
    }
}
