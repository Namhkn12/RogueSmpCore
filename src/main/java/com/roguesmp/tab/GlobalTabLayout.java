package com.roguesmp.tab;

import com.roguesmp.effect.SmpEffect;
import com.roguesmp.party.Party;
import com.roguesmp.party.PartyManager;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.quest.Quest;
import com.roguesmp.quest.QuestManager;
import com.roguesmp.quest.QuestProgress;
import com.roguesmp.quest.objective.ObjectiveProgress;
import java.util.Comparator;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import java.util.UUID;
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
    private static final int MAX_PARTY_LINES = 5;
    private static final int MAX_QUEST_LINES = 11;

    public static final TabClaim CLAIM = new TabClaim("global", 0, GlobalTabLayout::scoreboardView, GlobalTabLayout::tabListView);

    public static TabListView tabListView(Player player, TabContext context) {
        List<TabElement> effectRows = new ArrayList<>();
        for (int i = 0; i < MAX_EFFECT_LINES; i++) {
            int index = i;
            effectRows.add(TabElement.ticking(20, c -> effectLine(player, index)));
        }
        effectRows.add(TabElement.ticking(20, c -> effectOverflow(player)));

        List<TabElement> partyRows = new ArrayList<>();
        for (int i = 0; i < MAX_PARTY_LINES; i++) {
            int index = i;
            partyRows.add(TabElement.ticking(20, c -> partyLine(player, index)));
        }
        partyRows.add(TabElement.ticking(20, c -> partyOverflow(player)));
        partyRows.add(TabElement.constant(""));
        partyRows.add(TabElement.constant("<bold><yellow>Nhiệm vụ</yellow></bold>"));
        QuestLineCache questLines = new QuestLineCache(player);
        for (int i = 0; i < MAX_QUEST_LINES; i++) {
            int index = i;
            partyRows.add(TabElement.ticking(20, c -> questLines.line(index)));
        }

        TabListView view = TabListView.create(player, "global_info_tab", context)
                .header(TabListDefaults.header())
                .footer(TabListDefaults.footer(player));
        TabListDefaults.column(view, 1, "<bold><yellow>Hiệu ứng            </yellow></bold>", effectRows);
        TabListDefaults.column(view, 2, "<bold><yellow>Nhóm</yellow></bold>", partyRows);
        return TabListDefaults.playerList(view);
    }

    public static TabScoreboardView scoreboardView(Player player, TabContext context) {
        return TabScoreboardView.builder(player, "global_scoreboard_tab")
                .title(ScoreboardDefaults.title())
                .line(ScoreboardDefaults.divider())
                .line(ScoreboardDefaults.regionLine())
                .lines(ScoreboardDefaults.playerInfo(player))
                .lines(ScoreboardDefaults.abilityLines(player))
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

    private static final class QuestLineCache {
        private final Player player;
        private int builtAt = -1;
        private List<String> lines = List.of();

        private QuestLineCache(Player player) {
            this.player = player;
        }

        private String line(int index) {
            int now = Bukkit.getCurrentTick();
            if (now != builtAt) {
                builtAt = now;
                lines = questLines(player);
            }
            return index < lines.size() ? lines.get(index) : "";
        }
    }

    /**
     * The pinned quest with all its unfinished objectives, then other active quests by most recent progress with
     * only their next objective, ending in an overflow line when they don't all fit.
     */
    private static List<String> questLines(Player player) {
        QuestManager questManager = QuestManager.getInstance();
        SmpPlayer smpPlayer = PlayerManager.getInstance().getSmpPlayer(player);
        QuestProgress pinned = questManager.getPinnedQuest(player);
        List<String> lines = new ArrayList<>();

        if (pinned != null) {
            lines.add("<gold>★ " + questName(pinned.getQuest()) + "</gold>");
            for (Component objective : unfinishedObjectives(pinned, smpPlayer)) {
                if (lines.size() >= MAX_QUEST_LINES - 1) break;
                lines.add("  " + Utils.toString(objective));
            }
        }

        List<QuestProgress> others = new ArrayList<>();
        for (QuestProgress questProgress : questManager.getActiveQuests(player)) {
            if (questProgress != pinned) {
                others.add(questProgress);
            }
        }
        others.sort(Comparator.comparingLong(QuestProgress::getLastProgressAt).reversed());
        for (int i = 0; i < others.size(); i++) {
            int remaining = others.size() - i;
            int room = MAX_QUEST_LINES - lines.size();
            if (room < 2 || (remaining > 1 && room < 3)) {
                lines.add("<gray>... và " + remaining + " nhiệm vụ khác</gray>");
                break;
            }
            QuestProgress progress = others.get(i);
            lines.add("<yellow>• " + questName(progress.getQuest()) + "</yellow>");
            List<Component> objectives = unfinishedObjectives(progress, smpPlayer);
            if (!objectives.isEmpty()) lines.add("  " + Utils.toString(objectives.getFirst()));
        }
        return lines;
    }

    private static List<Component> unfinishedObjectives(QuestProgress progress, SmpPlayer smpPlayer) {
        List<Component> displays = new ArrayList<>();
        progress.getQuest().getObjectives().forEach((id, objective) -> {
            ObjectiveProgress objectiveProgress = progress.getProgressMap().get(id);
            if (objectiveProgress != null && !progress.isObjectiveCompleted(id)) displays.addAll(objective.getDisplay(smpPlayer, objectiveProgress));
        });
        return displays;
    }

    private static String questName(Quest quest) {
        return quest.getName() != null ? quest.getName() : quest.getId();
    }

    private static List<String> partyMembers(Player player) {
        Party party = PartyManager.getInstance().getPartyByPlayer(player);
        return party == null ? List.of() : party.getMembers();
    }

    private static String partyLine(Player viewer, int index) {
        List<String> members = partyMembers(viewer);
        if (index >= members.size()) return "";
        Player member = Bukkit.getPlayer(UUID.fromString(members.get(index)));
        if (member == null) return "<gray>(Offline)</gray>";


        double health = member.getHealth();
        double maxHealth = member.getAttribute(Attribute.MAX_HEALTH).getValue();
        double ratio = health / maxHealth;
        String color = ratio > 0.5 ? "green" : ratio > 0.25 ? "yellow" : "red";
        Party party = PartyManager.getInstance().getPartyByPlayer(viewer);
        String name = party != null && party.isOwner(member.getUniqueId().toString()) ? "<bold>" + member.getName() + "</bold>" : member.getName();
        return "<white>" + name + "</white> <gray>-</gray> <" + color + ">" + Utils.formatDecimal(health) + "❤</" + color + ">";
    }

    private static String partyOverflow(Player viewer) {
        int extra = partyMembers(viewer).size() - MAX_PARTY_LINES;
        return extra > 0 ? "<gray>(và " + extra + " người khác...)</gray>" : "";
    }
}
