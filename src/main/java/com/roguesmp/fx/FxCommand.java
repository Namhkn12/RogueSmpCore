package com.roguesmp.fx;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.constant.DamageType;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.fx.demo.ExplosionEffectDemo;
import com.roguesmp.utils.DamageUtils;
import dev.jorel.commandapi.CommandAPICommand;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class FxCommand {

    private static final double MAX_RAY_DISTANCE = 64;
    private static final double BLAST_DAMAGE = 6.0;
    private static final double BLAST_HIT_BAND = 0.75;

    private FxCommand() {
    }

    public static void register() {
        new CommandAPICommand("fx")
                .withSubcommand(new CommandAPICommand("explosion")
                        .executesPlayer((player, args) -> {
                            Location target = lookAtLocation(player);
                            ExplosionEffectDemo.Explosion explosion = ExplosionEffectDemo.create(target);

                            FxHandle handle = FxEngine.getInstance().play(explosion.effect());
                            watchShockwaveForHits(target.getWorld(), explosion.shockwave(), handle);
                        }))
                .register();
    }

    private static Location lookAtLocation(Player player) {
        RayTraceResult ray = player.rayTraceBlocks(MAX_RAY_DISTANCE);
        return ray != null
                ? ray.getHitPosition().toLocation(player.getWorld())
                : player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(MAX_RAY_DISTANCE));
    }

    /**
     * Demonstrates the caller-owned hit-detection pattern: fx never runs gameplay logic itself, so
     * this polls the shockwave part's live {@link FxPart#currentTransform()} every tick — instead of
     * a single check at cast time — to catch players exactly as the expanding ring passes them.
     */
    private static void watchShockwaveForHits(World world, FxPart shockwave, FxHandle handle) {
        new BukkitRunnable() {
            private final Set<UUID> alreadyHit = new HashSet<>();

            @Override
            public void run() {
                if (!handle.isActive()) {
                    cancel();
                    return;
                }

                FxTransform ring = shockwave.currentTransform();
                Location center = ring.toLocation(world);
                double radius = ExplosionEffectDemo.SHOCKWAVE_BASE_RADIUS * ring.scale().x();

                for (Player nearby : center.getNearbyPlayers(radius + BLAST_HIT_BAND)) {
                    if (alreadyHit.contains(nearby.getUniqueId())) continue;
                    if (FastMath.abs(nearby.getLocation().distance(center) - radius) > BLAST_HIT_BAND) continue;

                    alreadyHit.add(nearby.getUniqueId());
                    DamageUtils.damage(nearby, null, BLAST_DAMAGE, new DamageEvent.Metadata(DamageType.BLAST));
                }
            }
        }.runTaskTimer(RogueSmpCore.getInstance(), 0L, 1L);
    }
}
