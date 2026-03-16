package com.roguesmp.particle;

import com.destroystokyo.paper.ParticleBuilder;
import com.roguesmp.RogueSmpCore;
import com.roguesmp.particle.animation.ParticleAnimation;
import com.roguesmp.particle.path.ParticlePath;
import com.roguesmp.particle.shape.ParticleShape;
import org.bukkit.scheduler.BukkitRunnable;

public class ParticleTask extends BukkitRunnable {

    private final ParticleShape shape;
    private final ParticlePath path;
    private final ParticleAnimation animation;
    private final ParticleBuilder particleBuilder;
    private final int duration;

    private int tick = 0;

    public ParticleTask(
            ParticleBuilder particleBuilder,
            ParticleShape shape,
            ParticlePath path,
            ParticleAnimation animation,
            int duration
    ) {
        this.particleBuilder = particleBuilder;
        this.shape = shape;
        this.path = path;
        this.animation = animation;
        this.duration = duration;
    }

    public void start() {
        runTaskTimer(RogueSmpCore.getInstance(), 0, 1);
    }

    @Override
    public void run() {

        ParticleRenderer.render(particleBuilder, shape, path, animation, tick);

        tick++;

        if (tick > duration) cancel();
    }
}
