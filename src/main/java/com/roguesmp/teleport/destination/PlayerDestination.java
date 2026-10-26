package com.roguesmp.teleport.destination;

import com.roguesmp.codec.Codec;
import com.roguesmp.teleport.TeleportDestination;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public record PlayerDestination(String name) implements TeleportDestination {

    public static final String TYPE_KEY = "player";

    public static final Codec<PlayerDestination> CODEC = Codec.STRING.fieldOf("name")
            .xmap(PlayerDestination::new, PlayerDestination::name)
            .codec();

    @Override
    public String getTypeId() {
        return TYPE_KEY;
    }

    @Override
    public @Nullable Location resolve(Player player) {
        Player target = Bukkit.getPlayerExact(name);
        return target == null ? null : target.getLocation();
    }

    @Override
    public String format() {
        return "player " + name;
    }
}
