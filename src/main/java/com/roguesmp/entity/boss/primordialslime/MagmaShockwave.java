package com.roguesmp.entity.boss.primordialslime;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.constant.DamageType;
import com.roguesmp.entity.spell.ChargeUpManager;
import com.roguesmp.entity.spell.Spell;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.utils.DamageUtils;
import com.roguesmp.utils.MovementUtils;
import com.roguesmp.utils.PlayerUtils;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class MagmaShockwave extends Spell {
    private final LivingEntity owner;
    private final double damage;
    private final ChargeUpManager chargeUp;

    // Các thông số cấu hình (Configurable Parameters)
    private final double maxRadius;
    private final double expansionSpeed;
    private final double waveHeight;
    private final double hitWindow; // Độ dày của vòng sát thương (inner/outer bound)

    public MagmaShockwave(
            LivingEntity owner,
            double damage,
            int chargeTicks,
            double maxRadius,
            double expansionSpeed,
            double waveHeight,
            double hitWindow
    ) {
        this.owner = owner;
        this.damage = damage;
        this.maxRadius = maxRadius;
        this.expansionSpeed = expansionSpeed;
        this.waveHeight = waveHeight;
        this.hitWindow = hitWindow;

        this.chargeUp = new ChargeUpManager(
                owner,
                chargeTicks,
                Component.text("LÕI NHIỆT ĐANG TÍCH TỤ...", NamedTextColor.RED, TextDecoration.BOLD),
                BossBar.Color.RED,
                BossBar.Overlay.NOTCHED_10,
                (int) maxRadius
        );
    }

    @Override
    public void run() {
        owner.setAI(false);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (owner.isDead() || !owner.isValid()) {
                    chargeUp.remove();
                    this.cancel();
                    return;
                }

                // Hiệu ứng hạt dựa trên tiến trình gồng
                Location loc = owner.getLocation().add(0, 1, 0);
                spawnChargeParticles(loc);

                if (chargeUp.nextTick()) {
                    owner.setAI(true);
                    chargeUp.reset();
                    createTravelingShockwave(owner.getLocation());
                    this.cancel();
                }
            }
        }.runTaskTimer(RogueSmpCore.getInstance(), 0L, 1L);
    }

    private void spawnChargeParticles(Location loc) {
        Vector suction = new Vector(Math.random()-0.5, Math.random()-0.5, Math.random()-0.5).normalize().multiply(2);
        loc.getWorld().spawnParticle(Particle.FLAME, loc.clone().add(suction), 0, -suction.getX(), -suction.getY(), -suction.getZ(), 0.1);
    }

    private void createTravelingShockwave(Location center) {
        center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.5f);
        center.getWorld().playSound(center, Sound.ITEM_FIRECHARGE_USE, 2f, 0.5f);
        center.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, center, 2, 0.5, 0.5, 0.5, 0);

        new BukkitRunnable() {
            double currentRadius = 1.0;
            final Set<UUID> hitPlayers = new HashSet<>();

            @Override
            public void run() {
                if (currentRadius >= maxRadius || owner.isDead()) {
                    this.cancel();
                    return;
                }

                // 1. Vẽ vòng tròn hạt
                drawVisualCircle(center, currentRadius);

                // 2. Tính toán vùng va chạm (Ring-buffer)
                double innerBound = currentRadius - hitWindow;
                double outerBound = currentRadius + hitWindow;

                for (Player p : PlayerUtils.playersInRange(center, outerBound, true)) {
                    if (hitPlayers.contains(p.getUniqueId())) continue;

                    // Kiểm tra chiều cao (Jump-over logic)
                    double relativeY = p.getLocation().getY() - center.getY();
                    if (relativeY > waveHeight || relativeY < -1.5) continue;

                    // Kiểm tra BoundingBox trong mặt phẳng ngang
                    if (isInsideRing(p, center, innerBound, outerBound)) {
                        applyShockwaveEffect(p, center);
                        hitPlayers.add(p.getUniqueId());
                    }
                }
                currentRadius += expansionSpeed;
            }
        }.runTaskTimer(RogueSmpCore.getInstance(), 0L, 1L);
    }

    private void drawVisualCircle(Location center, double radius) {
        int particleCount = (int) (radius * 10);
        for (int i = 0; i < particleCount; i++) {
            double angle = i * (Math.PI * 2 / particleCount);
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            Location pLoc = center.clone().add(x, 0.1, z);
            center.getWorld().spawnParticle(Particle.FLAME, pLoc, 1, 0.02, 0, 0.02, 0.01);
        }
    }

    private boolean isInsideRing(Player p, Location center, double inner, double outer) {
        double distSq = p.getBoundingBox().getCenter().setY(center.getY()).distanceSquared(center.toVector());
        return distSq >= (inner * inner) && distSq <= (outer * outer);
    }

    private void applyShockwaveEffect(Player p, Location center) {
        DamageUtils.damage(p, owner, damage, new DamageEvent.Metadata(DamageType.FIRE));
        p.setFireTicks(60);

        // Hất văng người chơi
        Vector kb = p.getLocation().toVector().subtract(center.toVector()).normalize().multiply(1.5).setY(0.4);
        MovementUtils.knockAwayDirection(kb, p, 0, true);

        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 1f, 0.5f);
    }

    @Override
    public int cooldownTicks() { return 140; }
}
