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
 * A per-player TAB scoreboard where each line is backed by a {@link TabElement}. Lines are edited directly
 * through TAB's {@link Line#setText}, so updating one element never touches the others.
 */
public final class TabScoreboardView implements TabView {

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

    public static Builder builder(Player player, String name) {
        return new Builder(player, name);
    }

    public void show() {
        Objects.requireNonNull(TabAPI.getInstance().getScoreboardManager()).showScoreboard(tabPlayer, scoreboard);
    }

    /** Swaps the element rendering a given line, unbinding the old element's context/event listeners. */
    public void setLine(int index, TabElement element) {
        Binding binding = bindings.get(index);
        binding.replace(element);
        refresh(binding);
    }

    public void setTitle(TabElement element) {
        titleBinding.replace(element);
        refresh(titleBinding);
    }

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

    @Override
    public void destroy() {
        TabEngine.getInstance().unregister(this);
        titleBinding.unbind();
        bindings.forEach(Binding::unbind);
        scoreboard.unregister();
    }

    public static final class Builder {
        private final Player player;
        private final String name;
        private TabElement title = TabElement.constant("");
        private final List<TabElement> lines = new ArrayList<>();

        private Builder(Player player, String name) {
            this.player = player;
            this.name = name;
        }

        public Builder title(String text) {
            return title(TabElement.constant(text));
        }

        public Builder title(TabElement element) {
            this.title = element;
            return this;
        }

        public Builder line(String text) {
            return line(TabElement.constant(text));
        }

        public Builder line(TabElement element) {
            this.lines.add(element);
            return this;
        }

        public TabScoreboardView build(TabContext context) {
            return new TabScoreboardView(player, name, title, List.copyOf(lines), context);
        }
    }
}
