package com.roguesmp.dungeon.actor.scoreboard.Impl;

import com.roguesmp.dungeon.actor.scoreboard.IScoreBoardRenderer;
import com.roguesmp.dungeon.data.Dungeon;
import com.roguesmp.dungeon.dto.DungeonScoreBoard;
import com.roguesmp.dungeon.instance.DungeonInstance;
import fr.mrmicky.fastboard.FastBoard;
import org.bukkit.entity.Player;

import java.util.List;

public class FastBoardRenderer implements IScoreBoardRenderer {

    private final FastBoard board;
    private final String[] lines;

    private static final int IDX_DUNGEON_NAME  = 1;
    private static final int IDX_TIME          = 4;
    private static final int IDX_SCORE         = 7;
    private static final int IDX_OBJ_HEADER    = 9;
    private static final int IDX_OBJ_START     = 10;
    private static final int MAX_OBJ_LINES     = 4;
    private static final int IDX_SEP_MEMBER    = IDX_OBJ_START + MAX_OBJ_LINES;     // 14
    private static final int IDX_MEMBER_START  = IDX_SEP_MEMBER + 1;                // 15
    private static final int MAX_MEMBERS       = 4;
    private static final int TOTAL_LINES       = IDX_MEMBER_START + MAX_MEMBERS + 1; // 20

    public FastBoardRenderer(Player player) {
        this.board = new FastBoard(player);
        this.lines = new String[TOTAL_LINES];
        board.updateTitle("§6⚔ DUNGEON");
    }

    // ----------------------------------------------------------------
    // INIT
    // ----------------------------------------------------------------
    public void init(Dungeon template, DungeonInstance instance, List<DungeonScoreBoard.PartyMember> members) {
        lines[0]               = "§7──────────";
        lines[IDX_DUNGEON_NAME]= "§e" + template.getDgName();
        lines[2]               = "§7──────────";
        lines[3]               = "§eTime";
        lines[IDX_TIME]        = buildTime(instance.getRemainingSeconds());
        lines[5]               = "";
        lines[6]               = "§eScore";
        lines[IDX_SCORE]       = buildScore((int) instance.getScore());
        lines[8]               = "§7──────────";
        lines[IDX_OBJ_HEADER]  = "§eMục tiêu";

        for (int i = 0; i < MAX_OBJ_LINES; i++) {
            lines[IDX_OBJ_START + i] = "";
        }

        lines[IDX_SEP_MEMBER] = "§7──────────";

        for (int i = 0; i < MAX_MEMBERS; i++) {
            lines[IDX_MEMBER_START + i] = i < members.size()
                    ? buildMember(members.get(i))
                    : "";
        }

        lines[IDX_MEMBER_START + MAX_MEMBERS] = "§7──────────";

        flush();
    }

    // ----------------------------------------------------------------
    // UPDATE API
    // ----------------------------------------------------------------

    public void updateTime(int remainingSeconds) {
        lines[IDX_TIME] = buildTime(remainingSeconds);
    }

    public void updateScore(int score) {
        lines[IDX_SCORE] = buildScore(score);
    }

    public void updateObjectives(List<List<String>> objectives) {
        List<String> flat = objectives.stream()
                .flatMap(obj -> obj.stream().map(l -> "  " + l))
                .toList();

        for (int i = 0; i < MAX_OBJ_LINES; i++) {
            lines[IDX_OBJ_START + i] = i < flat.size() ? flat.get(i) : "";
        }
    }

    public void updateMember(int slotIndex, DungeonScoreBoard.PartyMember member) {
        if (slotIndex < 0 || slotIndex >= MAX_MEMBERS) return;
        lines[IDX_MEMBER_START + slotIndex] = buildMember(member);
    }

    public void clearMemberSlot(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= MAX_MEMBERS) return;
        lines[IDX_MEMBER_START + slotIndex] = "";
    }

    public void flush() {
        board.updateLines(lines);
    }

    public void destroy() {
        board.delete();
    }

    // ----------------------------------------------------------------
    // BUILDERS
    // ----------------------------------------------------------------

    private static String buildTime(int seconds) {
        if (seconds <= 0) return "  §c00:00";
        int m = seconds / 60;
        int s = seconds % 60;
        String color = seconds <= 30 ? "§c" : seconds <= 60 ? "§e" : "§a";
        return "  " + color + String.format("%02d:%02d", m, s);
    }

    private static String buildScore(int score) {
        return "  §f" + score;
    }

    private static String buildMember(DungeonScoreBoard.PartyMember m) {
        return m.getStatusIcon() + "§f" + m.getDisplayName();
    }
}