package com.roguesmp.dungeon_core.feature;

import com.roguesmp.codec.Codec;
import com.roguesmp.dungeon_core.DungeonDefinition;
import com.roguesmp.dungeon_core.DungeonFeature;
import com.roguesmp.dungeon_core.DungeonInstance;
import com.roguesmp.dungeon_core.DungeonManager;
import com.roguesmp.dungeon_core.DungeonSignal;
import com.roguesmp.dungeon_core.FeatureContext;
import com.roguesmp.dungeon_core.NoState;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.tab.element.TabElement;
import com.roguesmp.tab.layout.TabClaim;
import com.roguesmp.tab.scoreboard.ScoreboardDefaults;
import com.roguesmp.tab.scoreboard.TabScoreboardView;
import org.bukkit.entity.Player;

import java.time.Duration;

/**
 * Shows the run's scoreboard to players inside it: the dungeon's name and, with an {@link ExpiryFeature}, the time left.
 */
public record ScoreboardFeature(int priority) implements DungeonFeature<NoState> {

    public static final String TYPE_KEY = "scoreboard";
    public static final String LAYOUT_KEY = "dungeon_run";

    private static final long WARNING_MILLIS = 5 * 60_000L;

    public static final Codec<ScoreboardFeature> CODEC = Codec.composite(
            Codec.INT.optionalFieldOf("priority", 30).forGetter(ScoreboardFeature::priority),
            ScoreboardFeature::new
    );

    @Override
    public String getTypeId() {
        return TYPE_KEY;
    }

    @Override
    public NoState createState() {
        return NoState.INSTANCE;
    }

    @Override
    public Codec<NoState> stateCodec() {
        return NoState.CODEC;
    }

    @Override
    public void onSignal(FeatureContext<NoState> context, DungeonSignal signal) {
        switch (signal) {
            case DungeonSignal.PlayerEntered entered -> claim(context, entered.player());
            case DungeonSignal.PlayerLeft left -> release(left.player());
            default -> {
            }
        }
    }

    @Override
    public void onEnd(FeatureContext<NoState> context) {
        context.onlinePlayers().forEach(ScoreboardFeature::release);
    }

    private void claim(FeatureContext<NoState> context, Player player) {
        SmpPlayer smpPlayer = PlayerManager.getInstance().getSmpPlayer(player);
        if (smpPlayer == null) return;

        DungeonManager manager = context.manager();
        DungeonInstance instance = context.instance();
        DungeonDefinition definition = instance.definition();
        String name = definition != null ? definition.displayName() : instance.dungeonId();

        smpPlayer.getTabLayouts().claim(TabClaim.scoreboard(LAYOUT_KEY, priority, (viewer, tabContext) -> {
            TabScoreboardView.Builder builder = TabScoreboardView.builder(viewer, "dungeon_run_scoreboard")
                    .title(ScoreboardDefaults.title())
                    .line(ScoreboardDefaults.divider())
                    .line(ScoreboardDefaults.regionLine())
                    .line("<gold>Phó bản: <white>" + name);
            FeatureContext<ExpiryFeature.State> expiry = manager.feature(instance, ExpiryFeature.class);
            if (expiry != null) builder.line(TabElement.ticking(20, c -> timeLeftLine(expiry.state())));
            return builder
                    .lines(ScoreboardDefaults.playerInfo(viewer))
                    .line(ScoreboardDefaults.spacer())
                    .build(tabContext);
        }));
    }

    private static void release(Player player) {
        SmpPlayer smpPlayer = PlayerManager.getInstance().getSmpPlayer(player);
        if (smpPlayer != null) smpPlayer.getTabLayouts().release(LAYOUT_KEY);
    }

    private static String timeLeftLine(ExpiryFeature.State expiry) {
        long remaining = Math.max(0, expiry.expiresAt() - System.currentTimeMillis());
        String color = remaining <= WARNING_MILLIS ? "red" : "green";
        return "<gold>Còn lại: <" + color + ">" + formatRemaining(remaining);
    }

    private static String formatRemaining(long millis) {
        Duration duration = Duration.ofMillis(millis);
        long days = duration.toDays();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();

        if (days > 0) return days + "d " + hours + "h";
        if (hours > 0) return hours + "h " + minutes + "m";
        if (minutes > 0) return minutes + "m " + seconds + "s";
        return seconds + "s";
    }
}
