package com.roguesmp.player.ability.impl.active;

import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent;
import com.roguesmp.RogueSmpCore;
import com.roguesmp.constant.DamageType;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.ability.Ability;
import com.roguesmp.player.ability.AbilityInfo;
import com.roguesmp.utils.DamageUtils;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

public class Pyroblast extends Ability {
    public static final String ID = "pyroblast";

    // Cached Attributes
    private final double damage;
    private final double radius;
    private final int cooldown;

    // Mechanics
    private static final int FIRE_DURATION_TICKS = 80; // 4 seconds

    // Map to track active Pyroblast projectiles fired by this player
    private final Set<Projectile> activeProjectiles = Collections.newSetFromMap(new WeakHashMap<>());

    public static final AbilityInfo<Pyroblast> INFO = new AbilityInfo<>(
            ID,
            Pyroblast.class,
            Pyroblast::new
    );

    public Pyroblast(SmpPlayer player, int level) {
        super(player, level);
        this.damage = getAbilityInfo().getAttributeForLevel("damage", level);
        this.radius = getAbilityInfo().getAttributeForLevel("radius", level);
        this.cooldown = (int) getAbilityInfo().getAttributeForLevel("cooldown", level);
    }

    @Override
    public void onProjectileLaunch(PlayerLaunchProjectileEvent event) {
        if (isOnCooldown() || !event.getPlayer().isSneaking()) return;

        Projectile proj = event.getProjectile();
        castPyroblast(proj);
    }

    @Override
    public void onShootArrow(EntityShootBowEvent event) {
        if (isOnCooldown() || !event.getEntity().isSneaking()) return;

        Projectile proj = (Projectile) event.getProjectile();
        castPyroblast(proj);
    }

    private void castPyroblast(Projectile proj) {
        setCooldownTick(cooldown);
        Player p = smpPlayer.getBukkitPlayer();
        activeProjectiles.add(proj);

        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1f, 1.4f);

        // Visual Trail Task
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (proj.isDead() || proj.isOnGround() || !activeProjectiles.contains(proj) || ticks > 100) {
                    if (!proj.isDead() && (proj.isOnGround() || ticks > 100)) {
                        explode(proj.getLocation(), proj);
                    }
                    this.cancel();
                    return;
                }

                World world = proj.getWorld();
                world.spawnParticle(Particle.SOUL_FIRE_FLAME, proj.getLocation(), 2, 0.05, 0.05, 0.05, 0.02);
                world.spawnParticle(Particle.LARGE_SMOKE, proj.getLocation(), 1, 0, 0, 0, 0.01);

                ticks++;
            }
        }.runTaskTimer(RogueSmpCore.getInstance(), 0, 1);
    }

    @Override
    public void onProjectileHit(ProjectileHitEvent event) {
        if (activeProjectiles.contains(event.getEntity())) {
            explode(event.getEntity().getLocation(), event.getEntity());
        }
    }

    private void explode(Location loc, Projectile proj) {
        if (!activeProjectiles.remove(proj)) return;

        World world = loc.getWorld();
        Player p = smpPlayer.getBukkitPlayer();

        // Visuals
        world.spawnParticle(Particle.EXPLOSION_EMITTER, loc, 1);
        world.spawnParticle(Particle.FLAME, loc, 40, radius/2, radius/2, radius/2, 0.1);
        world.spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 40, radius/2, radius/2, radius/2, 0.1);
        world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);

        // Damage logic
        for (Entity e : loc.getNearbyEntities(radius, radius, radius)) {
            if (e instanceof LivingEntity victim && !(e instanceof Player)) {
                DamageUtils.damage(victim, p, damage, new DamageEvent.Metadata(ID, DamageType.MAGIC));
                victim.setFireTicks(FIRE_DURATION_TICKS);
            }
        }

        proj.remove();
    }

    @Override public @NotNull AbilityInfo<?> getAbilityInfo() { return INFO; }
}
