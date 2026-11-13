package com.roguesmp.billboard;

import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.function.Consumer;

final class PacketEntity<T extends org.bukkit.entity.Entity> {

    private final Entity handle;
    private final T bukkit;

    @SuppressWarnings("unchecked")
    PacketEntity(Entity handle, Location location, Consumer<T> configure) {
        this.handle = handle;
        handle.setPos(location.getX(), location.getY(), location.getZ());
        this.bukkit = (T) handle.getBukkitEntity();
        configure.accept(bukkit);
        handle.getEntityData().packDirty();
    }

    int getEntityId() {
        return handle.getId();
    }

    T bukkit() {
        return bukkit;
    }

    void spawn(Player viewer) {
        handle.getEntityData().packDirty();
        send(viewer, new ClientboundAddEntityPacket(handle.getId(), handle.getUUID(), handle.getX(), handle.getY(), handle.getZ(),
                0f, 0f, handle.getType(), 0, Vec3.ZERO, 0));
        send(viewer, new ClientboundSetEntityDataPacket(handle.getId(), handle.getEntityData().getNonDefaultValues()));
    }

    void sync(Player viewer) {
        List<SynchedEntityData.DataValue<?>> dirty = handle.getEntityData().packDirty();
        if (dirty != null) send(viewer, new ClientboundSetEntityDataPacket(handle.getId(), dirty));
    }

    void despawn(Player viewer) {
        send(viewer, new ClientboundRemoveEntitiesPacket(IntList.of(handle.getId())));
    }

    private static void send(Player viewer, Packet<?> packet) {
        ((CraftPlayer) viewer).getHandle().connection.send(packet);
    }
}
