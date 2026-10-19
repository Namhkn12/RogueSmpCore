package com.roguesmp.item.beam;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.utils.DamageUtils;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class MageBeam {
    private static final double STEP = 0.35;
    private static final int TRAIL_INTERVAL = 4;
    private static final int WITCH_INTERVAL = 2;
    private static final double START_OFFSET = 1.0;
    private static final double SPEED_PER_TICK = 1.5;
    private static final double RADIUS = 0.3;

    private final Particle.DustOptions core;
    private final List<Particle> trailParticles = new ArrayList<>();
    private final List<Consumer<LivingEntity>> hitEffects = new ArrayList<>();

    private Player shooter;
    private Location position;
    private Vector direction;
    private double remaining;
    private double travelled;
    private int particleIndex;

    public MageBeam(Color color) {
        this.core = new Particle.DustOptions(color, 0.5f);
    }

    public void addTrailParticle(Particle particle) {
        trailParticles.add(particle);
    }

    public void addHitEffect(Consumer<LivingEntity> effect) {
        hitEffects.add(effect);
    }

    public void launch(Player shooter, double range) {
        this.shooter = shooter;
        this.position = shooter.getEyeLocation();
        this.direction = position.getDirection().normalize();
        this.remaining = range;
        if (advance()) {
            shooter.getScheduler().runAtFixedRate(RogueSmpCore.getInstance(), this::tick, null, 1, 1);
        }
    }

    private void tick(ScheduledTask task) {
        if (!advance()) task.cancel();
    }

    private boolean advance() {
        double segment = Math.min(SPEED_PER_TICK, remaining);
        World world = position.getWorld();
        RayTraceResult hit = world.rayTrace(position, direction, segment, FluidCollisionMode.NEVER, true, RADIUS,
                entity -> entity instanceof LivingEntity && !entity.equals(shooter));

        double length = hit == null ? segment : hit.getHitPosition().distance(position.toVector());
        draw(world, length);
        position.add(direction.clone().multiply(length));
        remaining -= length;

        if (hit == null) return remaining > 0;
        if (hit.getHitEntity() instanceof LivingEntity victim) {
            DamageUtils.damage(victim, shooter, 1, DamageEvent.Metadata.staff());
            hitEffects.forEach(effect -> effect.accept(victim));
        }
        return false;
    }

    private void draw(World world, double length) {
        double end = travelled + length;
        double next = Math.max(START_OFFSET, Math.ceil(travelled / STEP) * STEP);
        for (double d = next; d < end; d += STEP) {
            Location point = position.clone().add(direction.clone().multiply(d - travelled));
            world.spawnParticle(Particle.DUST, point, 1, 0, 0, 0, 0, core);
            if (particleIndex % WITCH_INTERVAL == 0) world.spawnParticle(Particle.WITCH, point, 1, 0.05, 0.05, 0.05, 0);
            if (particleIndex++ % TRAIL_INTERVAL != 0) continue;
            for (Particle particle : trailParticles) {
                world.spawnParticle(particle, point, 1, 0.05, 0.05, 0.05, 0);
            }
        }
        travelled = end;
    }
}
