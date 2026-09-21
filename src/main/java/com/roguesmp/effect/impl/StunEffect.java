package com.roguesmp.effect.impl;

import com.destroystokyo.paper.ParticleBuilder;
import com.roguesmp.constant.Keys;
import com.roguesmp.effect.SmpEffect;
import com.roguesmp.fx.FxEffect;
import com.roguesmp.fx.FxEngine;
import com.roguesmp.fx.FxHandle;
import com.roguesmp.fx.FxPart;
import com.roguesmp.fx.motion.OrbitMotion;
import com.roguesmp.fx.render.ParticleRenderer;
import com.roguesmp.fx.shape.PointShape;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.attribute.Attributable;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/**
 * Stun effect, should always be applied under source_id {@link #ID} so it doesn't stack
 */
public class StunEffect extends SmpEffect {

    public static final String ID = "stun";

    private static final float ORBIT_RADIUS = 0.4f;
    private static final float ORBIT_SPEED_DEGREES_PER_TICK = 30f;
    private static final double ORBIT_HEIGHT_ABOVE_HEAD = 0.3;

    private FxHandle orbitHandle;

    public StunEffect(int duration) {
        super(ID, duration);
    }

    @Override
    public void onGainEffect(Entity entity) {
        if (entity instanceof Attributable attributable) {
            AttributeInstance movement = attributable.getAttribute(Attribute.MOVEMENT_SPEED);
            if (movement != null) {
                movement.removeModifier(Keys.of("stun_speed_effect"));
                movement.addTransientModifier(new AttributeModifier(Keys.of("stun_speed_effect"), -1, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
            }
        }
        if (entity instanceof Mob mob) {
            mob.getPathfinder().stopPathfinding();
            mob.setAware(false);
            mob.setTarget(null);
        }

        FxPart star = new FxPart(new PointShape(),
                new ParticleRenderer(new ParticleBuilder(Particle.DUST).data(new Particle.DustOptions(Color.YELLOW, 1f)).count(0)));

        FxEffect effect = FxEffect.builder(entity.getLocation())
                .rootMotion(new OrbitMotion(() -> headCenter(entity), ORBIT_RADIUS, ORBIT_SPEED_DEGREES_PER_TICK))
                .part(star)
                .duration(-1)
                .stopWhen(() -> !entity.isValid() || entity.isDead())
                .build();

        FxEngine.getInstance().play(effect);
    }

    @Override
    public void onLoseEffect(Entity entity) {
        if (entity instanceof Attributable attributable) {
            AttributeInstance movement = attributable.getAttribute(Attribute.MOVEMENT_SPEED);
            if (movement != null) movement.removeModifier(Keys.of("stun_speed_effect"));
        }
        if (entity instanceof Mob mob) {
            mob.setAware(true);
        }

        if (orbitHandle != null) {
            orbitHandle.stop();
            orbitHandle = null;
        }
    }

    @Override
    public void onDeath(EntityDeathEvent event) {
        if (orbitHandle != null) {
            orbitHandle.stop();
            orbitHandle = null;
        }
    }

    private static Vector3f headCenter(Entity entity) {
        Location loc = entity.getLocation();
        return new Vector3f((float) loc.getX(), (float) (loc.getY() + entity.getHeight() + ORBIT_HEIGHT_ABOVE_HEAD), (float) loc.getZ());
    }

    @Override
    public double getMagnitude() {
        return 0;
    }

    @Override
    public boolean isPersistent() {
        return false;
    }

    @Override
    public @Nullable Component getDisplayComponent() {
        return Component.text("Choáng", NamedTextColor.RED);
    }
}
