package com.roguesmp.fishing;

import com.roguesmp.text.Glyphs;
import com.roguesmp.text.Layout;
import com.roguesmp.text.Space;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

public final class FishingMinigameRenderer {

    private static final int PROGRESS_CELLS = 20;
    private static final String PROGRESS_CELL = "█";

    private FishingMinigameRenderer() {
    }

    /**
     * Cursor math: every layer's net advance is exactly {@code track.width()}, so the three
     * separately centered displays stay aligned as the bar and indicator move.
     */
    public static Component trackLayer() {
        return Glyphs.FISHING_TRACK.create();
    }

    public static Component barLayer(FishingMinigame game) {
        int barStart = game.barPosition();
        int barEnd = barStart + game.barWidth();

        return Layout.builder()
                .moveThen(barStart, barStrip(game.barSegments(), game.barColor()))
                .move(Glyphs.FISHING_TRACK.width() - barEnd)
                .component();
    }

    public static Component indicatorLayer(FishingMinigame game) {
        int indicatorStart = game.indicatorPosition();

        return Layout.builder()
                .moveThen(indicatorStart, Glyphs.FISHING_INDICATOR)
                .move(Glyphs.FISHING_TRACK.width() - indicatorStart - Glyphs.FISHING_INDICATOR.width())
                .component();
    }

    private static Component barStrip(int segments, TextColor color) {
        Component strip = Component.empty();
        for (int i = 0; i < segments; i++) {
            strip = strip.append(Glyphs.FISHING_BAR_SEGMENT.create().color(color)).append(Space.px(-1));
        }
        return strip;
    }

    public static Component progressBar(FishingMinigame game) {
        int filled = (int) Math.round(game.progress() * PROGRESS_CELLS);

        return Component.text(PROGRESS_CELL.repeat(filled), NamedTextColor.GREEN)
                .append(Component.text(PROGRESS_CELL.repeat(PROGRESS_CELLS - filled), NamedTextColor.DARK_GRAY));
    }
}
