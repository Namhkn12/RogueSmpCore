package com.roguesmp.dungeon.dto;

import com.roguesmp.dungeon.actor.scoreboard.MemberStatus;
import java.util.Collections;
import java.util.List;

public class DungeonScoreBoard {
    private String dungeonName;
    private int score;
    private List<PartyMember> members;
    private List<List<String>> objectiveLines;

    // DungeonScoreBoard.java
    public static class PartyMember {
        private final String displayName;
        private final MemberStatus status;

        public PartyMember(String displayName, MemberStatus status) {
            this.displayName = displayName;
            this.status = status;
        }

        public String getDisplayName() { return displayName; }

        // getStatusIcon() bạn đang thiếu — delegate sang enum
        public String getStatusIcon() {
            return status.getIcon();
        }

        public MemberStatus getStatus() { return status; }
    }

    public DungeonScoreBoard(String dungeonName, int score, List<PartyMember> members) {
        this.dungeonName = dungeonName;
        this.score = score;
        this.members = members;
    }

    public DungeonScoreBoard() {
    }

    public String getDungeonName() {
        return dungeonName;
    }

    public void setDungeonName(String dungeonName) {
        this.dungeonName = dungeonName;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public List<PartyMember> getMembers() {
        return Collections.unmodifiableList(members);
    }

    public void setMembers(List<PartyMember> members) {
        this.members = members;
    }
}
