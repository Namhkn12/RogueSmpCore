package com.roguesmp.tab.scoreboard;

import com.roguesmp.tab.ElementBinding;
import com.roguesmp.tab.TabContext;
import com.roguesmp.tab.TabEngine;
import com.roguesmp.tab.TabView;
import com.roguesmp.tab.element.TabElement;
import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.TabPlayer;
import me.neznamy.tab.api.scoreboard.Line;
import me.neznamy.tab.api.scoreboard.Scoreboard;
import me.neznamy.tab.api.scoreboard.ScoreboardManager;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * A per-player TAB scoreboard where each line is backed by a {@link TabElement}. Updating one line calls
 * TAB's {@link Line#setText} directly, so it never touches any of the other lines.
 */
public final class TabScoreboardView implements TabView {

    private final Player player;
    private final TabPlayer tabPlayer;
    private final Scoreboard scoreboard;
    private final List<ElementBinding> bindings = new ArrayList<>();
    private final ElementBinding titleBinding;

    private TabScoreboardView(Player player, String name, TabElement title, List<TabElement> lines, TabContext context) {
        this.player = player;
        this.tabPlayer = TabEngine.tabPlayer(player);
        if (tabPlayer == null) {
            throw new IllegalStateException("Player " + player.getName() + " is not yet loaded by TAB; check TabEngine.isReady() first.");
        }

        ScoreboardManager sm = Objects.requireNonNull(TabAPI.getInstance().getScoreboardManager());
        List<String> initialText = lines.stream().map(el -> el.render(context)).toList();
        this.scoreboard = sm.createScoreboard(name, title.render(context), initialText);

        List<Line> tabLines = scoreboard.getLines();
        for (int i = 0; i < lines.size(); i++) {
            Line line = tabLines.get(i);
            bindings.add(new ElementBinding(context, lines.get(i), binding -> line.setText(binding.render())));
        }
        this.titleBinding = new ElementBinding(context, title, binding -> scoreboard.setTitle(binding.render()));

        TabEngine.getInstance().register(this);
    }

    /** Starts building a scoreboard for {@code player}. Call {@link Builder#build} to finish. */
    public static Builder builder(Player player, String name) {
        return new Builder(player, name);
    }

    /** Shows this scoreboard to the player. */
    public void show() {
        Objects.requireNonNull(TabAPI.getInstance().getScoreboardManager()).showScoreboard(tabPlayer, scoreboard);
    }

    /** Swaps the element rendering a given line, unbinding the old element's context listeners. */
    public void setLine(int index, TabElement element) {
        ElementBinding binding = bindings.get(index);
        binding.replace(element);
        binding.refresh();
    }

    /** Swaps the element rendering the title, unbinding the old element's context listeners. */
    public void setTitle(TabElement element) {
        titleBinding.replace(element);
        titleBinding.refresh();
    }

    /** Re-renders every line and the title right now, even the ones with no changed dependency. */
    public void refreshAll() {
        bindings.forEach(ElementBinding::refresh);
        titleBinding.refresh();
    }

    @Override
    public UUID playerId() {
        return player.getUniqueId();
    }

    @Override
    public void tick() {
        titleBinding.tick();
        bindings.forEach(ElementBinding::tick);
    }

    /** Stops updating this view and removes it. With {@code restoreDefaults}, switches the player to the library default scoreboard first. */
    @Override
    public void destroy(boolean restoreDefaults) {
        TabEngine.getInstance().unregister(this);
        titleBinding.unbind();
        bindings.forEach(ElementBinding::unbind);
        if (restoreDefaults) {
            Objects.requireNonNull(TabAPI.getInstance().getScoreboardManager()).showScoreboard(tabPlayer, TabEngine.defaultScoreboard(player));
        }
        scoreboard.unregister();
    }

    /** Builds a {@link TabScoreboardView} one title/line at a time. */
    public static final class Builder {
        private final Player player;
        private final String name;
        private TabElement title = TabElement.constant("");
        private final List<TabElement> lines = new ArrayList<>();

        private Builder(Player player, String name) {
            this.player = player;
            this.name = name;
        }

        /** Sets a fixed title. */
        public Builder title(String text) {
            return title(TabElement.constant(text));
        }

        /** Sets the title element. */
        public Builder title(TabElement element) {
            this.title = element;
            return this;
        }

        /** Adds a fixed line. */
        public Builder line(String text) {
            return line(TabElement.constant(text));
        }

        /** Adds a line element. Lines render top to bottom in the order they're added. */
        public Builder line(TabElement element) {
            this.lines.add(element);
            return this;
        }

        /** Adds several line elements in order. */
        public Builder lines(List<TabElement> elements) {
            this.lines.addAll(elements);
            return this;
        }

        /** Creates the scoreboard and its initial content, using {@code context} for any elements that need it. Call {@link #show} to display it. */
        public TabScoreboardView build(TabContext context) {
            return new TabScoreboardView(player, name, title, List.copyOf(lines), context);
        }
    }
}
