package com.roguesmp.dungeon.script;

import com.roguesmp.codec.Codec;
import com.roguesmp.dungeon.FeatureContext;
import com.roguesmp.dungeon.feature.ExpiryFeature;
import com.roguesmp.dungeon.feature.RoomsFeature;
import com.roguesmp.dungeon.room.RoomDefinition;
import com.roguesmp.registry.Registries;

import java.util.List;
import org.bukkit.World;

public class DungeonConditions {

    public record Var(String var, ValuePredicate value) implements DungeonCondition {
        public static final Codec<Var> CODEC = Codec.composite(
                Codec.STRING.fieldOf("var").forGetter(Var::var),
                ValuePredicate.CODEC.fieldOf("value").forGetter(Var::value),
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
        public boolean test(FeatureContext<?> context) {
            return value.test(context.instance().variable(var));
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
        public boolean test(FeatureContext<?> context) {
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
        public boolean test(FeatureContext<?> context) {
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
        public boolean test(FeatureContext<?> context) {
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
        public boolean test(FeatureContext<?> context) {
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
            for (int i = 0; i < conditions.size(); i++) conditions.get(i).validate(check.at("conditions[" + i + "]"));
        }

        @Override
        public boolean test(FeatureContext<?> context) {
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
        public boolean test(FeatureContext<?> context) {
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
        public boolean test(FeatureContext<?> context) {
            World world = context.world();
            return count.test(DataValue.of(world == null ? 0 : context.manager().trackedMobs().aliveCount(world, tag)));
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

    public static void loadClass() {

    }

    private static <T extends DungeonCondition> Codec<T> register(String typeName, Codec<T> codec) {
        return Registries.DUNGEON_CONDITION_CODEC.register(typeName, codec);
    }
}
