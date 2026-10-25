package com.roguesmp.entity.boss.hollowheart;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.entity.EntityManager;
import com.roguesmp.entity.SmpEntity;
import com.roguesmp.entity.spell.common.SpellTargets;
import com.roguesmp.utils.EntityUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public final class Absorption {

    private static final double HEAL_CAP_PERCENT = 0.10;
    private static final int TRAIL_POINTS = 12;

    private Absorption() {
    }

    public static List<LivingEntity> alive(LivingEntity crystal, List<UUID> tracked) {
        return tracked.stream()
                .map(uuid -> crystal.getServer().getEntity(uuid))
                .filter(entity -> entity instanceof LivingEntity living && living.isValid() && !living.isDead())
                .map(LivingEntity.class::cast)
                .toList();
    }

    public static @Nullable LivingEntity spawn(String id, Location location, @Nullable EntityType fallback) {
        SmpEntity smpEntity = EntityManager.getInstance().spawnEntity(id, location);
        if (smpEntity != null) return smpEntity.getEntity();
        if (fallback == null) {
            RogueSmpCore.LOGGER.warn("Entity '{}' is not defined, the Hollowheart fight cannot spawn it", id);
            return null;
        }
        return location.getWorld().spawnEntity(location, fallback) instanceof LivingEntity living ? living : null;
    }

    public static void absorb(LivingEntity crystal, List<LivingEntity> survivors, double maxPercentEach, double multiplier) {
        double percent = 0;
        for (LivingEntity survivor : survivors) {
            double maxHealth = EntityUtils.getMaxHealth(survivor);
            double fraction = maxHealth <= 0 ? 1 : survivor.getHealth() / maxHealth;
            percent += maxPercentEach * fraction;
            trail(SpellTargets.centerOf(survivor), SpellTargets.centerOf(crystal), Particle.HAPPY_VILLAGER);
            remove(survivor);
        }
        heal(crystal, Math.min(percent * multiplier, HEAL_CAP_PERCENT * multiplier));
    }

    public static void heal(LivingEntity crystal, double percent) {
        if (percent <= 0 || !crystal.isValid()) return;
        double maxHealth = EntityUtils.getMaxHealth(crystal);
        crystal.setHealth(Math.min(maxHealth, crystal.getHealth() + maxHealth * percent));
        crystal.getWorld().spawnParticle(Particle.HEART, crystal.getEyeLocation(), 4, 0.5, 0.4, 0.5);
        crystal.getWorld().playSound(crystal.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.5f, 1.2f);
    }

    public static void trail(Location from, Location to, Particle particle) {
        Vector step = to.toVector().subtract(from.toVector()).multiply(1.0 / TRAIL_POINTS);
        Location point = from.clone();
        for (int i = 0; i < TRAIL_POINTS; i++) {
            point.getWorld().spawnParticle(particle, point.add(step), 1, 0, 0, 0, 0);
        }
    }

    public static void remove(Entity entity) {
        entity.getScheduler().run(RogueSmpCore.getInstance(), task -> entity.remove(), null);
    }
}
