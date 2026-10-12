package com.roguesmp.tab.layout;

import org.jetbrains.annotations.Nullable;

public record TabClaim(String key, int priority, @Nullable ScoreboardLayout scoreboard, @Nullable TabListLayout tabList) {

    public static TabClaim scoreboard(String key, int priority, ScoreboardLayout scoreboard) {
        return new TabClaim(key, priority, scoreboard, null);
    }

    public static TabClaim tabList(String key, int priority, TabListLayout tabList) {
        return new TabClaim(key, priority, null, tabList);
    }
}
