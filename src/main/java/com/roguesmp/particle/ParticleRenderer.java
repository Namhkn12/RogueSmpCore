package com.roguesmp.particle;

import com.destroystokyo.paper.ParticleBuilder;
import com.roguesmp.particle.animation.ParticleAnimation;
import com.roguesmp.particle.path.ParticlePath;
import com.roguesmp.particle.shape.ParticleShape;
import org.bukkit.Location;
import org.bukkit.util.Vector;

public class ParticleRenderer {

    public static void render(
            ParticleBuilder particleBuilder,
            ParticleShape shape,
            ParticlePath path,
            ParticleAnimation animation,
            int tick
    ) {

        Location center = path.getPosition(tick);
        Vector direction = path.getDirection(tick);

        animation.prepare(tick, direction);

        for (Vector point : shape.getPoints()) {

            Vector animated = animation.apply(point);

            Location loc = center.clone().add(animated);
            particleBuilder.location(loc).spawn();
        }
    }
}
