package com.roguesmp.tab;

import com.roguesmp.utils.Utils;
import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.TabPlayer;
import me.neznamy.tab.api.event.player.PlayerLoadEvent;
import me.neznamy.tab.api.scoreboard.Scoreboard;
import me.neznamy.tab.api.scoreboard.ScoreboardManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/** Keeps track of every active {@link TabView}. Ticks each one every server tick and destroys them on plugin shutdown or player quit. */
public final class TabEngine implements Listener {

    private static TabEngine INSTANCE;

    // TAB has no way to clear a header/footer once it's set. A TabListView that used header()/footer() has
    // to put something back on destroy() instead of leaving its last text stuck; this is what it uses.
    private static final String DEFAULT_HEADER =
            "<bold><aqua>✦ <white>RogueSMP</white> ✦</aqua></bold>\n<gray>play.roguesmp.net</gray>";
    private static final String DEFAULT_FOOTER =
            "<strikethrough><dark_gray>――――――――――――――</dark_gray></strikethrough>\n<gray>Thank you for playing!</gray>";

    // Same idea for scoreboards: TabScoreboardView shows this on destroy() instead of using TAB's own
    // default scoreboard, so what a player falls back to is under our control.
    private static final String DEFAULT_SCOREBOARD_NAME = "roguesmp_tab_default_scoreboard";
    private static final String DEFAULT_SCOREBOARD_TITLE = "<bold><aqua>✦ RogueSMP ✦</aqua></bold>";
    private static final List<String> DEFAULT_SCOREBOARD_LINES = List.of(
            "<strikethrough><dark_gray>――――――――――――</dark_gray></strikethrough>",
            "",
            "<gray>play.roguesmp.net</gray>",
            "",
            "<strikethrough><dark_gray>――――――――――――</dark_gray></strikethrough>"
    );
    private static volatile Scoreboard defaultScoreboard;

    private final Plugin plugin;
    private final List<TabView> active = new CopyOnWriteArrayList<>();
    // Keyed by the view's own class, so a player can have at most one TabListView and one
    // TabScoreboardView tracked at a time — not an arbitrary-length list of either.
    private final Map<UUID, Map<Class<?>, TabView>> viewsByPlayer = new ConcurrentHashMap<>();
    private BukkitTask task;

    private TabEngine(Plugin plugin) {
        this.plugin = plugin;
    }

    /** Creates the engine and starts its tick loop. Call once, during plugin startup. */
    public static void init(Plugin plugin) {
        if (INSTANCE == null) {
            INSTANCE = new TabEngine(plugin);
            Bukkit.getPluginManager().registerEvents(INSTANCE, plugin);
            Objects.requireNonNull(TabAPI.getInstance().getEventBus()).register(PlayerLoadEvent.class, INSTANCE::onLoad);
            INSTANCE.start();
        }
    }

    /** Returns the engine. Throws if {@link #init} hasn't been called yet. */
    public static TabEngine getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("TabEngine not initialized.");
        }
        return INSTANCE;
    }

    /** Returns {@code true} once TAB has finished loading this player. Views can't be created before then. */
    public static boolean isReady(Player player) {
        return TabAPI.getInstance().getPlayer(player.getUniqueId()) != null;
    }

    /** Returns the TAB player for this Bukkit player, or {@code null} if TAB hasn't loaded them yet. */
    public static TabPlayer tabPlayer(Player player) {
        return TabAPI.getInstance().getPlayer(player.getUniqueId());
    }

    /** The header text a destroyed {@link com.roguesmp.tab.tablist.TabListView} falls back to. Same for every player for now. */
    public static String defaultHeader(Player player) {
        return DEFAULT_HEADER;
    }

    /** The footer text a destroyed {@link com.roguesmp.tab.tablist.TabListView} falls back to. Same for every player for now. */
    public static String defaultFooter(Player player) {
        return DEFAULT_FOOTER;
    }

    /** The TAB scoreboard a destroyed {@link com.roguesmp.tab.scoreboard.TabScoreboardView} switches the player to. Shared, created on first use. */
    public static Scoreboard defaultScoreboard(Player player) {
        Scoreboard board = defaultScoreboard;
        if (board != null) return board;
        synchronized (TabEngine.class) {
            if (defaultScoreboard != null) return defaultScoreboard;
            ScoreboardManager sm = Objects.requireNonNull(TabAPI.getInstance().getScoreboardManager());
            defaultScoreboard = sm.createScoreboard(DEFAULT_SCOREBOARD_NAME, DEFAULT_SCOREBOARD_TITLE, DEFAULT_SCOREBOARD_LINES);
            return defaultScoreboard;
        }
    }

    /**
     * Adds a view to the tick loop and quit cleanup. Views call this themselves when created. If the
     * player already has a view of the same concrete class registered, that one is destroyed first —
     * a player can only ever look at one tablist and one scoreboard at a time, so the new view replaces
     * it rather than piling up alongside it.
     */
    public void register(TabView view) {
        Map<Class<?>, TabView> views = viewsByPlayer.computeIfAbsent(view.playerId(), id -> new ConcurrentHashMap<>());
        TabView previous = views.get(view.getClass());
        if (previous != null && previous != view) {
            previous.destroy(); // also calls unregister(previous), so this runs before views.put below
        }
        views.put(view.getClass(), view);
        active.add(view);
    }

    /** Removes a view from the tick loop and quit cleanup. Views call this themselves in {@code destroy()}. */
    public void unregister(TabView view) {
        active.remove(view);
        viewsByPlayer.computeIfPresent(view.playerId(), (id, views) -> {
            views.remove(view.getClass(), view); // only if it's still the one currently registered for that class
            return views.isEmpty() ? null : views;
        });
    }

    private void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (TabView view : active) {
                view.tick();
            }
        }, 0L, 1L);
    }

    /** Stops the tick loop and destroys every active view. Call from the plugin's {@code onDisable()}. */
    public void shutdown() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        for (TabView view : active) {
            view.destroy();
        }
        active.clear();
        viewsByPlayer.clear();
    }

    /** Destroys every view still open for a player who just disconnected. */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Map<Class<?>, TabView> views = viewsByPlayer.remove(event.getPlayer().getUniqueId());
        if (views == null) return;
        for (TabView view : List.copyOf(views.values())) {
            active.remove(view);
            view.destroy();
        }
    }

    /**
     * Shows the server's global tablist and scoreboard ({@link GlobalInfoTab}, which sets its own
     * header/footer) as soon as TAB finishes loading a player. Uses TAB's own load event, not Bukkit's
     * join event, since TAB isn't ready to show anything yet at the point Bukkit's join event fires.
     */
    private void onLoad(PlayerLoadEvent event) {
        Player player = (Player) event.getPlayer().getPlayer();
        Utils.runLater(() -> { //Run later so that other system have time to initialize
            GlobalInfoTab.tabListView(player).show();
            GlobalInfoTab.scoreboardView(player).show();
        });
    }
}
