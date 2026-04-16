package com.roguesmp.player.ability.impl.active;

import com.roguesmp.constant.DamageType;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.ability.Ability;
import com.roguesmp.player.ability.AbilityInfo;
import com.roguesmp.player.ability.trigger.AbilityResponse;
import com.roguesmp.utils.DamageUtils;
import com.roguesmp.utils.Utils;
import com.roguesmp.utils.VectorUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class Scrapshot extends Ability {
    public static final String ID = "scrapshot";

    // Cached Attributes
    private final double baseDamage;
    private final double range;
    private final int cooldown;

    // Mechanics Constants
    private static final double RECOIL_VELOCITY = 1.0;
    private static final double SHRAPNEL_DAMAGE_PERCENT = 0.33;
    private static final double SHRAPNEL_RANGE = 4.0;
    private static final double SHRAPNEL_CONE_ANGLE = 50.0;

    private static final Particle.DustOptions LIGHT_IRON = new Particle.DustOptions(Color.fromRGB(180, 180, 180), 1.0f);
    private static final Particle.DustOptions DARK_IRON = new Particle.DustOptions(Color.fromRGB(100, 100, 100), 1.0f);

    public static final AbilityInfo<Scrapshot> INFO = new AbilityInfo<>(
            ID,
            Scrapshot.class,
            Scrapshot::new
    ).registerAction("execute", Scrapshot::handleCast);

    public Scrapshot(SmpPlayer player, int level) {
        super(player, level);
        // Cache attributes from JSON
        this.baseDamage = getAbilityInfo().getAttributeForLevel("damage", level);
        this.range = getAbilityInfo().getAttributeForLevel("range", level);
        this.cooldown = (int) getAbilityInfo().getAttributeForLevel("cooldown", level);
    }

    public AbilityResponse handleCast() {
        if (isOnCooldown()) return AbilityResponse.continueChain();

        Player p = smpPlayer.getBukkitPlayer();
        Location eye = p.getEyeLocation();
        Vector dir = eye.getDirection();
        World world = p.getWorld();

        setCooldownTick(cooldown);

        // 1. Raytrace for Primary Target
        RayTraceResult result = world.rayTrace(eye, dir, range, FluidCollisionMode.NEVER, true, 0.5,
                e -> e instanceof LivingEntity && !(e instanceof Player));

        Location endLoc = (result != null && result.getHitPosition() != null)
                ? result.getHitPosition().toLocation(world)
                : eye.clone().add(dir.clone().multiply(range));

        // 2. Damage & Shrapnel Logic
        if (result != null && result.getHitEntity() instanceof LivingEntity target) {
            double dist = eye.distance(endLoc);

            // Proximity Multiplier (Closer = More Damage)
            double mult = Math.min(1.0, (range * 1.25 - dist) / range);
            double finalDamage = baseDamage * mult;

            DamageUtils.damage(target, p, finalDamage, new DamageEvent.Metadata(ID, DamageType.PROJECTILE_ABILITY));

            // Impact Visuals
            world.spawnParticle(Particle.SQUID_INK, endLoc, 10, 0.2, 0.2, 0.2, 0.1);
            world.spawnParticle(Particle.EXPLOSION, endLoc, 1);
            world.playSound(endLoc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1f, 0.5f);

            // Shrapnel Cone Logic
            spawnShrapnel(target, dir, finalDamage);

            if (mult > 0.85) {
                world.playSound(eye, Sound.BLOCK_IRON_DOOR_CLOSE, 1f, 0.5f);
                world.playSound(eye, Sound.ENTITY_IRON_GOLEM_HURT, 1f, 0.8f);
            }
        }

        // 3. Recoil Mechanic
        Vector recoil = dir.clone().multiply(-RECOIL_VELOCITY);
        p.setVelocity(p.getVelocity().add(recoil.setY(Math.max(0.2, recoil.getY()))));

        // 4. Visuals (Tracer/Muzzle)
        playEffects(eye, endLoc);

        return AbilityResponse.consume();
    }

    private void spawnShrapnel(LivingEntity target, Vector direction, double originalDamage) {
        Location center = target.getLocation().add(0, 1, 0);
        double shrapnelDmg = originalDamage * SHRAPNEL_DAMAGE_PERCENT;

        for (LivingEntity nearby : target.getWorld().getNearbyLivingEntities(center, SHRAPNEL_RANGE)) {
            if (nearby.equals(target) || nearby instanceof Player) continue;

            Vector toNearby = nearby.getLocation().toVector().subtract(center.toVector()).normalize();
            double angle = Math.toDegrees(direction.angle(toNearby));

            if (angle < SHRAPNEL_CONE_ANGLE / 2.0) {
                DamageUtils.damage(nearby, smpPlayer.getBukkitPlayer(), shrapnelDmg, new DamageEvent.Metadata(ID, DamageType.PROJECTILE_ABILITY));
                nearby.getWorld().spawnParticle(Particle.CRIT, nearby.getLocation().add(0, 1, 0), 3, 0.2, 0.2, 0.2, 0.1);
            }
        }

        // Shrapnel Visual Cone
        for (int i = -2; i <= 2; i++) {
            Vector spreadDir = VectorUtils.rotateYAxis(direction.clone(), i * (SHRAPNEL_CONE_ANGLE / 4.0)).multiply(SHRAPNEL_RANGE);
            renderSimpleTracer(center, center.clone().add(spreadDir), Particle.DUST, DARK_IRON);
        }
    }

    private void playEffects(Location start, Location end) {
        World world = start.getWorld();
        world.playSound(start, Sound.ENTITY_GENERIC_EXPLODE, 1f, 2f);
        world.playSound(start, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.2f, 0.6f);

        // Muzzle Smoke
        world.spawnParticle(Particle.SMOKE, start.clone().add(start.getDirection()), 20, 0.3, 0.3, 0.3, 0.05);

        // Bullet Tracer
        renderSimpleTracer(start.clone().add(start.getDirection()), end, Particle.DUST, LIGHT_IRON);
        renderSimpleTracer(start.clone().add(start.getDirection()), end, Particle.SMOKE, null);
    }

    private void renderSimpleTracer(Location start, Location end, Particle particle, Object data) {
        double dist = start.distance(end);
        Vector dir = end.toVector().subtract(start.toVector()).normalize();
        for (double i = 0; i < dist; i += 0.5) {
            Location point = start.clone().add(dir.clone().multiply(i));
            if (data != null) {
                point.getWorld().spawnParticle(particle, point, 1, 0, 0, 0, 0, data);
            } else {
                point.getWorld().spawnParticle(particle, point, 1, 0.05, 0.05, 0.05, 0.01);
            }
        }
    }

    @Override public @NotNull AbilityInfo<?> getAbilityInfo() { return INFO; }
}
