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
                .header(TabElement.constant("&b&lRogueSMP &7- Tab Demo"))
                .footer(TabElement.of(c -> "&7Coins: &e" + c.get("coins", 0), "coins"))
                .slot(1, TabElement.constant("&c&lWelcome"))
                .slot(2, TabElement.of(c -> "&fCombo: &c" + c.get("combo", 0) + "x", "combo"));
        tablist.show();

        BukkitTask ticker = new BukkitRunnable() {
            @Override
            public void run() {
                context.set("coins", context.get("coins", 0) + ThreadLocalRandom.current().nextInt(1, 10));
                context.set("combo", (context.get("combo", 0) + 1) % 10);
            }
        }.runTaskTimer(RogueSmpCore.getInstance(), 20L, 20L);

        return new Session(board, tablist, ticker);
    }
}
