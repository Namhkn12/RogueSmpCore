package com.roguesmp.player.ability.impl.shiftprojectile;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.constant.AbilityTrigger;
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

    // Level Scaling
    private static final List<Double> DAMAGE_LEVELS = List.of(20.0, 24.0, 28.0, 32.0, 44.0);
    private static final List<Double> RADIUS_LEVELS = List.of(3.5, 4.0, 4.5, 5.0, 6.0);
    private static final List<Integer> COOLDOWN_LEVELS = List.of(240, 220, 200, 180, 140); // 12s to 7s

    // Mechanics
    private static final int FIRE_DURATION_TICKS = 80; // 4 seconds

    // Map to track active Pyroblast projectiles fired by this player
    private final Set<Projectile> activeProjectiles = Collections.newSetFromMap(new WeakHashMap<>());

    public static final AbilityInfo<Pyroblast> INFO = new AbilityInfo.Builder<Pyroblast>()
            .id(ID)
            .displayText(Component.text("Pyroblast", NamedTextColor.RED, TextDecoration.BOLD))
            .descriptionProvider((p, l) -> List.of(
                    Utils.text("Ngồi xuống khi bắn tên để kích hoạt Pyroblast.", NamedTextColor.GRAY),
                    Utils.text("Mũi tên sẽ nổ tung khi va chạm, gây sát thương phép.", NamedTextColor.DARK_GRAY),
                    Utils.text("Sát thương nổ: ", NamedTextColor.GRAY)
                            .append(Utils.text(Utils.formatDecimal(DAMAGE_LEVELS.get(l - 1)), NamedTextColor.GOLD)),
                    Utils.text("Bán kính nổ: ", NamedTextColor.GRAY)
                            .append(Utils.text(Utils.formatDecimal(RADIUS_LEVELS.get(l - 1)) + "m", NamedTextColor.YELLOW))
            ))
            .displayIcon(Material.TNT_MINECART)
            .factory(Pyroblast::new)
            .trigger(AbilityTrigger.SHIFT_PROJECTILE)
            .build();

    public Pyroblast(SmpPlayer player, int level) {
        super(player, level);
    }

    @Override
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (isOnCooldown() || (event.getEntity().getShooter() instanceof Player player && player.isSneaking())) return;

        Projectile proj = event.getEntity();
        castPyroblast(proj);
    }

    @Override
    public void cast() {
    }

    private void castPyroblast(Projectile proj) {
        setCooldownTick(COOLDOWN_LEVELS.get(level - 1));
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
        double radius = RADIUS_LEVELS.get(level - 1);
        double damage = DAMAGE_LEVELS.get(level - 1);

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

    @Override public @NotNull AbilityInfo<? extends Ability> getAbilityInfo() { return INFO; }
}
