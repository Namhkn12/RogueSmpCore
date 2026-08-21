package com.roguesmp.tab.scoreboard;

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

    /** Ties one {@link TabElement} to the TAB {@link Line} it renders into, and to the context listeners that keep it updated. */
    private final class Binding {
        TabElement element;
        Line line; // null for the title binding
        int ticksSinceRefresh;
        final List<Runnable> unbindTasks = new ArrayList<>();

        Binding(TabElement element, Line line) {
            this.element = element;
            this.line = line;
            bind();
        }

        void bind() {
            for (String dep : element.dependencies()) {
                java.util.function.Consumer<TabContext> listener = ctx -> refresh(this);
                context.onChange(dep, listener);
                unbindTasks.add(() -> context.removeListener(dep, listener));
            }
        }

        void unbind() {
            unbindTasks.forEach(Runnable::run);
            unbindTasks.clear();
        }

        void replace(TabElement newElement) {
            unbind();
            this.element = newElement;
            bind();
        }
    }

    private final Player player;
    private final TabPlayer tabPlayer;
    private final TabContext context;
    private final Scoreboard scoreboard;
    private final List<Binding> bindings = new ArrayList<>();
    private final Binding titleBinding;

    private TabScoreboardView(Player player, String name, TabElement title, List<TabElement> lines, TabContext context) {
        this.player = player;
        this.context = context;
        this.tabPlayer = TabEngine.tabPlayer(player);
        if (tabPlayer == null) {
            throw new IllegalStateException("Player " + player.getName() + " is not yet loaded by TAB; check TabEngine.isReady() first.");
        }

        ScoreboardManager sm = Objects.requireNonNull(TabAPI.getInstance().getScoreboardManager());
        List<String> initialText = lines.stream().map(el -> el.render(context)).toList();
        this.scoreboard = sm.createScoreboard(name, title.render(context), initialText);

        List<Line> tabLines = scoreboard.getLines();
        for (int i = 0; i < lines.size(); i++) {
            bindings.add(new Binding(lines.get(i), tabLines.get(i)));
        }
        this.titleBinding = new Binding(title, null);

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
        Binding binding = bindings.get(index);
        binding.replace(element);
        refresh(binding);
    }

    /** Swaps the element rendering the title, unbinding the old element's context listeners. */
    public void setTitle(TabElement element) {
        titleBinding.replace(element);
        refresh(titleBinding);
    }

    /** Re-renders every line and the title right now, even the ones with no changed dependency. */
    public void refreshAll() {
        bindings.forEach(this::refresh);
        refresh(titleBinding);
    }

    private void refresh(Binding binding) {
        String text = binding.element.render(context);
        if (binding.line != null) {
            binding.line.setText(text);
        } else {
            scoreboard.setTitle(text);
        }
        binding.ticksSinceRefresh = 0;
    }

    @Override
    public UUID playerId() {
        return player.getUniqueId();
    }

    @Override
    public void tick() {
        tickBinding(titleBinding);
        for (Binding binding : bindings) {
            tickBinding(binding);
        }
    }

    private void tickBinding(Binding binding) {
        int interval = binding.element.refreshTicks();
        if (interval <= 0) return;
        if (++binding.ticksSinceRefresh >= interval) {
            refresh(binding);
        }
    }

    /** Stops updating this view, switches the player to the library default scoreboard, then removes this one. */
    @Override
    public void destroy() {
        TabEngine.getInstance().unregister(this);
        titleBinding.unbind();
        bindings.forEach(Binding::unbind);
        Objects.requireNonNull(TabAPI.getInstance().getScoreboardManager()).showScoreboard(tabPlayer, TabEngine.defaultScoreboard(player));
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

        /** Creates the scoreboard and its initial content, using {@code context} for any elements that need it. Call {@link #show} to display it. */
        public TabScoreboardView build(TabContext context) {
            return new TabScoreboardView(player, name, title, List.copyOf(lines), context);
        }
    }
}
