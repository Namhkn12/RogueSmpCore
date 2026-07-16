package com.roguesmp.npc;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.npc.action.NpcAction;
import io.papermc.paper.entity.LookAnchor;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SmpNpc {
    private final BaseNpc baseNpc;
    private final Entity entity; //Entity's uuid
    private final List<NpcAction> npcActions = new ArrayList<>();

    private final float defaultYaw;
    private final float defaultPitch = 0;

    private static final int TRACKING_RANGE = 5;

    public SmpNpc(BaseNpc baseNpc, Entity entity) {
        this.baseNpc = baseNpc;
        this.entity = entity;
        baseNpc.processEntity(entity);
        this.defaultYaw = entity.getLocation().getYaw();

        this.npcActions.addAll(baseNpc.getActions());

        entity.getScheduler().runAtFixedRate(RogueSmpCore.getInstance(), (task) -> {
            if (!entity.isValid()) {
                task.cancel();
                return;
            }

            // Find the nearest player within range

            Player nearest = getNearestPlayer(entity.getLocation(), TRACKING_RANGE);
            if (nearest != null) {
                // lookAt requires the X, Y, Z coordinates to focus on.
                // We focus on the player's eye location for a natural look.
                Location loc = nearest.getEyeLocation();
                entity.lookAt(loc.getX(), loc.getY(), loc.getZ(), LookAnchor.EYES);
            } else {
                entity.setRotation(defaultYaw, defaultPitch);
            }
        }, null, 1L, 3L);

    }

    public BaseNpc getBaseNpc() {
        return baseNpc;
    }

    public UUID getUuid() {
        return entity.getUniqueId();
    }

    public List<NpcAction> getNpcActions() {
        return npcActions;
    }

    public void onRightClick(PlayerInteractEntityEvent event) {
        for (NpcAction npcAction : npcActions) {
            npcAction.onRightClick(event);
        }
    }

    public void onLeftClick(PrePlayerAttackEntityEvent event) {
        for (NpcAction npcAction : npcActions) {
            npcAction.onLeftClick(event);
        }
    }

    public void onDamage(EntityDamageEvent event) {
        event.setCancelled(true); //Always invulnerable
    }

    private static @Nullable Player getNearestPlayer(Location location, double range) {
        double closestDistanceSq = range * range;
        Player closestPlayer = null;
        for (Player player : location.getWorld().getPlayers()) {
            if (player.getGameMode() == GameMode.SPECTATOR) {
                continue;
            }

            double distSq = player.getLocation().distanceSquared(location);

            if (distSq < closestDistanceSq) {
                closestDistanceSq = distSq;
                closestPlayer = player;
            }
        }
        return closestPlayer;
    }
}
