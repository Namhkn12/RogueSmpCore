package com.roguesmp.dungeon.room;

import com.roguesmp.effect.impl.VanillaPotionEffect;
import com.roguesmp.effect.SmpEffect;
import com.roguesmp.codec.Codec;
import com.roguesmp.effect.EffectManager;
import com.roguesmp.effect.impl.DamageIncreaseEffect;
import com.roguesmp.registry.Registries;
import com.roguesmp.utils.Utils;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class RoomEffects {

    public record Darkness(boolean flicker) implements RoomEffect<Void> {
        public static final Codec<Darkness> CODEC = Codec.composite(
                Codec.BOOLEAN.optionalFieldOf("flicker", false).forGetter(Darkness::flicker),
                Darkness::new
        );

        private static final int CYCLE_SECONDS = 20;
        private static final String DARKNESS_SOURCE = "room/darkness";
        private static final SmpEffect.BaseProperties DARKNESS_BLINDNESS = new SmpEffect.BaseProperties(
                40, SmpEffect.DeathBehavior.REMOVE_ON_DEATH, SmpEffect.DisplayMode.HIDDEN, false);

        @Override
        public String getTypeId() {
            return "darkness";
        }

        @Override
        public Void createState() {
            return null;
        }

        @Override
        public void onStart(RoomEffectContext<Void> context) {
            context.players().forEach(player -> player.sendMessage(Utils.fromString("<yellow>Bóng tối đang dõi theo bạn...")));
        }

        @Override
        public void onTick(RoomEffectContext<Void> context) {
            int phase = context.seconds() % CYCLE_SECONDS;
            boolean blind = !flicker || phase < 10 || phase > 15;
            for (Player player : context.players()) {
                if (blind) EffectManager.getInstance().addEffect(player, DARKNESS_SOURCE, new VanillaPotionEffect(DARKNESS_BLINDNESS, PotionEffectType.BLINDNESS, 0));
                else EffectManager.getInstance().clearEffects(player, DARKNESS_SOURCE);
            }
        }

        @Override
        public void onEnd(RoomEffectContext<Void> context) {
            context.players().forEach(player -> EffectManager.getInstance().clearEffects(player, DARKNESS_SOURCE));
        }
    }

    public record CreepingDread(int maxTriggers, double spawnChance, int minInterval, int maxInterval) implements RoomEffect<CreepingDread.State> {
        public static final Codec<CreepingDread> CODEC = Codec.composite(
                Codec.INT.optionalFieldOf("max_triggers", 4).forGetter(CreepingDread::maxTriggers),
                Codec.DOUBLE.optionalFieldOf("spawn_chance", 0.25).forGetter(CreepingDread::spawnChance),
                Codec.INT.optionalFieldOf("min_interval", 40).forGetter(CreepingDread::minInterval),
                Codec.INT.optionalFieldOf("max_interval", 100).forGetter(CreepingDread::maxInterval),
                CreepingDread::new
        );

        public static final class State {
            private final Map<UUID, Integer> triggers = new HashMap<>();
            private final Map<UUID, Integer> nextSecond = new HashMap<>();
        }

        @Override
        public String getTypeId() {
            return "creeping_dread";
        }

        @Override
        public State createState() {
            return new State();
        }

        @Override
        public void onTick(RoomEffectContext<State> context) {
            ThreadLocalRandom random = ThreadLocalRandom.current();
            State state = context.state();
            for (Player player : context.players()) {
                UUID id = player.getUniqueId();
                int count = state.triggers.getOrDefault(id, 0);
                if (count >= maxTriggers || context.seconds() < state.nextSecond.getOrDefault(id, 0)) continue;

                Location near = player.getLocation().add((random.nextDouble() - 0.5) * 4, 0, (random.nextDouble() - 0.5) * 4);
                player.playSound(near, Sound.ENTITY_CREEPER_PRIMED, 1f, 1f);
                if (random.nextDouble() < spawnChance) {
                    Location behind = player.getLocation().add(player.getLocation().getDirection().normalize().multiply(-3));
                    player.getWorld().spawn(behind, Creeper.class);
                }

                state.triggers.put(id, count + 1);
                state.nextSecond.put(id, context.seconds() + minInterval + random.nextInt(Math.max(1, maxInterval - minInterval)));
            }
        }
    }

    public record StrengthBuff(int durationTicks, int amplifier) implements RoomEffect<Void> {
        public static final Codec<StrengthBuff> CODEC = Codec.composite(
                Codec.INT.optionalFieldOf("duration_ticks", 20 * 60 * 3).forGetter(StrengthBuff::durationTicks),
                Codec.INT.optionalFieldOf("amplifier", 1).forGetter(StrengthBuff::amplifier),
                StrengthBuff::new
        );

        @Override
        public String getTypeId() {
            return "strength_buff";
        }

        @Override
        public Void createState() {
            return null;
        }

        @Override
        public void onStart(RoomEffectContext<Void> context) {
            context.players().forEach(player -> EffectManager.getInstance().addEffect(player, "damage_increase", new DamageIncreaseEffect(durationTicks, amplifier)));
        }
    }

    public static final Codec<Darkness> DARKNESS = register("darkness", Darkness.CODEC);
    public static final Codec<CreepingDread> CREEPING_DREAD = register("creeping_dread", CreepingDread.CODEC);
    public static final Codec<StrengthBuff> STRENGTH_BUFF = register("strength_buff", StrengthBuff.CODEC);

    public static void loadClass() {

    }

    private static <T extends RoomEffect<?>> Codec<T> register(String typeName, Codec<T> codec) {
        return Registries.DUNGEON_ROOM_EFFECT_CODEC.register(typeName, codec);
    }
}
