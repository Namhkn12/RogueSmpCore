package com.roguesmp.teleport.destination;

import com.roguesmp.codec.Codec;
import com.roguesmp.registry.Holder;
import com.roguesmp.registry.Registries;
import com.roguesmp.registry.Registry;
import com.roguesmp.teleport.TeleportDestination;
import com.roguesmp.warp.Warp;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public record WarpDestination(Holder<Warp> warp) implements TeleportDestination {

    public static final String TYPE_KEY = "warp";

    public static final Codec<WarpDestination> CODEC = Registry.referenceCodec(() -> Registries.WARP).fieldOf("warp")
            .xmap(WarpDestination::new, WarpDestination::warp)
            .codec();

    @Override
    public String getTypeId() {
        return TYPE_KEY;
    }

    @Override
    public @Nullable Location resolve(Player player) {
        if (!warp.isBound() || warp.value().destination() instanceof WarpDestination) return null;
        return warp.value().destination().resolve(player);
    }

    @Override
    public boolean isAvailable(Player player) {
        return warp.isBound() && !(warp.value().destination() instanceof WarpDestination) && warp.value().destination().isAvailable(player);
    }

    @Override
    public String format() {
        return "warp " + warp.getId();
    }
}
