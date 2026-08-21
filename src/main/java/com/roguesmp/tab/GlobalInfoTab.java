package com.roguesmp.tab;

import com.roguesmp.effect.SmpEffect;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.ability.Ability;
import com.roguesmp.player.ability.AbilityLoadout;
import com.roguesmp.player.ability.AbilityType;
import com.roguesmp.tab.element.TabElement;
import com.roguesmp.tab.scoreboard.TabScoreboardView;
import com.roguesmp.tab.tablist.TabListView;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.IntStream;

/**
 * Builds the server's global tablist layout: column 1 shows the viewer's active effects (same source
 * as {@link com.roguesmp.integration.PlaceholderAPIIntegration}'s {@code effect_N} placeholders),
 * column 2 shows their ability cooldowns, and the last two columns are TAB's own online-player list.
 * The header/footer add a scrolling marquee and a bouncing bar on top of the server MSPT and the
 * viewer's ping.
 */
public class GlobalInfoTab {

    // TAB's default tablist grid is 80 slots, 4 columns x 20 rows, numbered column-first: column 1 = 1-20,
    // column 2 = 21-40, columns 3-4 = 41-80. This only holds for that default grid/direction.
    private static final int EFFECT_COLUMN_BASE = 0;
    private static final int ABILITY_COLUMN_BASE = 20;
    private static final int MAX_EFFECT_LINES = 10;
    private static final int[] PLAYER_LIST_SLOTS = IntStream.rangeClosed(41, 80).toArray();

    private static final String MARQUEE_TEXT = "  ✦ Chào mừng đến với RogueSMP - máy chủ pro vip ✦  ";
    private static final int MARQUEE_WINDOW = 40;
    private static final int BOUNCE_WIDTH = 20;

    private static final int ABILITY_REFRESH_TICKS = 2;

    public static TabListView tabListView(Player player) {
        SmpPlayer smpPlayer = PlayerManager.getInstance().getSmpPlayer(player);
        AbilityLoadout loadout = smpPlayer == null ? null : smpPlayer.getAbilityLoadout();

        TabContext context = new TabContext();
        TabListView view = TabListView.create(player, "global_info_tab", context)
                .header(TabElement.lines(
                        TabElement.constant("<bold><aqua>✦ <white>RogueSMP</white> ✦</aqua></bold>"),
                        TabElement.ticking(2, c -> marquee())))
                .footer(TabElement.lines(
                        TabElement.ticking(20, c -> msptLine()),
                        TabElement.ticking(20, c -> pingLine(player)),
                        TabElement.ticking(2, c -> bounce())))
                .slot(EFFECT_COLUMN_BASE + 1, TabElement.constant("<bold><yellow>Hiệu ứng            </yellow></bold>"))
                .slot(ABILITY_COLUMN_BASE + 1, TabElement.constant("<bold><yellow>Kỹ năng</yellow></bold>"));

        for (int i = 0; i < MAX_EFFECT_LINES; i++) {
            int index = i;
            view.slot(EFFECT_COLUMN_BASE + 2 + i, TabElement.ticking(20, c -> effectLine(player, index)));
        }
        view.slot(EFFECT_COLUMN_BASE + 2 + MAX_EFFECT_LINES, TabElement.ticking(20, c -> effectOverflow(player)));

        int row = 2;
        for (int i = 0; i < AbilityType.ACTIVE.getMaxSlots(); i++) {
            int index = i;
            view.slot(ABILITY_COLUMN_BASE + row++, TabElement.ticking(ABILITY_REFRESH_TICKS, c -> abilityLine(loadout, AbilityType.ACTIVE, index)));
        }
        for (int i = 0; i < AbilityType.LIFELINE.getMaxSlots(); i++) {
            int index = i;
            view.slot(ABILITY_COLUMN_BASE + row++, TabElement.ticking(ABILITY_REFRESH_TICKS, c -> abilityLine(loadout, AbilityType.LIFELINE, index)));
        }

        view.group(null, PLAYER_LIST_SLOTS);

        return view;
    }

    public static TabScoreboardView scoreboardView(Player player) {
        SmpPlayer smpPlayer = PlayerManager.getInstance().getSmpPlayer(player);
        TabContext context = new TabContext();

        TabScoreboardView scoreboardView = TabScoreboardView.builder(player, "global_scoreboard_tab")
                .title("<aqua>✦ RogueSMP ✦")
                .line("<dark_gray>――――――――――――</dark_gray>")
                .line("")
                .line(TabElement.ticking(10, tabContext -> "<gold>Xu: " + Utils.formatMoney(smpPlayer.getPlayerData().getMoney())))
                .line("")
                .build(context);

        return scoreboardView;
    }

    /** Scrolls MARQUEE_TEXT through a fixed-width window; frame advances independently of how often it's sampled. */
    private static String marquee() {
        long frame = System.currentTimeMillis() / 100;
        int len = MARQUEE_TEXT.length();
        int offset = (int) (frame % len);
        String doubled = MARQUEE_TEXT + MARQUEE_TEXT;
        return "<aqua>" + doubled.substring(offset, offset + MARQUEE_WINDOW) + "</aqua>";
    }

    /** A dot bouncing back and forth between the ends of a fixed-width bar. */
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

    private static String effectLine(Player player, int index) {
        List<Component> effects = SmpEffect.getSortedEffectDisplays(player);
        return index < effects.size() ? Utils.toString(effects.get(index)) : "";
    }

    /** Mirrors PlaceholderAPIIntegration's {@code effect_more}: the 11th effect if there's exactly one extra, otherwise a "N more" line. */
    private static String effectOverflow(Player player) {
        List<Component> effects = SmpEffect.getSortedEffectDisplays(player);
        int extra = effects.size() - MAX_EFFECT_LINES;
        if (extra == 1) {
            return Utils.toString(effects.get(MAX_EFFECT_LINES));
        } else if (extra > 0) {
            return Utils.toString(Component.text("... và " + extra + " hiệu ứng khác", NamedTextColor.GRAY));
        }
        return "";
    }

    private static String abilityLine(AbilityLoadout loadout, AbilityType type, int index) {
        if (loadout == null) return "";
        Ability[] abilities = loadout.getAbilities(type);
        if (index >= abilities.length || abilities[index] == null) {
            return "<gray>- Trống -</gray>";
        }
        Ability ability = abilities[index];
        String name = Utils.toString(ability.getAbilityInfo().getFormattedDisplayName());
        if (ability.isOnCooldown()) {
            return name + " <red>" + Utils.formatDecimal(ability.getCooldownTick() / 20.0) + "s</red>";
        }
        return name + " <green>Sẵn sàng</green>";
    }
}
