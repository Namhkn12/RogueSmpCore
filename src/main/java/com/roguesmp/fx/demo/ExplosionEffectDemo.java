package com.roguesmp.fx.demo;

import com.destroystokyo.paper.ParticleBuilder;
import com.roguesmp.fx.FxEffect;
import com.roguesmp.fx.FxPart;
import com.roguesmp.fx.FxTransform;
import com.roguesmp.fx.motion.GravityMotion;
import com.roguesmp.fx.motion.SpinMotion;
import com.roguesmp.fx.render.BlockDisplayRenderer;
import com.roguesmp.fx.render.ParticleRenderer;
import com.roguesmp.fx.shape.CircleShape;
import com.roguesmp.fx.shape.PointShape;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.joml.Vector3f;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Demo effect showcasing the fx system: a one-shot flash, an expanding shockwave ring, flying spark
 * particles and spinning block debris — a flash/shockwave rendered with particles, debris rendered
 * as real block displays, each spark/chunk moving on its own {@code FxMotion} independently of the
 * others, all combined as parts of one {@link FxEffect}.
 */
public final class ExplosionEffectDemo {

    /** Base radius the shockwave ring shape is built at (before growth motion scales it). */
    public static final double SHOCKWAVE_BASE_RADIUS = 1.0;

    private static final int DURATION_TICKS = 45;
    private static final int SHOCKWAVE_GROWTH_TICKS = 15;
    private static final int SHOCKWAVE_POINTS = 96;
    private static final float SHOCKWAVE_GROWTH_PER_TICK = 0.35f;
    private static final int SPARK_COUNT = 10;
    private static final int DEBRIS_COUNT = 6;
    private static final Material[] DEBRIS_BLOCKS = {
            Material.BLACKSTONE, Material.COBBLESTONE, Material.NETHERRACK, Material.MAGMA_BLOCK
    };

    private ExplosionEffectDemo() {
    }

    /** @param shockwave the ring part, kept accessible so callers can read its live radius (via {@link FxPart#currentTransform()}) for their own hit detection */
    public record Explosion(FxEffect effect, FxPart shockwave) {
    }

    public static Explosion create(Location origin) {
        if (origin.getWorld() != null) {
            origin.getWorld().playSound(origin, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.MASTER, 2.5f, 0.9f);
        }

        FxPart shockwave = shockwave();

        FxEffect.Builder builder = FxEffect.builder(origin).duration(DURATION_TICKS);
        builder.part(flash());
        builder.part(shockwave);

        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < SPARK_COUNT; i++) builder.part(spark(random));
        for (int i = 0; i < DEBRIS_COUNT; i++) builder.part(debris(random));

        return new Explosion(builder.build(), shockwave);
    }

    private static FxPart flash() {
        ParticleRenderer renderer = new ParticleRenderer(List.of(
                new ParticleBuilder(Particle.FLASH).color(Color.WHITE).count(1),
                new ParticleBuilder(Particle.EXPLOSION).count(1),
                new ParticleBuilder(Particle.LARGE_SMOKE).count(15).offset(0.3, 0.3, 0.3).extra(0.02)
        ));
        return new FxPart(new PointShape(), renderer).once();
    }

    private static FxPart shockwave() {
        ParticleRenderer renderer = new ParticleRenderer(
                new ParticleBuilder(Particle.DUST).data(new Particle.DustOptions(Color.ORANGE, 1.4f)).count(0));

        return new FxPart(new CircleShape(SHOCKWAVE_BASE_RADIUS, SHOCKWAVE_POINTS), renderer)
                .motion((current, tick) -> {
                    if (tick > SHOCKWAVE_GROWTH_TICKS) return current;
                    Vector3f grown = current.scale().add(new Vector3f(SHOCKWAVE_GROWTH_PER_TICK, 0f, SHOCKWAVE_GROWTH_PER_TICK));
                    return new FxTransform(current.position(), current.rotation(), grown);
                })
                // Point count is dense enough to look connected up to the ring's max radius; no
                // point spending particles keeping it "drawn" once it has stopped growing.
                .activeTicks(tick -> tick <= SHOCKWAVE_GROWTH_TICKS);
    }

    private static FxPart spark(ThreadLocalRandom random) {
        Vector3f velocity = randomOutwardVelocity(random, 0.25, 0.55, 0.2, 0.4);
        ParticleRenderer renderer = new ParticleRenderer(new ParticleBuilder(Particle.LAVA).count(1));

        return new FxPart(new PointShape(), renderer)
                .motion(new GravityMotion(velocity, 0.03f));
    }

    private static FxPart debris(ThreadLocalRandom random) {
        Vector3f velocity = randomOutwardVelocity(random, 0.15, 0.3, 0.3, 0.5);
        Vector3f spinAxis = new Vector3f(random.nextFloat(), random.nextFloat(), random.nextFloat());
        Material block = DEBRIS_BLOCKS[random.nextInt(DEBRIS_BLOCKS.length)];

        BlockDisplayRenderer renderer = new BlockDisplayRenderer(block.createBlockData(), new Vector3f(0.35f, 0.35f, 0.35f));
        return new FxPart(new PointShape(), renderer)
                .motion(new GravityMotion(velocity, 0.025f).andThen(new SpinMotion(spinAxis, random.nextInt(10, 25))));
    }

    private static Vector3f randomOutwardVelocity(ThreadLocalRandom random, double minSpeed, double maxSpeed, double minLift, double maxLift) {
        double angle = random.nextDouble(0, FastMath.PI * 2);
        double speed = random.nextDouble(minSpeed, maxSpeed);
        return new Vector3f(
                (float) (FastMath.cos(angle) * speed),
                (float) random.nextDouble(minLift, maxLift),
                (float) (FastMath.sin(angle) * speed)
        );
    }
}
