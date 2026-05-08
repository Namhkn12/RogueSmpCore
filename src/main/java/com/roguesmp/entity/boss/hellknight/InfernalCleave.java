package com.roguesmp.entity.boss.hellknight;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.constant.DamageType;
import com.roguesmp.entity.spell.ChargeUpManager;
import com.roguesmp.entity.spell.Spell;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.utils.DamageUtils;
import com.roguesmp.utils.Hitbox;
import com.roguesmp.utils.Utils;
import com.roguesmp.utils.VectorUtils;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class InfernalCleave extends Spell {
    private final HellKnight boss;
    private final double damage;
    private final double fanRadius;   // Độ xa của nhát chém
    private final double fanAngle;
    private final int chargeTime;
    private final ChargeUpManager charge;

    public InfernalCleave(HellKnight boss, double damage, double fanRadius, double fanAngle, int chargeTime) {
        this.boss = boss;
        this.damage = damage;
        this.fanRadius = fanRadius;
        this.fanAngle = fanAngle;
        this.charge = new ChargeUpManager(boss.getEntity(), chargeTime, Utils.fromString("<red>TRẢM QUYẾT..."),
                BossBar.Color.RED, BossBar.Overlay.NOTCHED_6, 50);
        this.chargeTime = chargeTime;
    }

    @Override
    public int cooldownTicks() { return 160; }

    @Override
    public void run(int interval) {
        LivingEntity caster = boss.getEntity();
        caster.setAI(false);

        // 1. Tạo ItemDisplay Thanh kiếm khổng lồ

        // 2. ChargeUpManager

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!caster.isValid() || boss.dead) {
                    charge.remove(); this.cancel();
                    return;
                }

                if (charge.getTime() % 2 == 0) {
                    drawChargeOutline(caster.getLocation(), caster.getLocation().getDirection().setY(0).normalize());
                }

                if (charge.nextTick()) {
                    caster.setAI(true);
                    executeCleave(caster);
                    charge.reset();
                    this.cancel();
                    return;
                }

            }
        }.runTaskTimer(RogueSmpCore.getInstance(), 0L, 1L);
    }

    private void executeCleave(LivingEntity caster) {
        Location center = caster.getLocation();
        Vector direction = center.getDirection().setY(0).normalize();

        int stepDegrees = 10;     // Độ chi tiết (mỗi 10 độ vẽ một hàng hạt)

        caster.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2f, 0.5f);
        caster.getWorld().playSound(center, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1.5f, 0.5f);

        // Quét theo góc
        for (double currentAngle = -fanAngle / 2; currentAngle <= fanAngle / 2; currentAngle += stepDegrees) {

            // Tính toán hướng của tia dựa trên góc xoay
            Vector rayDir = VectorUtils.rotateYAxis(direction.clone(), currentAngle);

            // Quét dọc theo chiều dài của tia
            for (double d = 1; d <= fanRadius; d += 1.5) {
                Location loc = center.clone().add(rayDir.clone().multiply(d));

                // VFX: Hiệu ứng nổ và lửa lan tỏa theo hình quạt
                loc.getWorld().spawnParticle(Particle.EXPLOSION, loc, 1, 0.1, 0, 0.1, 0);
                loc.getWorld().spawnParticle(Particle.LAVA, loc, 3, 0.3, 0.1, 0.3, 0.05);
                loc.getWorld().spawnParticle(Particle.FLAME, loc, 5, 0.5, 0.2, 0.5, 0.02);

                // Kiểm tra sát thương tại điểm này
                Hitbox hb = new Hitbox.SphereHitbox(loc, 1.8);
                hb.getHitPlayers(true).forEach(p -> {
                    // Tránh gây damage nhiều lần cho một người trong cùng một chiêu
                    // Metadata giúp hệ thống damage nhận diện chiêu thức
                    DamageUtils.damage(p, caster, damage, new DamageEvent.Metadata("infernal_fan", null, DamageType.MELEE, false));

                    // Lực hất vung: Hất ra xa tâm và hất lên cao
                    Vector knockback = p.getLocation().toVector().subtract(center.toVector()).normalize().multiply(0.8).setY(0.5);
                    p.setVelocity(knockback);
                });
            }
        }

        // Thêm một vòng tròn lửa lớn ở rìa để xác định rõ phạm vi
        drawFanOutline(center, direction, fanRadius, fanAngle);
    }

    /**
     * Vẽ viền hình quạt để người chơi dễ nhìn thấy hitbox
     */
    private void drawFanOutline(Location center, Vector dir, double radius, double angle) {
        for (double i = -angle / 2; i <= angle / 2; i += 2) {
            Vector v = VectorUtils.rotateYAxis(dir.clone(), i).multiply(radius);
            center.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, center.clone().add(v), 1, 0, 0, 0, 0);
        }
    }

    private void drawChargeOutline(Location center, Vector dir) {
        double angleRad = Math.toRadians(fanAngle);
        // Draw the two side lines of the fan
        for (double d = 0; d < fanRadius; d += 1.0) {
            Vector left = VectorUtils.rotateYAxis(dir.clone(), -fanAngle / 2).multiply(d);
            Vector right = VectorUtils.rotateYAxis(dir.clone(), fanAngle / 2).multiply(d);

            center.getWorld().spawnParticle(Particle.FLAME, center.clone().add(left), 1, 0, 0, 0, 0);
            center.getWorld().spawnParticle(Particle.FLAME, center.clone().add(right), 1, 0, 0, 0, 0);
        }
        // Draw the outer arc
        for (double a = -fanAngle / 2; a <= fanAngle / 2; a += 5) {
            Vector arc = VectorUtils.rotateYAxis(dir.clone(), a).multiply(fanRadius);
            center.getWorld().spawnParticle(Particle.SMALL_FLAME, center.clone().add(arc), 1, 0, 0, 0, 0);
        }
    }
}
