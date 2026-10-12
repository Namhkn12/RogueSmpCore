package com.roguesmp.tab;

import com.roguesmp.effect.SmpEffect;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.ability.Ability;
import com.roguesmp.player.ability.AbilityLoadout;
import com.roguesmp.player.ability.AbilityWithCharge;
import com.roguesmp.tab.element.TabElement;
import com.roguesmp.tab.layout.TabClaim;
import com.roguesmp.tab.scoreboard.ScoreboardDefaults;
import com.roguesmp.tab.scoreboard.TabScoreboardView;
import com.roguesmp.tab.tablist.TabListDefaults;
import com.roguesmp.tab.tablist.TabListView;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the server's global tablist layout: column 1 shows the viewer's active effects (same source
 * as {@link com.roguesmp.integration.PlaceholderAPIIntegration}'s {@code effect_N} placeholders) and
 * column 2 shows their ability cooldowns. The header, footer and player list come from {@link TabListDefaults}.
 */
public class GlobalTabLayout {

    private static final int MAX_EFFECT_LINES = 10;

    public static final TabClaim CLAIM = new TabClaim("global", 0, GlobalTabLayout::scoreboardView, GlobalTabLayout::tabListView);

    public static TabListView tabListView(Player player, TabContext context) {
        SmpPlayer smpPlayer = PlayerManager.getInstance().getSmpPlayer(player);
        AbilityLoadout loadout = smpPlayer == null ? null : smpPlayer.getAbilityLoadout();

        List<TabElement> effectRows = new ArrayList<>();
        for (int i = 0; i < MAX_EFFECT_LINES; i++) {
            int index = i;
            effectRows.add(TabElement.ticking(20, c -> effectLine(player, index)));
        }
        effectRows.add(TabElement.ticking(20, c -> effectOverflow(player)));

        List<TabElement> abilityRows = new ArrayList<>();
        for (int i = 0; i < AbilityLoadout.SLOT_COUNT; i++) {
            int index = i;
            abilityRows.add(TabElement.ticking(20, c -> abilityLine(loadout, index)));
        }

        TabListView view = TabListView.create(player, "global_info_tab", context)
                .header(TabListDefaults.header())
                .footer(TabListDefaults.footer(player));
        TabListDefaults.column(view, 1, "<bold><yellow>Hiệu ứng            </yellow></bold>", effectRows);
        TabListDefaults.column(view, 2, "<bold><yellow>Kỹ năng</yellow></bold>", abilityRows);
        return TabListDefaults.playerList(view);
    }

    public static TabScoreboardView scoreboardView(Player player, TabContext context) {
        return TabScoreboardView.builder(player, "global_scoreboard_tab")
                .title(ScoreboardDefaults.title())
                .line(ScoreboardDefaults.divider())
                .line(ScoreboardDefaults.regionLine())
                .lines(ScoreboardDefaults.playerInfo(player))
                .line(ScoreboardDefaults.spacer())
                .build(context);
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

    private static String abilityLine(AbilityLoadout loadout, int index) {
        if (loadout == null) return "";
        Ability[] abilities = loadout.getAbilities();
        if (index >= abilities.length || abilities[index] == null) {
            return "<gray>- Trống -</gray>";
        }
        Ability ability = abilities[index];
        String name = Utils.toString(ability.getAbilityInfo().getFormattedDisplayName());
        if (ability instanceof AbilityWithCharge charged) {
            String color = charged.getCharges() > 0 ? "green" : "red";
            String text = "<" + color + ">" + charged.getCharges() + "/" + charged.getMaxCharges() + "</" + color + ">";
            if (charged.getCharges() < charged.getMaxCharges()) {
                text += " <gray>(" + Utils.formatDecimal(ability.getCooldownTick() / 20.0) + "s)</gray>";
            }
            return name + " " + text;
        } else if (ability.isOnCooldown()) {
            return name + " <red>" + Utils.formatDecimal(ability.getCooldownTick() / 20.0) + "s</red>";
        }
        return name + " <green>Sẵn sàng</green>";
    }
}
