package com.roguesmp.player.ability.impl.shiftleftclick;

import com.roguesmp.constant.AbilityTrigger;
import com.roguesmp.constant.DamageType;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.ability.Ability;
import com.roguesmp.player.ability.AbilityInfo;
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

    // Level Scaling
    private static final List<Double> DAMAGE_LEVELS = List.of(35.0, 43.0, 52.0, 59.0, 68.0);
    private static final List<Double> RANGE_LEVELS = List.of(8.0, 9.0, 10.0, 11.0, 12.0);
    private static final List<Integer> COOLDOWN_LEVELS = List.of(200, 180, 160, 140, 120); // 10s to 6s

    // Mechanics Constants
    private static final double RECOIL_VELOCITY = 1.0;
    private static final double SHRAPNEL_DAMAGE_PERCENT = 0.33;
    private static final double SHRAPNEL_RANGE = 4.0;
    private static final double SHRAPNEL_CONE_ANGLE = 50.0;

    private static final Particle.DustOptions LIGHT_IRON = new Particle.DustOptions(Color.fromRGB(180, 180, 180), 1.0f);
    private static final Particle.DustOptions DARK_IRON = new Particle.DustOptions(Color.fromRGB(100, 100, 100), 1.0f);

    public static final AbilityInfo<Scrapshot> INFO = new AbilityInfo.Builder<Scrapshot>()
            .id(ID)
            .displayText(Component.text("Scrapshot", NamedTextColor.GRAY, TextDecoration.BOLD))
            .descriptionProvider((p, l) -> List.of(
                    Utils.text("Bắn một phát súng hỏa mai từ thép vụn.", NamedTextColor.GRAY),
                    Utils.text("Gây sát thương tối đa ở cự ly gần và giảm dần theo khoảng cách.", NamedTextColor.DARK_GRAY),
                    Utils.text("Sát thương: ", NamedTextColor.GRAY)
                            .append(Utils.text(Utils.formatDecimal(DAMAGE_LEVELS.get(l - 1)), NamedTextColor.YELLOW)),
                    Utils.text("Mảnh vỡ: ", NamedTextColor.GRAY)
                            .append(Utils.text("33% sát thương lan theo hình nón.", NamedTextColor.GOLD))
            ))
            .displayIcon(Material.NETHERITE_SCRAP)
            .factory(Scrapshot::new)
            .trigger(AbilityTrigger.SHIFT_LEFT_CLICK)
            .build();

    public Scrapshot(SmpPlayer player, int level) { super(player, level); }

    @Override
    public void cast() {
        if (isOnCooldown()) return;

        Player p = smpPlayer.getBukkitPlayer();
        Location eye = p.getEyeLocation();
        Vector dir = eye.getDirection();
        World world = p.getWorld();

        double baseDamage = DAMAGE_LEVELS.get(level - 1);
        double range = RANGE_LEVELS.get(level - 1);
        setCooldownTick(COOLDOWN_LEVELS.get(level - 1));

        // 1. Raytrace for Primary Target
        RayTraceResult result = world.rayTrace(eye, dir, range, FluidCollisionMode.NEVER, true, 0.5,
                e -> e instanceof LivingEntity && !(e instanceof Player));

        Location endLoc = (result != null) ? result.getHitPosition().toLocation(world)
                : eye.clone().add(dir.clone().multiply(range));

        // 2. Damage & Shrapnel Logic
        if (result != null && result.getHitEntity() instanceof LivingEntity target) {
            double dist = eye.distance(endLoc);

            // Proximity Multiplier (Closer = More Damage)
            // Starts dropping if distance > 25% of max range
            double mult = Math.min(1.0, (range * 1.25 - dist) / range);
            double finalDamage = baseDamage * mult;

            DamageUtils.damage(target, p, finalDamage, new DamageEvent.Metadata(ID, DamageType.PROJECTILE_ABILITY));

            // Impact Visuals
            world.spawnParticle(Particle.SQUID_INK, endLoc, 10, 0.2, 0.2, 0.2, 0.1);
            world.spawnParticle(Particle.EXPLOSION, endLoc, 1);
            world.playSound(endLoc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1f, 0.5f);

            // Shrapnel Cone Logic
            spawnShrapnel(target, dir, finalDamage);

            if (mult > 0.85) { // Satisfying "Thunk" for point-blank shots
                world.playSound(eye, Sound.BLOCK_IRON_DOOR_CLOSE, 1f, 0.5f);
                world.playSound(eye, Sound.ENTITY_IRON_GOLEM_HURT, 1f, 0.8f);
            }
        }

        // 3. Recoil Mechanic
        Vector recoil = dir.clone().multiply(-RECOIL_VELOCITY);
        p.setVelocity(p.getVelocity().add(recoil.setY(Math.max(0.2, recoil.getY()))));

        // 4. Tracer & Muzzle Visuals
        playEffects(eye, endLoc);
    }

    private void spawnShrapnel(LivingEntity target, Vector direction, double originalDamage) {
        Location center = target.getLocation().add(0, 1, 0);
        double shrapnelDamage = originalDamage * SHRAPNEL_DAMAGE_PERCENT;

        // Hit mobs in a cone behind the target
        for (LivingEntity nearby : target.getWorld().getNearbyLivingEntities(center, SHRAPNEL_RANGE)) {
            if (nearby.equals(target) || nearby instanceof Player) continue;

            Vector toNearby = nearby.getLocation().toVector().subtract(center.toVector()).normalize();
            double angle = Math.toDegrees(direction.angle(toNearby));

            if (angle < SHRAPNEL_CONE_ANGLE / 2.0) {
                DamageUtils.damage(nearby, smpPlayer.getBukkitPlayer(), shrapnelDamage, new DamageEvent.Metadata(ID, DamageType.PROJECTILE_ABILITY));
                nearby.getWorld().spawnParticle(Particle.CRIT, nearby.getLocation().add(0, 1, 0), 3, 0.2, 0.2, 0.2, 0.1);
            }
        }

        // Shrapnel "Cone" Lines Visual
        for (int i = -2; i <= 2; i++) {
            Vector spreadDir = VectorUtils.rotateYAxis(direction.clone(), i * (SHRAPNEL_CONE_ANGLE / 4.0)).multiply(SHRAPNEL_RANGE);
            Location shrapnelEnd = center.clone().add(spreadDir);
            renderSimpleTracer(center, shrapnelEnd, Particle.DUST, DARK_IRON);
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

    @Override public @NotNull AbilityInfo<? extends Ability> getAbilityInfo() { return INFO; }
}
