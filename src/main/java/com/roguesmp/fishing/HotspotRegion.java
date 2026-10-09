package com.roguesmp.fishing;

import com.roguesmp.codec.Codec;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.jetbrains.annotations.Nullable;

public record HotspotRegion(Key world, String region) {

    public static final Codec<HotspotRegion> CODEC = Codec.composite(
            Codec.KEY.fieldOf("world").forGetter(HotspotRegion::world),
            Codec.STRING.fieldOf("region").forGetter(HotspotRegion::region),
            HotspotRegion::new
    );

    public @Nullable World resolveWorld() {
        return Bukkit.getWorld(new NamespacedKey(world.namespace(), world.value()));
    }

    public String key() {
        return world.asString() + "/" + region;
    }
}
