package com.roguesmp.teleport.destination;

import com.roguesmp.codec.Codec;
import com.roguesmp.codec.MapCodec;
import com.roguesmp.teleport.TeleportDestination;
import com.roguesmp.world.Hub;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public record HubDestination() implements TeleportDestination {

    public static final String TYPE_KEY = "hub";
    public static final HubDestination INSTANCE = new HubDestination();

    public static final Codec<HubDestination> CODEC = MapCodec.unit(() -> INSTANCE).codec();

    @Override
    public String getTypeId() {
        return TYPE_KEY;
    }

    @Override
    public Location resolve(Player player) {
        return Hub.spawn();
    }

    @Override
    public String format() {
        return "hub";
    }
}
