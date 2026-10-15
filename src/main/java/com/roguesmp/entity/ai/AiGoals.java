package com.roguesmp.entity.ai;

import com.roguesmp.codec.Codec;
import com.roguesmp.entity.ai.goal.BowAttackGoal;
import com.roguesmp.registry.Registries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class AiGoals {

    private AiGoals() {
    }

    public static void loadClass() {
    }

    public record Param(String name, String description) {
    }

    private static final Param PRIORITY = new Param("priority", "Độ ưu tiên, số nhỏ hơn được ưu tiên trước");
    private static final String FILTER_FORMAT = "minecraft:<type>, #<tag entity> hoặc id entity custom";
    private static final Map<String, List<Param>> PARAMS = new HashMap<>();

    public static List<Param> params(String type) {
        return PARAMS.getOrDefault(type, List.of(PRIORITY));
    }

    private static <T extends AiGoal> void register(String id, Codec<T> codec, Param... params) {
        Registries.AI_GOAL_CODEC.register(id, codec);
        List<Param> all = new ArrayList<>();
        all.add(PRIORITY);
        all.addAll(List.of(params));
        PARAMS.put(id, List.copyOf(all));
    }

    static {
        register(MeleeAttack.TYPE, MeleeAttack.CODEC,
                new Param("speed", "Hệ số tốc độ khi đuổi theo mục tiêu"),
                new Param("followUnseen", "Tiếp tục đuổi khi mất tầm nhìn mục tiêu (true/false)"));
        register(RangedAttack.TYPE, RangedAttack.CODEC,
                new Param("speed", "Hệ số tốc độ di chuyển khi tấn công"),
                new Param("interval", "Thời gian chờ giữa các lần bắn (tick)"),
                new Param("radius", "Tầm bắn tối đa (block)"));
        register(BowAttack.TYPE, BowAttack.CODEC,
                new Param("speed", "Hệ số tốc độ di chuyển khi tấn công"),
                new Param("interval", "Thời gian chờ sau mỗi phát bắn trước khi kéo cung lại (tick)"),
                new Param("pullTime", "Thời gian kéo cung trước khi bắn (tick, vanilla = 20), sát thương luôn như kéo đầy"),
                new Param("radius", "Tầm bắn tối đa (block)"));
        register(LeapAtTarget.TYPE, LeapAtTarget.CODEC,
                new Param("yVelocity", "Lực nhảy theo chiều dọc khi lao vào mục tiêu"));
        register(AvoidEntity.TYPE, AvoidEntity.CODEC,
                new Param("entity", "Entity cần né: " + FILTER_FORMAT),
                new Param("distance", "Khoảng cách bắt đầu né (block)"),
                new Param("walkSpeed", "Hệ số tốc độ khi né lúc còn xa"),
                new Param("sprintSpeed", "Hệ số tốc độ khi né lúc ở gần (dưới 7 block)"));
        register(Panic.TYPE, Panic.CODEC,
                new Param("speed", "Hệ số tốc độ bỏ chạy sau khi bị đánh"));
        register(RandomStroll.TYPE, RandomStroll.CODEC,
                new Param("speed", "Hệ số tốc độ đi lang thang"),
                new Param("avoidWater", "Tránh đi xuống nước (true/false)"));
        register(LookAtPlayer.TYPE, LookAtPlayer.CODEC,
                new Param("range", "Khoảng cách nhìn về phía người chơi (block)"));
        register(RandomLookAround.TYPE, RandomLookAround.CODEC);
        register(Swim.TYPE, Swim.CODEC);
        register(NearestAttackableTarget.TYPE, NearestAttackableTarget.CODEC,
                new Param("target", "Mục tiêu tấn công: " + FILTER_FORMAT),
                new Param("mustSee", "Chỉ nhắm mục tiêu nhìn thấy được (true/false)"),
                new Param("mustReach", "Chỉ nhắm mục tiêu đi tới được (true/false)"),
                new Param("interval", "Chu kỳ tìm mục tiêu mới (tick)"));
        register(HurtByTarget.TYPE, HurtByTarget.CODEC,
                new Param("alertOthers", "Gọi đồng loại cùng tấn công kẻ gây sát thương (true/false)"));
    }

    public record MeleeAttack(int priority, double speed, boolean followUnseen) implements AiGoal {
        public static final String TYPE = "melee_attack";
        public static final Codec<MeleeAttack> CODEC = Codec.composite(
                Codec.INT.optionalFieldOf("priority", 2).forGetter(MeleeAttack::priority),
                Codec.DOUBLE.optionalFieldOf("speed", 1.0).forGetter(MeleeAttack::speed),
                Codec.BOOLEAN.optionalFieldOf("followUnseen", true).forGetter(MeleeAttack::followUnseen),
                MeleeAttack::new
        );

        @Override
        public String getTypeId() {
            return TYPE;
        }

        @Override
        public @Nullable Goal create(Mob mob) {
            return mob instanceof PathfinderMob pathfinder ? new MeleeAttackGoal(pathfinder, speed, followUnseen) : null;
        }
    }

    public record RangedAttack(int priority, double speed, int interval, float radius) implements AiGoal {
        public static final String TYPE = "ranged_attack";
        public static final Codec<RangedAttack> CODEC = Codec.composite(
                Codec.INT.optionalFieldOf("priority", 2).forGetter(RangedAttack::priority),
                Codec.DOUBLE.optionalFieldOf("speed", 1.0).forGetter(RangedAttack::speed),
                Codec.INT.optionalFieldOf("interval", 40).forGetter(RangedAttack::interval),
                Codec.FLOAT.optionalFieldOf("radius", 15f).forGetter(RangedAttack::radius),
                RangedAttack::new
        );

        @Override
        public String getTypeId() {
            return TYPE;
        }

        @Override
        public @Nullable Goal create(Mob mob) {
            return mob instanceof RangedAttackMob shooter ? new RangedAttackGoal(shooter, speed, interval, radius) : null;
        }
    }

    public record BowAttack(int priority, double speed, int interval, int pullTime, float radius) implements AiGoal {
        public static final String TYPE = "bow_attack";
        public static final Codec<BowAttack> CODEC = Codec.composite(
                Codec.INT.optionalFieldOf("priority", 2).forGetter(BowAttack::priority),
                Codec.DOUBLE.optionalFieldOf("speed", 1.0).forGetter(BowAttack::speed),
                Codec.INT.optionalFieldOf("interval", 20).forGetter(BowAttack::interval),
                Codec.INT.optionalFieldOf("pullTime", 20).forGetter(BowAttack::pullTime),
                Codec.FLOAT.optionalFieldOf("radius", 15f).forGetter(BowAttack::radius),
                BowAttack::new
        );

        @Override
        public String getTypeId() {
            return TYPE;
        }

        @Override
        public @Nullable Goal create(Mob mob) {
            return mob instanceof Monster && mob instanceof RangedAttackMob ? bowGoal((Monster & RangedAttackMob) mob) : null;
        }

        private <T extends Monster & RangedAttackMob> Goal bowGoal(T mob) {
            return new BowAttackGoal<>(mob, speed, interval, pullTime, radius);
        }
    }

    public record LeapAtTarget(int priority, float yVelocity) implements AiGoal {
        public static final String TYPE = "leap_at_target";
        public static final Codec<LeapAtTarget> CODEC = Codec.composite(
                Codec.INT.optionalFieldOf("priority", 3).forGetter(LeapAtTarget::priority),
                Codec.FLOAT.optionalFieldOf("yVelocity", 0.4f).forGetter(LeapAtTarget::yVelocity),
                LeapAtTarget::new
        );

        @Override
        public String getTypeId() {
            return TYPE;
        }

        @Override
        public Goal create(Mob mob) {
            return new LeapAtTargetGoal(mob, yVelocity);
        }
    }

    public record AvoidEntity(int priority, EntityFilter entity, float distance, double walkSpeed, double sprintSpeed) implements AiGoal {
        public static final String TYPE = "avoid_entity";
        public static final Codec<AvoidEntity> CODEC = Codec.composite(
                Codec.INT.optionalFieldOf("priority", 1).forGetter(AvoidEntity::priority),
                EntityFilter.CODEC.optionalFieldOf("entity", EntityFilter.PLAYER).forGetter(AvoidEntity::entity),
                Codec.FLOAT.optionalFieldOf("distance", 8f).forGetter(AvoidEntity::distance),
                Codec.DOUBLE.optionalFieldOf("walkSpeed", 1.0).forGetter(AvoidEntity::walkSpeed),
                Codec.DOUBLE.optionalFieldOf("sprintSpeed", 1.3).forGetter(AvoidEntity::sprintSpeed),
                AvoidEntity::new
        );

        @Override
        public String getTypeId() {
            return TYPE;
        }

        @Override
        public @Nullable Goal create(Mob mob) {
            if (!(mob instanceof PathfinderMob pathfinder)) return null;
            return new AvoidEntityGoal<>(pathfinder, LivingEntity.class, distance, walkSpeed, sprintSpeed, entity::test);
        }
    }

    public record Panic(int priority, double speed) implements AiGoal {
        public static final String TYPE = "panic";
        public static final Codec<Panic> CODEC = Codec.composite(
                Codec.INT.optionalFieldOf("priority", 1).forGetter(Panic::priority),
                Codec.DOUBLE.optionalFieldOf("speed", 1.25).forGetter(Panic::speed),
                Panic::new
        );

        @Override
        public String getTypeId() {
            return TYPE;
        }

        @Override
        public @Nullable Goal create(Mob mob) {
            return mob instanceof PathfinderMob pathfinder ? new PanicGoal(pathfinder, speed) : null;
        }
    }

    public record RandomStroll(int priority, double speed, boolean avoidWater) implements AiGoal {
        public static final String TYPE = "random_stroll";
        public static final Codec<RandomStroll> CODEC = Codec.composite(
                Codec.INT.optionalFieldOf("priority", 6).forGetter(RandomStroll::priority),
                Codec.DOUBLE.optionalFieldOf("speed", 1.0).forGetter(RandomStroll::speed),
                Codec.BOOLEAN.optionalFieldOf("avoidWater", true).forGetter(RandomStroll::avoidWater),
                RandomStroll::new
        );

        @Override
        public String getTypeId() {
            return TYPE;
        }

        @Override
        public @Nullable Goal create(Mob mob) {
            if (!(mob instanceof PathfinderMob pathfinder)) return null;
            return avoidWater ? new WaterAvoidingRandomStrollGoal(pathfinder, speed) : new RandomStrollGoal(pathfinder, speed);
        }
    }

    public record LookAtPlayer(int priority, float range) implements AiGoal {
        public static final String TYPE = "look_at_player";
        public static final Codec<LookAtPlayer> CODEC = Codec.composite(
                Codec.INT.optionalFieldOf("priority", 7).forGetter(LookAtPlayer::priority),
                Codec.FLOAT.optionalFieldOf("range", 8f).forGetter(LookAtPlayer::range),
                LookAtPlayer::new
        );

        @Override
        public String getTypeId() {
            return TYPE;
        }

        @Override
        public Goal create(Mob mob) {
            return new LookAtPlayerGoal(mob, Player.class, range);
        }
    }

    public record RandomLookAround(int priority) implements AiGoal {
        public static final String TYPE = "random_look_around";
        public static final Codec<RandomLookAround> CODEC = Codec.composite(
                Codec.INT.optionalFieldOf("priority", 8).forGetter(RandomLookAround::priority),
                RandomLookAround::new
        );

        @Override
        public String getTypeId() {
            return TYPE;
        }

        @Override
        public Goal create(Mob mob) {
            return new RandomLookAroundGoal(mob);
        }
    }

    public record Swim(int priority) implements AiGoal {
        public static final String TYPE = "float";
        public static final Codec<Swim> CODEC = Codec.composite(
                Codec.INT.optionalFieldOf("priority", 0).forGetter(Swim::priority),
                Swim::new
        );

        @Override
        public String getTypeId() {
            return TYPE;
        }

        @Override
        public Goal create(Mob mob) {
            return new FloatGoal(mob);
        }
    }

    public record NearestAttackableTarget(int priority, EntityFilter target, boolean mustSee, boolean mustReach, int interval) implements AiGoal {
        public static final String TYPE = "nearest_attackable_target";
        public static final Codec<NearestAttackableTarget> CODEC = Codec.composite(
                Codec.INT.optionalFieldOf("priority", 1).forGetter(NearestAttackableTarget::priority),
                EntityFilter.CODEC.optionalFieldOf("target", EntityFilter.PLAYER).forGetter(NearestAttackableTarget::target),
                Codec.BOOLEAN.optionalFieldOf("mustSee", true).forGetter(NearestAttackableTarget::mustSee),
                Codec.BOOLEAN.optionalFieldOf("mustReach", false).forGetter(NearestAttackableTarget::mustReach),
                Codec.INT.optionalFieldOf("interval", 10).forGetter(NearestAttackableTarget::interval),
                NearestAttackableTarget::new
        );

        @Override
        public String getTypeId() {
            return TYPE;
        }

        @Override
        public boolean isTarget() {
            return true;
        }

        @Override
        public Goal create(Mob mob) {
            return new NearestAttackableTargetGoal<>(mob, LivingEntity.class, interval, mustSee, mustReach, (entity, level) -> target.test(entity));
        }
    }

    public record HurtByTarget(int priority, boolean alertOthers) implements AiGoal {
        public static final String TYPE = "hurt_by_target";
        public static final Codec<HurtByTarget> CODEC = Codec.composite(
                Codec.INT.optionalFieldOf("priority", 1).forGetter(HurtByTarget::priority),
                Codec.BOOLEAN.optionalFieldOf("alertOthers", false).forGetter(HurtByTarget::alertOthers),
                HurtByTarget::new
        );

        @Override
        public String getTypeId() {
            return TYPE;
        }

        @Override
        public boolean isTarget() {
            return true;
        }

        @Override
        public @Nullable Goal create(Mob mob) {
            if (!(mob instanceof PathfinderMob pathfinder)) return null;
            HurtByTargetGoal goal = new HurtByTargetGoal(pathfinder);
            return alertOthers ? goal.setAlertOthers() : goal;
        }
    }
}
