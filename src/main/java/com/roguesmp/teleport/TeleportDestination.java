package com.roguesmp.teleport;

import com.roguesmp.codec.Codec;
import com.roguesmp.registry.Registries;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public interface TeleportDestination {

    Codec<TeleportDestination> CODEC = Codec.dispatch(
            TeleportDestination::getTypeId,
            type -> Registries.TELEPORT_DESTINATION_CODEC.getOrThrow(type)
    );

    String getTypeId();

    @Nullable Location resolve(Player player);

    default boolean isAvailable(Player player) {
        return resolve(player) != null;
    }

    String format();
}
