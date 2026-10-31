package com.roguesmp.entity.ai.sensor;

import com.roguesmp.utils.EntityUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.PiglinSpecificSensor;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.player.Player;
import org.bukkit.craftbukkit.entity.CraftPiglin;
import org.bukkit.entity.PiglinAbstract;
import org.bukkit.entity.memory.MemoryKey;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;

public class CombatPiglinSensor extends PiglinSpecificSensor {

    private static final Field SENSORS_FIELD;

    static {
        try {
            SENSORS_FIELD = Brain.class.getDeclaredField("sensors");
            SENSORS_FIELD.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static void applyTo(PiglinAbstract piglinAbstract) {
        piglinAbstract.setImmuneToZombification(true);
        if (!(piglinAbstract instanceof org.bukkit.entity.Piglin piglin)) return;
        piglin.setCanPickupItems(false);
        piglin.setIsAbleToHunt(false);
        piglin.setMemory(MemoryKey.ADMIRING_DISABLED, true);
        install(((CraftPiglin) piglin).getHandle().getBrain());
    }

    @SuppressWarnings("unchecked")
    private static void install(Brain<?> brain) {
        try {
            Map<SensorType<?>, Sensor<?>> sensors = (Map<SensorType<?>, Sensor<?>>) SENSORS_FIELD.get(brain);
            if (sensors.containsKey(SensorType.PIGLIN_SPECIFIC_SENSOR)) {
                sensors.put(SensorType.PIGLIN_SPECIFIC_SENSOR, new CombatPiglinSensor());
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    protected void doTick(ServerLevel level, LivingEntity body) {
        super.doTick(level, body);
        Brain<?> brain = body.getBrain();
        brain.eraseMemory(MemoryModuleType.NEAREST_REPELLENT);
        brain.eraseMemory(MemoryModuleType.NEAREST_VISIBLE_ZOMBIFIED);
        Optional<Player> target = brain.getMemory(MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER)
                .filter(player -> !EntityUtils.isInStealth(player.getBukkitEntity()));
        brain.setMemory(MemoryModuleType.NEAREST_TARGETABLE_PLAYER_NOT_WEARING_GOLD, target);
    }
}
