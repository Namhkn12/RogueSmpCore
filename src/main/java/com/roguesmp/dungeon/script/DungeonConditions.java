package com.roguesmp.dungeon.script;

import com.roguesmp.codec.Codec;
import com.roguesmp.dungeon.script.expr.DungeonQueries;
import com.roguesmp.dungeon.script.expr.Expression;
import com.roguesmp.registry.Registries;
import org.bukkit.entity.Player;

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
        public boolean test(ScriptContext context) {
            return value.test(context.variable(var, perPlayer));
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
        public boolean test(ScriptContext context) {
            String type = DungeonQueries.roomType(context.feature());
            return value.test(type == null ? null : DataValue.of(type));
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
        public boolean test(ScriptContext context) {
            return value.test(DataValue.of(context.feature().instance().members().size()));
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
        public boolean test(ScriptContext context) {
            Double seconds = DungeonQueries.timeLeft(context.feature());
            return value.test(seconds == null ? null : DataValue.of(seconds));
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
            DungeonCondition.validateAll(conditions, check.at("conditions"));
        }

        @Override
        public boolean test(ScriptContext context) {
            return DungeonCondition.all(conditions, context);
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
            DungeonCondition.validateAll(conditions, check.at("conditions"));
        }

        @Override
        public boolean test(ScriptContext context) {
            return conditions.stream().anyMatch(condition -> condition.test(context));
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
        public boolean test(ScriptContext context) {
            return !condition.test(context);
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
        public boolean test(ScriptContext context) {
            return count.test(DataValue.of(DungeonQueries.mobCount(context.feature(), tag)));
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
        public boolean test(ScriptContext context) {
            return DungeonQueries.inRegion(context.feature(), context.player(), region, all);
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
        public boolean test(ScriptContext context) {
            return DungeonQueries.hasItem(context.player(), item, amount);
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
        public boolean test(ScriptContext context) {
            Player player = context.player();
            return value.test(player == null ? null : DataValue.of(DungeonQueries.playerHealth(player)));
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
        public boolean test(ScriptContext context) {
            return value.test(DataValue.of(DungeonQueries.aliveCount(context.feature())));
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
        public boolean test(ScriptContext context) {
            return ThreadLocalRandom.current().nextDouble() < chance;
        }
    }

    public record Expr(Expression expression) implements DungeonCondition {
        public static final Codec<Expr> CODEC = Codec.composite(
                Expression.CODEC.fieldOf("expr").forGetter(Expr::expression),
                Expr::new
        );

        @Override
        public String getTypeId() {
            return "expr";
        }

        @Override
        public void validate(DungeonValidation check) {
            check.reads(expression.refs());
        }

        @Override
        public boolean test(ScriptContext context) {
            return expression.test(context);
        }
    }

    public static final Codec<Expr> EXPR = register("expr", Expr.CODEC);
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
