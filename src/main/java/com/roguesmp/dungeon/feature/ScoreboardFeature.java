package com.roguesmp.dungeon.feature;

import com.roguesmp.codec.Codec;
import com.roguesmp.dungeon.DungeonDefinition;
import com.roguesmp.dungeon.DungeonFeature;
import com.roguesmp.dungeon.DungeonInstance;
import com.roguesmp.dungeon.DungeonManager;
import com.roguesmp.dungeon.DungeonEvent;
import com.roguesmp.dungeon.FeatureContext;
import com.roguesmp.dungeon.NoState;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.tab.element.TabElement;
import com.roguesmp.tab.layout.TabClaim;
import com.roguesmp.tab.scoreboard.ScoreboardDefaults;
import com.roguesmp.tab.scoreboard.TabScoreboardView;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.List;

public record ScoreboardFeature(int priority) implements DungeonFeature<NoState> {

    public static final String TYPE_KEY = "scoreboard";
    public static final String LAYOUT_KEY = "dungeon_run";

    private static final long WARNING_MILLIS = 5 * 60_000L;
    private static final int PROGRESS_LINES = 6;

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
    public void onEvent(FeatureContext<NoState> context, DungeonEvent event) {
        switch (event) {
            case DungeonEvent.PlayerEntered entered -> claim(context, entered.player());
            case DungeonEvent.PlayerLeft left -> release(left.player());
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
            FeatureContext<RoomsFeature.State> rooms = manager.feature(instance, RoomsFeature.class);
            if (rooms != null) {
                builder.line(ScoreboardDefaults.spacer());
                for (int i = 0; i < PROGRESS_LINES; i++) {
                    int index = i;
                    builder.line(TabElement.ticking(20, c -> {
                        List<String> lines = RoomsFeature.progressLines(rooms.state());
                        return index < lines.size() ? lines.get(index) : "";
                    }));
                }
            }
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
