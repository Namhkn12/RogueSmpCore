package com.roguesmp.dungeon_static;

import com.roguesmp.tab.element.TabElement;
import com.roguesmp.tab.layout.TabClaim;
import com.roguesmp.tab.scoreboard.ScoreboardDefaults;
import com.roguesmp.tab.scoreboard.TabScoreboardView;

import java.time.Duration;

public final class StaticDungeonTabLayout {

    public static final String KEY = "static_dungeon";

    private static final int PRIORITY = 30;
    private static final long WARNING_MILLIS = 5 * 60_000L;

    private StaticDungeonTabLayout() {
    }

    public static TabClaim claim(StaticDungeonInstance instance) {
        return TabClaim.scoreboard(KEY, PRIORITY, (player, context) -> TabScoreboardView.builder(player, "static_dungeon_scoreboard")
                .title(ScoreboardDefaults.title())
                .line(ScoreboardDefaults.divider())
                .line(ScoreboardDefaults.regionLine())
                .line("<gold>Phó bản: <white>" + StaticDungeonInfo.displayNameOf(instance.dungeonId()))
                .line(TabElement.ticking(20, c -> timeLeftLine(instance)))
                .lines(ScoreboardDefaults.playerInfo(player))
                .line(ScoreboardDefaults.spacer())
                .build(context));
    }

    private static String timeLeftLine(StaticDungeonInstance instance) {
        long remaining = Math.max(0, instance.expiresAt() - System.currentTimeMillis());
        String color = remaining <= WARNING_MILLIS ? "red" : "green";
        return "<gold>Còn lại: <" + color + ">" + formatRemaining(remaining);
    }

    private static String formatRemaining(long millis) {
        Duration duration = Duration.ofMillis(millis);
        long days = duration.toDays();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();

        if (days > 0) return days + "d " + hours + "h";
        if (hours > 0) return hours + "h " + minutes + "m";
        if (minutes > 0) return minutes + "m " + seconds + "s";
        return seconds + "s";
    }
}
