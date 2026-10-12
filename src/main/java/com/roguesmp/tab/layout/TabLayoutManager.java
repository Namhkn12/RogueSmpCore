package com.roguesmp.tab.layout;

import com.roguesmp.tab.TabContext;
import com.roguesmp.tab.scoreboard.TabScoreboardView;
import com.roguesmp.tab.tablist.TabListView;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

public final class TabLayoutManager {

    private final TabContext context;
    private final Map<String, TabClaim> claims = new LinkedHashMap<>();

    private @Nullable Player player;
    private @Nullable TabClaim scoreboardClaim;
    private @Nullable TabScoreboardView scoreboardView;
    private @Nullable TabClaim tabListClaim;
    private @Nullable TabListView tabListView;

    public TabLayoutManager(TabContext context) {
        this.context = context;
    }

    public void claim(TabClaim claim) {
        claims.remove(claim.key());
        claims.put(claim.key(), claim);
        resolve();
    }

    public void release(String key) {
        if (claims.remove(key) != null) resolve();
    }

    public void clear() {
        claims.clear();
        resolve();
    }

    public void attach(Player player) {
        this.player = player;
        scoreboardClaim = null;
        scoreboardView = null;
        tabListClaim = null;
        tabListView = null;
        resolve();
    }

    public void detach() {
        player = null;
        scoreboardClaim = null;
        scoreboardView = null;
        tabListClaim = null;
        tabListView = null;
    }

    private void resolve() {
        if (player == null) return;
        resolveScoreboard(winner(TabClaim::scoreboard));
        resolveTabList(winner(TabClaim::tabList));
    }

    private void resolveScoreboard(@Nullable TabClaim winner) {
        if (winner == scoreboardClaim) return;
        if (winner == null) {
            TabScoreboardView previous = scoreboardView;
            scoreboardClaim = null;
            scoreboardView = null;
            if (previous != null) previous.destroy();
            return;
        }
        TabScoreboardView next = winner.scoreboard().create(player, context);
        next.show();
        scoreboardClaim = winner;
        scoreboardView = next;
    }

    private void resolveTabList(@Nullable TabClaim winner) {
        if (winner == tabListClaim) return;
        if (winner == null) {
            TabListView previous = tabListView;
            tabListClaim = null;
            tabListView = null;
            if (previous != null) previous.destroy();
            return;
        }
        TabListView next = winner.tabList().create(player, context);
        next.show();
        tabListClaim = winner;
        tabListView = next;
    }

    private @Nullable TabClaim winner(Function<TabClaim, ?> channel) {
        TabClaim best = null;
        for (TabClaim claim : claims.values()) {
            if (channel.apply(claim) == null) continue;
            if (best == null || claim.priority() >= best.priority()) best = claim;
        }
        return best;
    }
}
