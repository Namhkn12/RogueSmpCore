package com.roguesmp.dungeon_v2.actor.scoreboard;

import com.roguesmp.dungeon_v2.data.definition.Dungeon;
import com.roguesmp.dungeon_v2.data.definition.objective.IDisplayable;
import com.roguesmp.dungeon_v2.data.runtime.DungeonInstance;
import com.roguesmp.dungeon_v2.data.runtime.RoomInstance;
import com.roguesmp.dungeon_v2.data.definition.objective.IObjective;
import com.roguesmp.dungeon_v2.expansion.DungeonExpansion;
import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.TabPlayer;
import me.neznamy.tab.api.scoreboard.Scoreboard;
import me.neznamy.tab.api.scoreboard.ScoreboardManager;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class TabBoardRenderer {

    private final TabPlayer tabPlayer;
    private final UUID playerId;
    private final DungeonExpansion papi;
    private Scoreboard scoreboard;

    private static final List<String> LAYOUT = List.of(
            "&c&lᴅᴜɴɢᴇᴏɴ ",
            "&c▎&f ɴᴀᴍᴇ &#D1D1D1%dungeon_name% ",
            "&c▎&f ᴛɪᴍᴇ &#D1D1D1%dungeon_time% ",
            "&c▎&f ꜱᴄᴏʀᴇ &#D1D1D1%dungeon_score% ",

            "&c&lᴏʙᴊᴇᴄᴛɪᴠᴇѕ ",
            "&c▎&f &#D1D1D1%dungeon_obj_0% ",
            "&c▎&f &#D1D1D1%dungeon_obj_1% ",
            "&c▎&f &#D1D1D1%dungeon_obj_2% ",
            "&c▎&f &#D1D1D1%dungeon_obj_3% ",

            "&7&nplay.yourserver.net "
    );

    public TabBoardRenderer(Player player, DungeonExpansion papi) {
        this.playerId  = player.getUniqueId();
        this.papi      = papi;
        this.tabPlayer = TabAPI.getInstance().getPlayer(playerId);

        ScoreboardManager sm = TabAPI.getInstance().getScoreboardManager();
        if (tabPlayer == null || sm == null) return;

        sm.resetScoreboard(tabPlayer);
        this.scoreboard = sm.createScoreboard("dungeon_" + playerId, "    - &f&lYour Server -    ", LAYOUT);
    }

    public void init(Dungeon template, DungeonInstance instance) {
        papi.setName(playerId, "§e" + template.getName());
        papi.setTime(playerId, buildTime(instance.getTimer().getRemainingSeconds()));
        papi.setScore(playerId, buildScore(instance.getProgress().getScore()));
        papi.setObjectives(playerId, new String[]{"", "", "", ""});

        Objects.requireNonNull(TabAPI.getInstance().getScoreboardManager())
                .showScoreboard(tabPlayer, scoreboard);
    }

    public void updateTime(int remainingSeconds) {
        papi.setTime(playerId, buildTime(remainingSeconds));
    }

    public void updateScore(int score) {
        papi.setScore(playerId, buildScore(score));
    }

    public void updateObjectives(List<List<String>> objectives) {
        List<String> flat = objectives.stream()
                .flatMap(obj -> obj.stream().map(l -> "  " + l))
                .toList();

        String[] arr = new String[4];
        for (int i = 0; i < 4; i++) {
            arr[i] = i < flat.size() ? flat.get(i) : "";
        }
        papi.setObjectives(playerId, arr);
    }

    public void tick(DungeonInstance instance) {
        updateTime(instance.getTimer().getRemainingSeconds());
        updateScore(instance.getProgress().getScore());

        RoomInstance room = instance.getProgress().getCurrentRoom();
        if (room != null && room.getActiveObjectives() != null) {
            List<List<String>> objectiveLines = room.getActiveObjectives().stream()
                    .filter(obj -> obj instanceof IDisplayable)
                    .map(obj -> ((IDisplayable) obj).getScoreBoardLine())
                    .toList();
            updateObjectives(objectiveLines);
        }
    }

    public void destroy() {
        papi.clear(playerId);
        TabAPI.getInstance().getScoreboardManager().resetScoreboard(tabPlayer);
    }

    private static String buildTime(int seconds) {
        if (seconds <= 0) return "  §c00:00";
        int m = seconds / 60, s = seconds % 60;
        String color = seconds <= 30 ? "§c" : seconds <= 60 ? "§e" : "§a";
        return "  " + color + String.format("%02d:%02d", m, s);
    }

    private static String buildScore(int score) {
        return "  §f" + score;
    }
}