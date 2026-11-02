package com.roguesmp.dungeon.script;

import com.roguesmp.codec.Codec;
import com.roguesmp.dungeon.FeatureContext;
import com.roguesmp.dungeon.feature.ExpiryFeature;
import com.roguesmp.dungeon.feature.RoomsFeature;
import com.roguesmp.dungeon.room.RoomDefinition;
import com.roguesmp.registry.Registries;

import com.roguesmp.region.ServerRegionManager;
import com.roguesmp.utils.PlayerUtils;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class DungeonConditions {

    public record Var(String var, ValuePredicate value, boolean perPlayer) implements DungeonCondition {
        public static final Codec<Var> CODEC = Codec.composite(
                Codec.STRING.fieldOf("var").forGetter(Var::var),
                ValuePredicate.CODEC.fieldOf("value").forGetter(Var::value),
                Codec.BOOLEAN.optionalFieldOf("player", false).forGetter(Var::perPlayer),
                Var::new
        );

        @Override
        public String getTypeId() {
            return "var";
        }

        @Override
        public void validate(DungeonValidation check) {
            check.readsVar(var);
        }

        @Override
        public boolean test(FeatureContext<?> context, @Nullable Player player) {
            return value.test(context.instance().variable(FeatureContext.varName(var, perPlayer, player)));
        }
    }

    public record RoomType(ValuePredicate value) implements DungeonCondition {
        public static final Codec<RoomType> CODEC = Codec.composite(
                ValuePredicate.CODEC.fieldOf("value").forGetter(RoomType::value),
                RoomType::new
        );

        @Override
        public String getTypeId() {
            return "room_type";
        }

        @Override
        public boolean test(FeatureContext<?> context, @Nullable Player player) {
            FeatureContext<RoomsFeature.State> rooms = context.manager().feature(context.instance(), RoomsFeature.class);
            String roomId = rooms == null ? null : rooms.state().room();
            RoomDefinition room = roomId == null ? null : Registries.DUNGEON_ROOM.get(roomId);
            return value.test(room == null ? null : DataValue.of(room.type().name().toLowerCase()));
        }
    }

    public record Members(ValuePredicate value) implements DungeonCondition {
        public static final Codec<Members> CODEC = Codec.composite(
                ValuePredicate.CODEC.fieldOf("value").forGetter(Members::value),
                Members::new
        );

        @Override
        public String getTypeId() {
            return "members";
        }

        @Override
        public boolean test(FeatureContext<?> context, @Nullable Player player) {
            return value.test(DataValue.of(context.instance().members().size()));
        }
    }

    // Seconds until the expiry feature ends the instance; no value when the dungeon has no expiry.
    public record TimeLeft(ValuePredicate value) implements DungeonCondition {
        public static final Codec<TimeLeft> CODEC = Codec.composite(
                ValuePredicate.CODEC.fieldOf("value").forGetter(TimeLeft::value),
                TimeLeft::new
        );

        @Override
        public String getTypeId() {
            return "time_left";
        }

        @Override
        public boolean test(FeatureContext<?> context, @Nullable Player player) {
            FeatureContext<ExpiryFeature.State> expiry = context.manager().feature(context.instance(), ExpiryFeature.class);
            if (expiry == null) return value.test(null);
            return value.test(DataValue.of((expiry.state().expiresAt() - System.currentTimeMillis()) / 1000.0));
        }
    }

    public record AllOf(List<DungeonCondition> conditions) implements DungeonCondition {
        public static final Codec<AllOf> CODEC = Codec.composite(
                Codec.listOf(DungeonCondition.CODEC).fieldOf("conditions").forGetter(AllOf::conditions),
                AllOf::new
        );

        @Override
        public String getTypeId() {
            return "all_of";
        }

        @Override
        public void validate(DungeonValidation check) {
            for (int i = 0; i < conditions.size(); i++) conditions.get(i).validate(check.at("conditions[" + i + "]"));
        }

        @Override
        public boolean test(FeatureContext<?> context, @Nullable Player player) {
            return DungeonCondition.all(conditions, context, player);
        }
    }

    public record AnyOf(List<DungeonCondition> conditions) implements DungeonCondition {
        public static final Codec<AnyOf> CODEC = Codec.composite(
                Codec.listOf(DungeonCondition.CODEC).fieldOf("conditions").forGetter(AnyOf::conditions),
                AnyOf::new
        );

        @Override
        public String getTypeId() {
            return "any_of";
        }

        @Override
        public void validate(DungeonValidation check) {
            for (int i = 0; i < conditions.size(); i++) conditions.get(i).validate(check.at("conditions[" + i + "]"));
        }

        @Override
        public boolean test(FeatureContext<?> context, @Nullable Player player) {
            return conditions.stream().anyMatch(condition -> condition.test(context, player));
        }
    }

    public record Not(DungeonCondition condition) implements DungeonCondition {
        public static final Codec<Not> CODEC = Codec.composite(
                DungeonCondition.CODEC.fieldOf("condition").forGetter(Not::condition),
                Not::new
        );

        @Override
        public String getTypeId() {
            return "not";
        }

        @Override
        public void validate(DungeonValidation check) {
            condition.validate(check.at("condition"));
        }

        @Override
        public boolean test(FeatureContext<?> context, @Nullable Player player) {
            return !condition.test(context, player);
        }
    }

    public record MobCount(String tag, ValuePredicate count) implements DungeonCondition {
        public static final Codec<MobCount> CODEC = Codec.composite(
                Codec.STRING.fieldOf("tag").forGetter(MobCount::tag),
                ValuePredicate.CODEC.fieldOf("count").forGetter(MobCount::count),
                MobCount::new
        );

        @Override
        public String getTypeId() {
            return "mob_count";
        }

        @Override
        public boolean test(FeatureContext<?> context, @Nullable Player player) {
            World world = context.world();
            return count.test(DataValue.of(world == null ? 0 : context.manager().trackedMobs().aliveCount(world, tag)));
        }
    }

    public record InRegion(String region, boolean all) implements DungeonCondition {
        public static final Codec<InRegion> CODEC = Codec.composite(
                Codec.STRING.fieldOf("region").forGetter(InRegion::region),
                Codec.BOOLEAN.optionalFieldOf("all", false).forGetter(InRegion::all),
                InRegion::new
        );

        @Override
        public String getTypeId() {
            return "in_region";
        }

        @Override
        public boolean test(FeatureContext<?> context, @Nullable Player player) {
            if (!all) return player != null && contains(player.getLocation());
            List<Player> players = context.onlinePlayers();
            return !players.isEmpty() && players.stream().allMatch(target -> contains(target.getLocation()));
        }

        private boolean contains(Location location) {
            return ServerRegionManager.getInstance().regionIdsAt(location).contains(region);
        }
    }

    public record HasItem(String item, int amount) implements DungeonCondition {
        public static final Codec<HasItem> CODEC = Codec.composite(
                Codec.STRING.fieldOf("item").forGetter(HasItem::item),
                Codec.INT.optionalFieldOf("amount", 1).forGetter(HasItem::amount),
                HasItem::new
        );

        @Override
        public String getTypeId() {
            return "has_item";
        }

        @Override
        public void validate(DungeonValidation check) {
            check.checkItem(item);
        }

        @Override
        public boolean test(FeatureContext<?> context, @Nullable Player player) {
            return player != null && PlayerUtils.countItemsInInventory(player, item) >= amount;
        }
    }

    // Health as a percentage (0-100) of the player's max health.
    public record PlayerHealth(ValuePredicate value) implements DungeonCondition {
        public static final Codec<PlayerHealth> CODEC = Codec.composite(
                ValuePredicate.CODEC.fieldOf("value").forGetter(PlayerHealth::value),
                PlayerHealth::new
        );

        @Override
        public String getTypeId() {
            return "player_health";
        }

        @Override
        public boolean test(FeatureContext<?> context, @Nullable Player player) {
            if (player == null) return value.test(null);
            AttributeInstance max = player.getAttribute(Attribute.MAX_HEALTH);
            double maxHealth = max == null ? 20 : max.getValue();
            return value.test(DataValue.of(maxHealth <= 0 ? 0 : player.getHealth() / maxHealth * 100));
        }
    }

    public record AliveCount(ValuePredicate value) implements DungeonCondition {
        public static final Codec<AliveCount> CODEC = Codec.composite(
                ValuePredicate.CODEC.fieldOf("value").forGetter(AliveCount::value),
                AliveCount::new
        );

        @Override
        public String getTypeId() {
            return "alive_count";
        }

        @Override
        public boolean test(FeatureContext<?> context, @Nullable Player player) {
            long alive = context.onlinePlayers().stream().filter(target -> !target.isDead() && target.getGameMode() != GameMode.SPECTATOR).count();
            return value.test(DataValue.of(alive));
        }
    }

    public record Chance(double chance) implements DungeonCondition {
        public static final Codec<Chance> CODEC = Codec.composite(
                Codec.DOUBLE.fieldOf("chance").forGetter(Chance::chance),
                Chance::new
        );

        @Override
        public String getTypeId() {
            return "random";
        }

        @Override
        public void validate(DungeonValidation check) {
            if (chance < 0 || chance > 1) check.warn("'chance' must be between 0 and 1");
        }

        @Override
        public boolean test(FeatureContext<?> context, @Nullable Player player) {
            return ThreadLocalRandom.current().nextDouble() < chance;
        }
    }

    public static final Codec<MobCount> MOB_COUNT = register("mob_count", MobCount.CODEC);
    public static final Codec<Var> VAR = register("var", Var.CODEC);
    public static final Codec<RoomType> ROOM_TYPE = register("room_type", RoomType.CODEC);
    public static final Codec<Members> MEMBERS = register("members", Members.CODEC);
    public static final Codec<TimeLeft> TIME_LEFT = register("time_left", TimeLeft.CODEC);
    public static final Codec<AllOf> ALL_OF = register("all_of", AllOf.CODEC);
    public static final Codec<AnyOf> ANY_OF = register("any_of", AnyOf.CODEC);
    public static final Codec<Not> NOT = register("not", Not.CODEC);
    public static final Codec<InRegion> IN_REGION = register("in_region", InRegion.CODEC);
    public static final Codec<HasItem> HAS_ITEM = register("has_item", HasItem.CODEC);
    public static final Codec<PlayerHealth> PLAYER_HEALTH = register("player_health", PlayerHealth.CODEC);
    public static final Codec<AliveCount> ALIVE_COUNT = register("alive_count", AliveCount.CODEC);
    public static final Codec<Chance> RANDOM = register("random", Chance.CODEC);

    public static void loadClass() {

    }

    private static <T extends DungeonCondition> Codec<T> register(String typeName, Codec<T> codec) {
        return Registries.DUNGEON_CONDITION_CODEC.register(typeName, codec);
    }
}
