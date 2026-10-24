package com.roguesmp.entity.boss.hollowheart;

import com.roguesmp.constant.DamageType;
import com.roguesmp.entity.spell.common.SpellTargets;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.utils.DamageUtils;
import com.roguesmp.utils.EntityUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class ShockwaveRing {

    public static final String ID = "crystal_pulse";

    private static final double SPEED_PER_TICK = 0.4;
    private static final double THICKNESS = 0.9;
    private static final double MAX_HIT_HEIGHT = 0.6;
    private static final double DAMAGE_MULTIPLIER = 0.6;
    private static final double PARTICLE_SPACING = 0.6;

    private final LivingEntity crystal;
    private final Arena arena;
    private final double maxRadius;
    private final Set<UUID> hit = new HashSet<>();
    private double radius = 0.5;

    public ShockwaveRing(LivingEntity crystal, Arena arena) {
        this.crystal = crystal;
        this.arena = arena;
        this.maxRadius = arena.radius();
        Location center = arena.center();
        center.getWorld().playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 2f, 1.2f);
    }

    public static void chargeEffect(LivingEntity crystal, long elapsed, long total) {
        Location center = SpellTargets.centerOf(crystal);
        crystal.getWorld().spawnParticle(Particle.ENCHANT, center, 12, 1.5, 1.0, 1.5, 0.6);
        if (elapsed % 10 == 0) {
            float pitch = 0.5f + 1.5f * Math.min(1f, (float) elapsed / Math.max(1, total));
            crystal.getWorld().playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 2f, pitch);
        }
    }

    public boolean tick(int interval) {
        radius += SPEED_PER_TICK * interval;
        Location center = arena.center();
        draw(center);

        double damage = EntityUtils.getAttributeOrDefault(crystal, Attribute.ATTACK_DAMAGE, 0) * DAMAGE_MULTIPLIER;
        for (Player player : arena.players()) {
            if (hit.contains(player.getUniqueId())) continue;
            double distance = SpellTargets.horizontalDistance(player.getLocation(), center);
            if (distance > radius || distance < radius - THICKNESS) continue;
            if (!isGrounded(player)) continue;

            hit.add(player.getUniqueId());
            DamageEvent.Metadata metadata = DamageEvent.Metadata.spell(DamageType.MAGIC, ID);
            metadata.setIgnoreIframe(true);
            metadata.setDoKnockback(false);
            DamageUtils.damage(player, crystal, damage, metadata);
            Vector push = SpellTargets.horizontalDirection(center, player.getLocation()).multiply(0.8).setY(0.4);
            player.setVelocity(player.getVelocity().add(push));
        }
        return radius >= maxRadius;
    }

    private boolean isGrounded(Player player) {
        Location floor = arena.floorAt(player.getX(), player.getZ());
        double floorY = floor == null ? player.getLocation().getBlockY() : floor.getY();
        return player.getY() - floorY <= MAX_HIT_HEIGHT;
    }

    private void draw(Location center) {
        int points = Math.max(12, (int) (Math.PI * 2 * radius / PARTICLE_SPACING));
        double y = center.getY() + 0.2;
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2 * i / points;
            center.getWorld().spawnParticle(Particle.END_ROD,
                    center.getX() + Math.cos(angle) * radius, y, center.getZ() + Math.sin(angle) * radius, 1, 0, 0, 0, 0);
        }
    }
}
