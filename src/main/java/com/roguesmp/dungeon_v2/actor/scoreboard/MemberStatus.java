package com.roguesmp.dungeon_v2.actor.scoreboard;

public enum MemberStatus {
    ALIVE,
    DEAD,
    OFFLINE;

    public String getIcon() {
        return switch (this) {
            case ALIVE    -> "§a● ";  // xanh
            case DEAD     -> "§c✦ ";  // đỏ
            case OFFLINE  -> "§8○ ";  // xám
        };
    }
}
