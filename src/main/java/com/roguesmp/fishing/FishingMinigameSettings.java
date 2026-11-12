package com.roguesmp.fishing;

import net.kyori.adventure.text.format.TextColor;

public record FishingMinigameSettings(
        int barSegments,
        double barTurnChance,
        TextColor barColor,
        int durationTicks,
        double startProgress,
        double lossMultiplier,
        double barMaxSpeed
) {

    private static final int DURATION_TICKS = 80;
    private static final double START_PROGRESS = 0.5;
    private static final double LOSS_MULTIPLIER = 0.6;
    private static final double BAR_MAX_SPEED = 1.6;

    private static final int MIN_BAR_SEGMENTS = 1;
    private static final int MAX_BAR_SEGMENTS = 20;

    public FishingMinigameSettings {
        barSegments = Math.max(MIN_BAR_SEGMENTS, Math.min(MAX_BAR_SEGMENTS, barSegments));
        durationTicks = Math.max(1, durationTicks);
    }

    public static FishingMinigameSettings of(FishTier tier) {
        return new FishingMinigameSettings(tier.barSegments(), tier.barTurnChance(), tier.color(), DURATION_TICKS, START_PROGRESS, LOSS_MULTIPLIER, BAR_MAX_SPEED);
    }

    public FishingMinigameSettings withBarSegments(int barSegments) {
        return new FishingMinigameSettings(barSegments, barTurnChance, barColor, durationTicks, startProgress, lossMultiplier, barMaxSpeed);
    }

    public FishingMinigameSettings withBarTurnChance(double barTurnChance) {
        return new FishingMinigameSettings(barSegments, barTurnChance, barColor, durationTicks, startProgress, lossMultiplier, barMaxSpeed);
    }

    public FishingMinigameSettings withBarColor(TextColor barColor) {
        return new FishingMinigameSettings(barSegments, barTurnChance, barColor, durationTicks, startProgress, lossMultiplier, barMaxSpeed);
    }

    public FishingMinigameSettings withDurationTicks(int durationTicks) {
        return new FishingMinigameSettings(barSegments, barTurnChance, barColor, durationTicks, startProgress, lossMultiplier, barMaxSpeed);
    }

    public FishingMinigameSettings withStartProgress(double startProgress) {
        return new FishingMinigameSettings(barSegments, barTurnChance, barColor, durationTicks, startProgress, lossMultiplier, barMaxSpeed);
    }

    public FishingMinigameSettings withLossMultiplier(double lossMultiplier) {
        return new FishingMinigameSettings(barSegments, barTurnChance, barColor, durationTicks, startProgress, lossMultiplier, barMaxSpeed);
    }

    public FishingMinigameSettings withBarMaxSpeed(double barMaxSpeed) {
        return new FishingMinigameSettings(barSegments, barTurnChance, barColor, durationTicks, startProgress, lossMultiplier, barMaxSpeed);
    }
}
