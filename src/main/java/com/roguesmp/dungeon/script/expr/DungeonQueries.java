package com.roguesmp.dungeon.script.expr;

import com.roguesmp.dungeon.FeatureContext;
import com.roguesmp.dungeon.feature.ExpiryFeature;
import com.roguesmp.dungeon.feature.RoomsFeature;
import com.roguesmp.dungeon.room.RoomDefinition;
import com.roguesmp.dungeon.script.DataValue;
import com.roguesmp.region.ServerRegionManager;
import com.roguesmp.registry.Registries;
import com.roguesmp.utils.PlayerUtils;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

import static com.roguesmp.dungeon.script.expr.ExprFunction.number;
import static com.roguesmp.dungeon.script.expr.ExprFunction.string;

public final class DungeonQueries {

    static {
        register("alive_count", ExprFunction.of(0, (context, args) -> DataValue.of(aliveCount(context.feature()))));
        register("members", ExprFunction.of(0, (context, args) -> DataValue.of(context.feature().instance().members().size())));
        register("player_count", ExprFunction.of(0, (context, args) -> DataValue.of(context.feature().onlinePlayers().size())));
        register("time_left", ExprFunction.of(0, (context, args) -> {
            Double seconds = timeLeft(context.feature());
            return seconds == null ? null : DataValue.of(seconds);
        }));
        register("room_type", ExprFunction.of(0, (context, args) -> {
            String type = roomType(context.feature());
            return type == null ? null : DataValue.of(type);
        }));
        register("mob_count", ExprFunction.of(1, (context, args) -> DataValue.of(mobCount(context.feature(), string(args, 0)))));
        register("in_region", ExprFunction.of(1, 2, (context, args) -> {
            boolean all = args.size() > 1 && "all".equalsIgnoreCase(string(args, 1));
            return Expr.of(inRegion(context.feature(), context.player(), string(args, 0), all));
        }));
        register("has_item", ExprFunction.player(1, 2, (context, args) ->
                Expr.of(hasItem(context.player(), string(args, 0), args.size() > 1 ? (int) number(args, 1) : 1))));
        register("item_count", ExprFunction.player(1, 1, (context, args) -> DataValue.of(PlayerUtils.countItemsInInventory(context.player(), string(args, 0)))));
        register("player_health", ExprFunction.player(0, 0, (context, args) -> DataValue.of(playerHealth(context.player()))));
        register("player_name", ExprFunction.player(0, 0, (context, args) -> DataValue.of(context.player().getName())));
    }

    private DungeonQueries() {
    }

    public static void loadClass() {
    }

    public static long aliveCount(FeatureContext<?> context) {
        return context.onlinePlayers().stream().filter(target -> !target.isDead() && target.getGameMode() != GameMode.SPECTATOR).count();
    }

    public static @Nullable Double timeLeft(FeatureContext<?> context) {
        FeatureContext<ExpiryFeature.State> expiry = context.manager().feature(context.instance(), ExpiryFeature.class);
        return expiry == null ? null : (expiry.state().expiresAt() - System.currentTimeMillis()) / 1000.0;
    }

    public static @Nullable String roomType(FeatureContext<?> context) {
        FeatureContext<RoomsFeature.State> rooms = context.manager().feature(context.instance(), RoomsFeature.class);
        String roomId = rooms == null ? null : rooms.state().room();
        RoomDefinition room = roomId == null ? null : Registries.DUNGEON_ROOM.get(roomId);
        return room == null ? null : room.type().name().toLowerCase(Locale.ROOT);
    }

    public static int mobCount(FeatureContext<?> context, String tag) {
        World world = context.world();
        return world == null ? 0 : context.manager().trackedMobs().aliveCount(world, tag);
    }

    public static boolean inRegion(FeatureContext<?> context, @Nullable Player player, String region, boolean all) {
        if (!all) return player != null && contains(player.getLocation(), region);
        List<Player> players = context.onlinePlayers();
        return !players.isEmpty() && players.stream().allMatch(target -> contains(target.getLocation(), region));
    }

    public static boolean hasItem(@Nullable Player player, String item, int amount) {
        return player != null && PlayerUtils.countItemsInInventory(player, item) >= amount;
    }

    public static double playerHealth(Player player) {
        AttributeInstance max = player.getAttribute(Attribute.MAX_HEALTH);
        double maxHealth = max == null ? 20 : max.getValue();
        return maxHealth <= 0 ? 0 : player.getHealth() / maxHealth * 100;
    }

    private static boolean contains(Location location, String region) {
        return ServerRegionManager.getInstance().regionIdsAt(location).contains(region);
    }

    private static void register(String name, ExprFunction query) {
        Registries.DUNGEON_QUERY.register(name, query);
    }
}
