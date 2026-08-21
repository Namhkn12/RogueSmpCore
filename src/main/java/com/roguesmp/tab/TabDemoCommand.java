package com.roguesmp.tab;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.tab.element.TabElement;
import com.roguesmp.tab.scoreboard.TabScoreboardView;
import com.roguesmp.tab.tablist.TabListView;
import dev.jorel.commandapi.CommandAPICommand;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/** {@code /tabdemo} — a scoreboard and tablist sharing one {@link TabContext} to show elements updating independently as it changes. */
public final class TabDemoCommand {

    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();

    private static final String MARQUEE_TEXT = "  ★ Welcome to RogueSMP — powered by the TAB rendering engine ★  ";
    private static final int MARQUEE_WINDOW = 42;
    private static final int BOUNCE_WIDTH = 20;

    /** Scrolls MARQUEE_TEXT through a fixed-width window; frame advances independently of how often it's sampled. */
    private static String marquee() {
        long frame = System.currentTimeMillis() / 100;
        int len = MARQUEE_TEXT.length();
        int offset = (int) (frame % len);
        String doubled = MARQUEE_TEXT + MARQUEE_TEXT;
        return "&b" + doubled.substring(offset, offset + MARQUEE_WINDOW);
    }

    /** A dot bouncing back and forth between the ends of a fixed-width bar. */
    private static String bounce() {
        long frame = System.currentTimeMillis() / 100;
        int period = (BOUNCE_WIDTH - 1) * 2;
        int pos = (int) (frame % period);
        int index = pos < BOUNCE_WIDTH ? pos : period - pos;
        StringBuilder bar = new StringBuilder("&7[");
        for (int i = 0; i < BOUNCE_WIDTH; i++) {
            bar.append(i == index ? "&e●" : "&8·");
        }
        return bar.append("&7]").toString();
    }

    // TAB's default tablist grid is 80 slots, 4 columns x 20 rows, numbered column-first (column 1 = 1-20,
    // column 2 = 21-40, column 3 = 41-60, column 4 = 61-80). So the last two columns are 41-60 and 61-80;
    // row 1 of each (41, 61) is the label, rows 2-20 (42-60, 62-80) are reserved for TAB's own native player
    // group (real name/skin/ping, auto-populated, overflow handled by TAB itself — no per-player code needed).
    // This only holds for that default grid/direction; a reconfigured TAB layout would shift it.
    private static final int[] PLAYER_LIST_LABEL_SLOTS = {41, 61};
    private static final int[] PLAYER_LIST_SLOTS = buildPlayerListSlots();

    private static int[] buildPlayerListSlots() {
        int[] slots = new int[2 * 19];
        int i = 0;
        for (int row = 2; row <= 20; row++) {
            slots[i++] = 40 + row; // column 3
            slots[i++] = 60 + row; // column 4
        }
        return slots;
    }

    private record Session(TabScoreboardView board, TabListView tablist, BukkitTask ticker) {
    }

    private TabDemoCommand() {
    }

    public static void register() {
        new CommandAPICommand("tabdemo")
                .executesPlayer((player, args) -> {
                    toggle(player);
                })
                .register();

        Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onQuit(PlayerQuitEvent event) {
                // Views are torn down by TabEngine itself on quit; this only needs to stop the demo's own ticker.
                Session session = SESSIONS.remove(event.getPlayer().getUniqueId());
                if (session != null) {
                    session.ticker().cancel();
                }
            }
        }, RogueSmpCore.getInstance());
    }

    private static void toggle(Player player) {
        Session existing = SESSIONS.remove(player.getUniqueId());
        if (existing != null) {
            existing.ticker().cancel();
            existing.board().destroy();
            existing.tablist().destroy();
            player.sendMessage(Component.text("Tab demo stopped.", NamedTextColor.GRAY));
            return;
        }

        if (!TabEngine.isReady(player)) {
            player.sendMessage(Component.text("TAB hasn't loaded you in yet, try again in a moment.", NamedTextColor.RED));
            return;
        }

        SESSIONS.put(player.getUniqueId(), start(player));
        player.sendMessage(Component.text("Tab demo started. Run again to stop.", NamedTextColor.GREEN));
    }

    private static Session start(Player player) {
        TabContext context = new TabContext();
        context.set("coins", 0);
        context.set("combo", 0);

        TabScoreboardView board = TabScoreboardView.builder(player, "tabdemo_" + player.getUniqueId())
                .title(TabElement.ticking(20, c -> "&b&lRogueSMP &7" + LocalTime.now().format(CLOCK)))
                .line(TabElement.constant(""))
                .line(TabElement.of(c -> "&fCoins: &e" + c.get("coins", 0), "coins"))
                .line(TabElement.of(c -> "&fCombo: &c" + c.get("combo", 0) + "x", "combo"))
                .line(TabElement.constant(""))
                .line(TabElement.constant("&7play.roguesmp.net"))
                .build(context);
        board.show();

        TabListView tablist = TabListView.create(player, "tabdemo", context)
                .header(TabElement.lines(
                        TabElement.constant("&b&lRogueSMP &7- Tab Demo"),
                        TabElement.ticking(2, c -> marquee())))
                .footer(TabElement.lines(
                        TabElement.of(c -> "&7Coins: &e" + c.get("coins", 0), "coins"),
                        TabElement.ticking(2, c -> bounce())))
                .slot(1, TabElement.constant("&c&lWelcome"))
                .slot(2, TabElement.of(c -> "&fCombo: &c" + c.get("combo", 0) + "x", "combo"))
                .slot(3, TabElement.ticking(20, c -> "&7Clock: &f" + LocalTime.now().format(CLOCK)))
                .group(null, PLAYER_LIST_SLOTS);

        for (int labelSlot : PLAYER_LIST_LABEL_SLOTS) {
            tablist.slot(labelSlot, TabElement.constant("&a&lNgười chơi:"));
        }

        tablist.show();

        BukkitTask ticker = new BukkitRunnable() {
            private int ticks;
            private boolean bonusSlotAdded;

            @Override
            public void run() {
                ticks++;
                context.set("coins", context.get("coins", 0) + ThreadLocalRandom.current().nextInt(1, 10));
                context.set("combo", (context.get("combo", 0) + 1) % 10);

                // A slot added after show() is a structural change (one full layout resend), unlike the
                // per-value pushes above — this proves that path doesn't disturb the slots already showing.
                if (!bonusSlotAdded && ticks >= 5) {
                    bonusSlotAdded = true;
                    tablist.slot(4, TabElement.constant("&6&lBonus slot unlocked!"));
                }
            }
        }.runTaskTimer(RogueSmpCore.getInstance(), 20L, 20L);

        return new Session(board, tablist, ticker);
    }
}
