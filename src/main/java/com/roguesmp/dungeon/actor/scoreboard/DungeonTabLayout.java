package com.roguesmp.dungeon.actor.scoreboard;

import com.roguesmp.dungeon.data.definition.Dungeon;
import com.roguesmp.dungeon.data.definition.objective.IDisplayable;
import com.roguesmp.dungeon.data.runtime.DungeonInstance;
import com.roguesmp.dungeon.data.runtime.RoomInstance;
import com.roguesmp.tab.element.TabElement;
import com.roguesmp.tab.layout.TabClaim;
import com.roguesmp.tab.scoreboard.ScoreboardDefaults;
import com.roguesmp.tab.scoreboard.TabScoreboardView;

import java.util.List;

public final class DungeonTabLayout {

    public static final String KEY = "dungeon";

    private static final int PRIORITY = 30;
    private static final int OBJECTIVE_LINES = 4;

    private DungeonTabLayout() {
    }

    public static TabClaim claim(Dungeon template, DungeonInstance instance) {
        return TabClaim.scoreboard(KEY, PRIORITY, (player, context) -> {
            TabScoreboardView.Builder builder = TabScoreboardView.builder(player, "dungeon_scoreboard")
                    .title(ScoreboardDefaults.title())
                    .line(ScoreboardDefaults.divider())
                    .line("<gold>Phó bản: <yellow>" + template.getName())
                    .line(TabElement.ticking(20, c -> timeLine(instance.getTimer().getRemainingSeconds())))
                    .line(TabElement.ticking(20, c -> "<gold>Điểm: <white>" + instance.getProgress().getScore()))
                    .line(ScoreboardDefaults.spacer())
                    .line("<red><bold>Mục tiêu");
            for (int i = 0; i < OBJECTIVE_LINES; i++) {
                int index = i;
                builder.line(TabElement.ticking(20, c -> objectiveLine(instance, index)));
            }
            return builder.line(ScoreboardDefaults.spacer()).build(context);
        });
    }

    private static String timeLine(int seconds) {
        String color = seconds <= 30 ? "red" : seconds <= 60 ? "yellow" : "green";
        return "<gold>Thời gian: <" + color + ">" + String.format("%02d:%02d", Math.max(0, seconds) / 60, Math.max(0, seconds) % 60);
    }

    private static String objectiveLine(DungeonInstance instance, int index) {
        List<String> lines = objectiveLines(instance);
        return index < lines.size() ? " " + lines.get(index) : "";
    }

    private static List<String> objectiveLines(DungeonInstance instance) {
        RoomInstance room = instance.getProgress().getCurrentRoom();
        if (room == null || room.getActiveObjectives() == null) return List.of();
        return room.getActiveObjectives().stream()
                .filter(IDisplayable.class::isInstance)
                .flatMap(objective -> ((IDisplayable) objective).getScoreBoardLine().stream())
                .toList();
    }
}
