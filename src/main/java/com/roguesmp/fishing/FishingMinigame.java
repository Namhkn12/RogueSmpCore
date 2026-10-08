package com.roguesmp.fishing;

import com.roguesmp.text.Glyphs;
import net.kyori.adventure.text.format.TextColor;

import java.util.concurrent.ThreadLocalRandom;

public final class FishingMinigame {

    public enum State { RUNNING, CAUGHT, ESCAPED }

    public static final int BAR_SEGMENT_PITCH = Glyphs.FISHING_BAR_SEGMENT.width() - 1;

    private static final double INDICATOR_ACCELERATION = 0.4;
    private static final double INDICATOR_DRAG = 0.9;
    private static final double WALL_BOUNCE = 0.3;
    private static final double BAR_ACCELERATION = 0.15;
    private static final double BAR_ARRIVAL_DISTANCE = 1.0;

    private final FishingMinigameSettings settings;
    private final int trackWidth = Glyphs.FISHING_TRACK.width();
    private final int barWidth;
    private final int indicatorWidth = Glyphs.FISHING_INDICATOR.width();
    private final double progressPerTick;

    private double barPosition;
    private double barVelocity;
    private double barTarget;

    private double indicatorPosition;
    private double indicatorVelocity;

    private double progress;

    public FishingMinigame(FishingMinigameSettings settings) {
        this.settings = settings;
        this.barWidth = settings.barSegments() * BAR_SEGMENT_PITCH;
        this.progressPerTick = 1.0 / settings.durationTicks();
        this.progress = settings.startProgress();

        this.barPosition = (trackWidth - barWidth) / 2.0;
        this.barTarget = barPosition;
        this.indicatorPosition = (trackWidth - indicatorWidth) / 2.0;
    }

    public State tick(boolean pushing) {
        moveBar();
        moveIndicator(pushing);

        progress += isIndicatorInsideBar() ? progressPerTick : -progressPerTick * settings.lossMultiplier();
        progress = Math.min(1.0, progress);

        if (progress >= 1.0) return State.CAUGHT;
        if (progress <= 0.0) return State.ESCAPED;
        return State.RUNNING;
    }

    private void moveBar() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double maxPosition = trackWidth - barWidth;

        if (Math.abs(barTarget - barPosition) < BAR_ARRIVAL_DISTANCE || random.nextDouble() < settings.barTurnChance()) {
            barTarget = random.nextDouble(0, maxPosition + 1);
        }

        barVelocity += Math.signum(barTarget - barPosition) * BAR_ACCELERATION;
        barVelocity = Math.max(-settings.barMaxSpeed(), Math.min(settings.barMaxSpeed(), barVelocity));
        barPosition = Math.max(0, Math.min(maxPosition, barPosition + barVelocity));
    }

    private void moveIndicator(boolean pushing) {
        double maxPosition = trackWidth - indicatorWidth;

        indicatorVelocity += pushing ? INDICATOR_ACCELERATION : -INDICATOR_ACCELERATION;
        indicatorVelocity *= INDICATOR_DRAG;
        indicatorPosition += indicatorVelocity;

        if (indicatorPosition < 0) {
            indicatorPosition = 0;
            indicatorVelocity = -indicatorVelocity * WALL_BOUNCE;
        } else if (indicatorPosition > maxPosition) {
            indicatorPosition = maxPosition;
            indicatorVelocity = -indicatorVelocity * WALL_BOUNCE;
        }
    }

    private boolean isIndicatorInsideBar() {
        double center = indicatorPosition + indicatorWidth / 2.0;
        return center >= barPosition && center <= barPosition + barWidth;
    }

    public int barPosition() {
        return (int) Math.round(barPosition);
    }

    public int barWidth() {
        return barWidth;
    }

    public TextColor barColor() {
        return settings.barColor();
    }

    public int barSegments() {
        return settings.barSegments();
    }

    public int indicatorPosition() {
        return (int) Math.round(indicatorPosition);
    }

    public double progress() {
        return progress;
    }
}
