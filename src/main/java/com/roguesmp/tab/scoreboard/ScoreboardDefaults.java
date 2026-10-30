package com.roguesmp.tab.scoreboard;

import com.roguesmp.island.IslandManager;
import com.roguesmp.island.IslandRegionManager;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.quest.board.AdventureRank;
import com.roguesmp.quest.board.QuestBoardManager;
import com.roguesmp.region.SmpRegionFlags;
import com.roguesmp.tab.TabKeys;
import com.roguesmp.tab.element.TabElement;
import com.roguesmp.text.Glyphs;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.List;

public final class ScoreboardDefaults {

    private static final String TITLE = "<aqua>✦ RogueSMP ✦";
    private static final TabElement DIVIDER = TabElement.constant("<dark_gray>―――――――――――――――</dark_gray>");
    private static final TabElement SPACER = TabElement.constant("");

    private ScoreboardDefaults() {
    }

    public static String title() {
        return TITLE;
    }

    public static TabElement divider() {
        return DIVIDER;
    }

    public static TabElement spacer() {
        return SPACER;
    }

    public static TabElement regionLine() {
        return TabElement.of(c -> "<gold>Khu vực: <white>" + c.get(TabKeys.REGION_NAME, SmpRegionFlags.DISPLAY_NAME.getDefault()), TabKeys.REGION_NAME);
    }

    private static String adventureRankLine(Player player) {
        AdventureRank rank = QuestBoardManager.getInstance().getStatus(player.getUniqueId()).rank();
        if (rank.name().isEmpty()) return "<gold>Hạng: <gray>Chưa có";
        return "<gold>Hạng: <white>" + rank.name() + Utils.toString(rank.getDisplayIcon());
    }

    public static List<TabElement> playerInfo(Player player) {
        SmpPlayer smpPlayer = PlayerManager.getInstance().getSmpPlayer(player);
        return List.of(
                SPACER,
                TabElement.ticking(20, c -> "<gold>Class: " + (smpPlayer.getPlayerClass() == null ? "<gray>Chưa có" : smpPlayer.getPlayerClass().getDisplayName())),
                TabElement.ticking(20, c -> adventureRankLine(player)),
                TabElement.ticking(10, c -> "<gold>Xu: " + Utils.formatMoney(smpPlayer.getPlayerData().getMoney()) + Utils.toString(Glyphs.COIN_ICON.create().color(NamedTextColor.WHITE))));
    }
}
