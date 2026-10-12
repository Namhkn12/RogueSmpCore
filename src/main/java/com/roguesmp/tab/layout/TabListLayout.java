package com.roguesmp.tab.layout;

import com.roguesmp.tab.TabContext;
import com.roguesmp.tab.tablist.TabListView;
import org.bukkit.entity.Player;

@FunctionalInterface
public interface TabListLayout {

    TabListView create(Player player, TabContext context);
}
