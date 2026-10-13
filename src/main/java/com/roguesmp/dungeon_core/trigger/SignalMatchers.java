package com.roguesmp.dungeon_core.trigger;

import com.roguesmp.codec.Codec;
import com.roguesmp.dungeon_core.DungeonSignal;
import com.roguesmp.dungeon_core.FeatureContext;
import com.roguesmp.registry.Registries;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Every {@link SignalMatcher} type, one per {@link DungeonSignal}. An empty id filter matches any value, and
 * {@code owner_only} limits a player signal to the run's owner. Each constant registers itself into
 * {@link Registries#DUNGEON_SIGNAL_MATCHER_CODEC} as it's initialized.
 */
public class SignalMatchers {

    public record Started() implements SignalMatcher {
        public static final Codec<Started> CODEC = Codec.unit(Started::new);

        @Override
        public String getTypeId() {
            return "started";
        }

        @Override
        public boolean matches(FeatureContext<?> context, DungeonSignal signal) {
            return signal instanceof DungeonSignal.Started;
        }
    }

    public record PlayerEntered(boolean ownerOnly) implements SignalMatcher {
        public static final Codec<PlayerEntered> CODEC = Codec.composite(
                Codec.BOOLEAN.optionalFieldOf("owner_only", false).forGetter(PlayerEntered::ownerOnly),
                PlayerEntered::new
        );

        @Override
        public String getTypeId() {
            return "player_entered";
        }

        @Override
        public boolean matches(FeatureContext<?> context, DungeonSignal signal) {
            return signal instanceof DungeonSignal.PlayerEntered(Player player) && allowed(context, player, ownerOnly);
        }
    }

    public record PlayerLeft(boolean ownerOnly) implements SignalMatcher {
        public static final Codec<PlayerLeft> CODEC = Codec.composite(
                Codec.BOOLEAN.optionalFieldOf("owner_only", false).forGetter(PlayerLeft::ownerOnly),
                PlayerLeft::new
        );

        @Override
        public String getTypeId() {
            return "player_left";
        }

        @Override
        public boolean matches(FeatureContext<?> context, DungeonSignal signal) {
            return signal instanceof DungeonSignal.PlayerLeft(Player player) && allowed(context, player, ownerOnly);
        }
    }

    public record RegionEntered(String region, boolean ownerOnly) implements SignalMatcher {
        public static final Codec<RegionEntered> CODEC = Codec.composite(
                Codec.STRING.optionalFieldOf("region", "").forGetter(RegionEntered::region),
                Codec.BOOLEAN.optionalFieldOf("owner_only", false).forGetter(RegionEntered::ownerOnly),
                RegionEntered::new
        );

        @Override
        public String getTypeId() {
            return "region_entered";
        }

        @Override
        public boolean matches(FeatureContext<?> context, DungeonSignal signal) {
            return signal instanceof DungeonSignal.RegionEntered(
                    Player player, String region1
            ) && matchesId(region, region1) && allowed(context, player, ownerOnly);
        }
    }

    public record RegionLeft(String region, boolean ownerOnly) implements SignalMatcher {
        public static final Codec<RegionLeft> CODEC = Codec.composite(
                Codec.STRING.optionalFieldOf("region", "").forGetter(RegionLeft::region),
                Codec.BOOLEAN.optionalFieldOf("owner_only", false).forGetter(RegionLeft::ownerOnly),
                RegionLeft::new
        );

        @Override
        public String getTypeId() {
            return "region_left";
        }

        @Override
        public boolean matches(FeatureContext<?> context, DungeonSignal signal) {
            return signal instanceof DungeonSignal.RegionLeft(
                    Player player, String region1
            ) && matchesId(region, region1) && allowed(context, player, ownerOnly);
        }
    }

    public record MobKilled(String mob, boolean ownerOnly) implements SignalMatcher {
        public static final Codec<MobKilled> CODEC = Codec.composite(
                Codec.STRING.optionalFieldOf("mob", "").forGetter(MobKilled::mob),
                Codec.BOOLEAN.optionalFieldOf("owner_only", false).forGetter(MobKilled::ownerOnly),
                MobKilled::new
        );

        @Override
        public String getTypeId() {
            return "mob_killed";
        }

        @Override
        public boolean matches(FeatureContext<?> context, DungeonSignal signal) {
            return signal instanceof DungeonSignal.MobKilled killed && matchesId(mob, killed.mobId()) && allowed(context, killed.killer(), ownerOnly);
        }
    }

    public record PlayerDied(boolean ownerOnly) implements SignalMatcher {
        public static final Codec<PlayerDied> CODEC = Codec.composite(
                Codec.BOOLEAN.optionalFieldOf("owner_only", false).forGetter(PlayerDied::ownerOnly),
                PlayerDied::new
        );

        @Override
        public String getTypeId() {
            return "player_died";
        }

        @Override
        public boolean matches(FeatureContext<?> context, DungeonSignal signal) {
            return signal instanceof DungeonSignal.PlayerDied(Player player) && allowed(context, player, ownerOnly);
        }
    }

    public record QuestCompleted(String quest) implements SignalMatcher {
        public static final Codec<QuestCompleted> CODEC = Codec.composite(
                Codec.STRING.optionalFieldOf("quest", "").forGetter(QuestCompleted::quest),
                QuestCompleted::new
        );

        @Override
        public String getTypeId() {
            return "quest_completed";
        }

        @Override
        public boolean matches(FeatureContext<?> context, DungeonSignal signal) {
            return signal instanceof DungeonSignal.QuestCompleted(
                    com.roguesmp.quest.Quest quest1
            ) && matchesId(quest, quest1.getId());
        }
    }

    /**
     * @param data entries the custom signal must carry with these exact values
     */
    public record Custom(String id, Map<String, String> data) implements SignalMatcher {
        public static final Codec<Custom> CODEC = Codec.composite(
                Codec.STRING.fieldOf("id").forGetter(Custom::id),
                Codec.unboundedMap(Codec.STRING).optionalFieldOf("data", Map.of()).forGetter(Custom::data),
                Custom::new
        );

        @Override
        public String getTypeId() {
            return "signal";
        }

        @Override
        public boolean matches(FeatureContext<?> context, DungeonSignal signal) {
            if (!(signal instanceof DungeonSignal.Custom custom) || !custom.id().equals(id)) return false;
            return data.entrySet().stream().allMatch(entry -> entry.getValue().equals(custom.data().get(entry.getKey())));
        }
    }

    public static final Codec<Started> STARTED = register("started", Started.CODEC);
    public static final Codec<PlayerEntered> PLAYER_ENTERED = register("player_entered", PlayerEntered.CODEC);
    public static final Codec<PlayerLeft> PLAYER_LEFT = register("player_left", PlayerLeft.CODEC);
    public static final Codec<RegionEntered> REGION_ENTERED = register("region_entered", RegionEntered.CODEC);
    public static final Codec<RegionLeft> REGION_LEFT = register("region_left", RegionLeft.CODEC);
    public static final Codec<MobKilled> MOB_KILLED = register("mob_killed", MobKilled.CODEC);
    public static final Codec<PlayerDied> PLAYER_DIED = register("player_died", PlayerDied.CODEC);
    public static final Codec<QuestCompleted> QUEST_COMPLETED = register("quest_completed", QuestCompleted.CODEC);
    public static final Codec<Custom> CUSTOM = register("signal", Custom.CODEC);

    public static void loadClass() {

    }

    private static boolean matchesId(String filter, String value) {
        return filter.isEmpty() || filter.equals(value);
    }

    private static boolean allowed(FeatureContext<?> context, @Nullable Player player, boolean ownerOnly) {
        return !ownerOnly || (player != null && player.getUniqueId().equals(context.instance().ownerId()));
    }

    private static <T extends SignalMatcher> Codec<T> register(String typeName, Codec<T> codec) {
        return Registries.DUNGEON_SIGNAL_MATCHER_CODEC.register(typeName, codec);
    }
}
