package com.roguesmp.player.ability.impl.active;

import com.destroystokyo.paper.ParticleBuilder;
import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent;
import com.roguesmp.constant.DamageType;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.particle.ParticleShape;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.ability.Ability;
import com.roguesmp.player.ability.AbilityInfo;
import com.roguesmp.utils.DamageUtils;
import com.roguesmp.utils.ParticleUtils;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class Raygun extends Ability {
    public static final String ID = "raygun";

    private final double damage;
    private final double range;
    private final int cooldown;

    public static final AbilityInfo<Raygun> INFO = new AbilityInfo<>(
            ID,
            Raygun.class,
            Raygun::new
    );

    public Raygun(SmpPlayer player, int level) {
        super(player, level);
        this.damage = getAbilityInfo().getAttributeForLevel("damage", level);
        this.range = getAbilityInfo().getAttributeForLevel("range", level);
        this.cooldown = (int) getAbilityInfo().getAttributeForLevel("cooldown", level);
    }

    @Override
    public void onProjectileLaunch(PlayerLaunchProjectileEvent event) {
        Player p = smpPlayer.getBukkitPlayer();
        if (isOnCooldown() || !p.isSneaking()) return;
        event.setCancelled(true);
        castRayGun(p);
    }

    @Override
    public void onShootArrow(EntityShootBowEvent event) {
        Player p = smpPlayer.getBukkitPlayer();
        if (isOnCooldown() || !p.isSneaking()) return;
        event.setCancelled(true);
        castRayGun(p);
    }

    private void castRayGun(Player p) {
        setCooldownTick(cooldown);

        Location start = p.getEyeLocation().subtract(0, 0.2, 0);
        Vector direction = start.getDirection();

        RayTraceResult ray = p.getWorld().rayTrace(
                start,
                direction,
                range,
                FluidCollisionMode.NEVER,
                true,
                0.5,
                e -> !e.equals(p)
        );

        Location end = ray != null
                ? ray.getHitPosition().toLocation(p.getWorld())
                : start.clone().add(direction.clone().multiply(range));

        playElectricLaunchSounds(start);
        spawnLaserBeam(start, end);

        if (ray != null && ray.getHitEntity() instanceof LivingEntity victim && !(victim instanceof Player)) {
            DamageUtils.damage(victim, p, damage, new DamageEvent.Metadata(ID, DamageType.PROJECTILE_ABILITY));
            playImpactEffects(end, victim);
        }
    }

    private void playElectricLaunchSounds(Location loc) {
        World w = loc.getWorld();
        w.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 1.5f);
        w.playSound(loc, Sound.ENTITY_GUARDIAN_ATTACK, 1.0f, 2.0f);
        w.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.4f, 2.0f);
        w.playSound(loc, Sound.ITEM_TRIDENT_RIPTIDE_1, 1.0f, 1.6f);
    }

    private void playImpactEffects(Location loc, Entity victim) {
        World w = loc.getWorld();
        w.spawnParticle(Particle.FLASH, loc, 1, Color.AQUA);
        w.spawnParticle(Particle.ELECTRIC_SPARK, loc, 5, 0.1, 0.1, 0.1, 0.05);
    }

    private void spawnLaserBeam(Location start, Location end) {
        double dist = start.distance(end);
        Vector dir = end.toVector().subtract(start.toVector()).normalize();
        Location cursor = start.clone().setDirection(dir);

        List<ParticleBuilder> builders = List.of(
                new ParticleBuilder(Particle.SOUL_FIRE_FLAME).count(0),
                new ParticleBuilder(Particle.DUST).data(new Particle.DustOptions(Color.AQUA, 0.5f)).count(0)
        );

        double spacing = 0.2;
        double maxBulge = 0.4 + (level * 0.05); // Arcs get slightly wider as level increases

        for (double d = 0; d < dist; d += spacing) {
            double progress = d / dist;
            double offset = Math.sin(progress * Math.PI) * maxBulge;

            double jitter = (Math.random() - 0.5) * 0.04;

            double finalD = d;
            ParticleShape arcs = (tick) -> List.of(
                    new Vector(offset + jitter, offset + jitter, finalD),
                    new Vector(-offset + jitter, offset + jitter, finalD)
            );

            ParticleUtils.spawnShape(cursor, arcs, builders, 0);
        }
    }

    @Override public @NotNull AbilityInfo<?> getAbilityInfo() { return INFO; }
}