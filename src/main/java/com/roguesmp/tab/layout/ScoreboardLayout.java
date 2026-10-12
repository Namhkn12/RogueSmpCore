package com.roguesmp.tab.layout;

import com.roguesmp.tab.TabContext;
import com.roguesmp.tab.scoreboard.TabScoreboardView;
import org.bukkit.entity.Player;

@FunctionalInterface
public interface ScoreboardLayout {

    TabScoreboardView create(Player player, TabContext context);
}
