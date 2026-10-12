package com.roguesmp.tab.tablist;

import com.roguesmp.tab.element.TabElement;
import com.roguesmp.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.IntStream;

/**
 * Building blocks shared by tablist layouts. Assumes TAB's default grid: 4 columns x 20 rows, numbered
 * column-first, so column 1 = slots 1-20, column 2 = 21-40, columns 3-4 = 41-80.
 */
public final class TabListDefaults {

    private static final int ROWS_PER_COLUMN = 20;
    private static final int LAST_LEFT_COLUMN = 2;
    private static final int[] PLAYER_LIST_SLOTS = IntStream.rangeClosed(41, 80).toArray();

    private static final String MARQUEE_TEXT = "  ✦ Chào mừng đến với RogueSMP - máy chủ pro vip ✦  ";
    private static final int MARQUEE_WINDOW = 40;
    private static final int BOUNCE_WIDTH = 20;

    private TabListDefaults() {
    }

    public static TabElement header() {
        return TabElement.lines(
                TabElement.constant("<bold><aqua>✦ <white>RogueSMP</white> ✦</aqua></bold>"),
                TabElement.ticking(2, c -> marquee()));
    }

    public static TabElement footer(Player player) {
        return TabElement.lines(
                TabElement.ticking(20, c -> msptLine()),
                TabElement.ticking(20, c -> pingLine(player)),
                TabElement.ticking(2, c -> bounce()));
    }

    public static TabListView column(TabListView view, int column, String title, List<TabElement> rows) {
        if (column < 1 || column > LAST_LEFT_COLUMN) throw new IllegalArgumentException("Only columns 1-" + LAST_LEFT_COLUMN + " are free for layouts");
        if (rows.size() > ROWS_PER_COLUMN - 1) throw new IllegalArgumentException("A column holds at most " + (ROWS_PER_COLUMN - 1) + " rows below its title");

        int base = (column - 1) * ROWS_PER_COLUMN;
        view.slot(base + 1, TabElement.constant(title));
        for (int i = 0; i < rows.size(); i++) {
            view.slot(base + 2 + i, rows.get(i));
        }
        return view;
    }

    public static TabListView playerList(TabListView view) {
        return view.group(null, PLAYER_LIST_SLOTS);
    }

    private static String marquee() {
        long frame = System.currentTimeMillis() / 100;
        int len = MARQUEE_TEXT.length();
        int offset = (int) (frame % len);
        String doubled = MARQUEE_TEXT + MARQUEE_TEXT;
        return "<aqua>" + doubled.substring(offset, offset + MARQUEE_WINDOW) + "</aqua>";
    }

    private static String bounce() {
        long frame = System.currentTimeMillis() / 100;
        int period = (BOUNCE_WIDTH - 1) * 2;
        int pos = (int) (frame % period);
        int index = pos < BOUNCE_WIDTH ? pos : period - pos;
        StringBuilder bar = new StringBuilder("<dark_gray>[");
        for (int i = 0; i < BOUNCE_WIDTH; i++) {
            bar.append(i == index ? "<yellow>●</yellow>" : "<dark_gray>·</dark_gray>");
        }
        return bar.append("<dark_gray>]</dark_gray>").toString();
    }

    private static String msptLine() {
        double mspt = Bukkit.getServer().getAverageTickTime();
        String color = mspt <= 40 ? "green" : mspt <= 50 ? "yellow" : "red";
        return "<gray>MSPT: <" + color + ">" + Utils.formatDecimal(mspt) + "ms</" + color + "></gray>";
    }

    private static String pingLine(Player player) {
        int ping = player.getPing();
        String color = ping <= 100 ? "green" : ping <= 200 ? "yellow" : "red";
        return "<gray>Ping: <" + color + ">" + ping + "ms</" + color + "></gray>";
    }
}
