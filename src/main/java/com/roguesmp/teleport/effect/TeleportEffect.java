package com.roguesmp.teleport.effect;

import com.roguesmp.codec.Codec;
import com.roguesmp.codec.DataResult;
import com.roguesmp.codec.DynamicOps;
import com.roguesmp.registry.Registries;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface TeleportEffect {

    Codec<TeleportEffect> CODEC = new Codec<>() {
        private final Codec<TeleportEffect> full = Codec.dispatch(TeleportEffect::getTypeId, type -> Registries.TELEPORT_EFFECT_CODEC.getOrThrow(type));

        @Override
        public <O> DataResult<O> encode(TeleportEffect input, DynamicOps<O> ops) {
            if (input.equals(TeleportEffects.PRESETS.get(input.getTypeId()))) return Codec.STRING.encode(input.getTypeId(), ops);
            return full.encode(input, ops);
        }

        @Override
        public <O> DataResult<TeleportEffect> decode(O input, DynamicOps<O> ops) {
            DataResult<String> name = Codec.STRING.decode(input, ops);
            if (!name.isSuccess()) return full.decode(input, ops);

            TeleportEffect preset = TeleportEffects.PRESETS.get(name.result());
            return preset == null
                    ? DataResult.error("Unknown teleport effect '" + name.result() + "' (presets: " + TeleportEffects.PRESETS.keySet() + ")")
                    : DataResult.success(preset);
        }
    };

    String getTypeId();

    default void onWarmupStart(Player player, int warmupTicks) {

    }

    default void onDepart(Player player, Location from) {

    }

    default void onArrive(Player player) {

    }

    default void onFailed(Player player) {

    }
}
