package com.roguesmp.dungeon.actor.scoreboard.Impl;

import com.roguesmp.dungeon.actor.scoreboard.IScoreBoardRenderer;
import com.roguesmp.dungeon.data.Dungeon;
import com.roguesmp.dungeon.dto.DungeonScoreBoard;
import com.roguesmp.dungeon.expansion.DungeonExpansion;
import com.roguesmp.dungeon.instance.DungeonInstance;
import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.TabPlayer;
import me.neznamy.tab.api.scoreboard.Scoreboard;
import me.neznamy.tab.api.scoreboard.ScoreboardManager;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class TabBoardRenderer implements IScoreBoardRenderer {

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

            "&c&lᴘᴀʀᴛʏ ",
            "&c▎&f &#D1D1D1%dungeon_member_0% ",
            "&c▎&f &#D1D1D1%dungeon_member_1% ",
            "&c▎&f &#D1D1D1%dungeon_member_2% ",
            "&c▎&f &#D1D1D1%dungeon_member_3% ",

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

    @Override
    public void init(Dungeon template, DungeonInstance instance, List<DungeonScoreBoard.PartyMember> members) {
        papi.setName(playerId, "§e" + template.getDgName());
        papi.setTime(playerId, buildTime(instance.getRemainingSeconds()));
        papi.setScore(playerId, buildScore((int) instance.getScore()));

        String[] memberArr = new String[4];
        for (int i = 0; i < 4; i++) {
            memberArr[i] = i < members.size() ? buildMember(members.get(i)) : "";
        }
        papi.setMembers(playerId, memberArr);
        papi.setObjectives(playerId, new String[]{"", "", "", ""});

        Objects.requireNonNull(TabAPI.getInstance().getScoreboardManager()).showScoreboard(tabPlayer, scoreboard);
    }

    @Override
    public void updateTime(int remainingSeconds) {
        papi.setTime(playerId, buildTime(remainingSeconds));
    }

    @Override
    public void updateScore(int score) {
        papi.setScore(playerId, buildScore(score));
    }

    @Override
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

    @Override
    public void updateMember(int slotIndex, DungeonScoreBoard.PartyMember member) {
        // rebuild array giữ nguyên các slot khác
        // cần lưu lại memberArr hoặc đọc lại từ papi nếu cần
    }

    @Override
    public void clearMemberSlot(int slotIndex) {

    }

    @Override
    public void flush() { /* no-op */ }

    @Override
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

    private static String buildScore(int score)              { return "  §f" + score; }
    private static String buildMember(DungeonScoreBoard.PartyMember m) {
        return m.getStatusIcon() + "§f" + m.getDisplayName();
    }
}