package com.roguesmp.entity.boss.hellknight.minion;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.constant.DamageType;
import com.roguesmp.entity.SmpEntity;
import com.roguesmp.entity.spell.Spell;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.utils.DamageUtils;
import com.roguesmp.utils.Hitbox;
import com.roguesmp.utils.MovementUtils;
import com.roguesmp.utils.PlayerUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LineChargeSpell extends Spell {

    private final SmpEntity boss;
    private final double damage;
    private final double speed; // Tốc độ lao lên mỗi tick
    private final double range;
    private final double hitRadius;
    private final int delay;
    private final int cooldown;

    public LineChargeSpell(SmpEntity boss, double damage, double speed, double range, double hitRadius, int delay, int cooldown) {
        this.boss = boss;
        this.damage = damage;
        this.speed = speed;
        this.range = range;
        this.hitRadius = hitRadius;
        this.delay = delay;
        this.cooldown = cooldown;
    }

    @Override
    public int cooldownTicks() {
        return cooldown; // 10 giây cooldown
    }

    @Override
    public void run(int interval) {
        LivingEntity caster = boss.getEntity();
        Player target = PlayerUtils.playersInRange(caster.getLocation(), range, false).stream().findAny().orElse(null);

        if (target == null) return;

        // 1. Khóa mục tiêu và xác định hướng lao
        Location startLoc = caster.getLocation();
        // Lấy vị trí mục tiêu tại thời điểm cast để người chơi có thể né
        Location targetLoc = target.getLocation();
        Vector direction = targetLoc.toVector().subtract(startLoc.toVector()).setY(0).normalize();
        double distance = Math.min(startLoc.distance(targetLoc) + 4, 15); // Lao quá mục tiêu một chút

        // 2. Giai đoạn hiển thị đường đi (Telegraphing)
        BukkitRunnable prepareTask = new BukkitRunnable() {
            int timer = 0;

            @Override
            public void run() {
                if (!caster.isValid() || boss.dead) {
                    this.cancel();
                    return;
                }

                // Vẽ đường thẳng chỉ báo đường đi của Boss
                drawChargePath(startLoc, direction, distance);

                if (timer == 0) {
                    caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1f, 1.5f);
                    caster.setAI(false); // Đứng yên gồng
                }

                if (timer >= delay) {
                    caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 1.5f);
                    executeCharge(caster, direction, distance);
                    this.cancel();
                    return;
                }
                timer++;
            }
        };
        prepareTask.runTaskTimer(RogueSmpCore.getInstance(), 0L, 1L);
        activeRunnables.add(prepareTask);
    }

    private void drawChargePath(Location start, Vector dir, double dist) {
        for (double i = 0; i < dist; i += 0.5) {
            Location point = start.clone().add(dir.clone().multiply(i)).add(0, 0.2, 0);
            // Hạt bụi đỏ hoặc lửa để chỉ báo đường đi
            point.getWorld().spawnParticle(Particle.DUST, point, 1, 0, 0, 0, 0,
                    new Particle.DustOptions(Color.RED, 1.5f));
        }
    }

    private void executeCharge(LivingEntity caster, Vector dir, double maxDist) {
        BukkitRunnable chargeTask = new BukkitRunnable() {
            double traveled = 0;
            final List<UUID> hitTargets = new ArrayList<>();

            @Override
            public void run() {
                if (!caster.isValid() || traveled >= maxDist) {
                    caster.setAI(true);
                    this.cancel();
                    return;
                }

                // Di chuyển Boss
                Location nextLoc = caster.getLocation().add(dir.clone().multiply(speed));

                // Kiểm tra va chạm tường
                if (nextLoc.getBlock().getType().isSolid()) {
                    caster.setAI(true);
                    this.cancel();
                    return;
                }

                caster.teleport(nextLoc);
                traveled += speed;

                // Hiệu ứng hạt khi đang lao
                caster.getWorld().spawnParticle(Particle.LARGE_SMOKE, caster.getLocation().add(0, 1, 0), 5, 0.2, 0.2, 0.2, 0.05);
                caster.getWorld().spawnParticle(Particle.SWEEP_ATTACK, caster.getLocation().add(0, 1, 0), 1);

                // Xử lý gây sát thương
                Hitbox hb = new Hitbox.SphereHitbox(caster.getLocation().add(0, 1, 0), hitRadius);
                for (Player p : hb.getHitPlayers(true)) {
                    if (!hitTargets.contains(p.getUniqueId())) {
                        DamageUtils.damage(p, caster, damage, new DamageEvent.Metadata("charge", null, DamageType.MELEE, true));

                        // Đẩy lùi người chơi sang hai bên hoặc ra xa
                        Vector knockback = p.getLocation().toVector().subtract(caster.getLocation().toVector()).normalize().multiply(1.2).setY(0.4);
                        MovementUtils.knockAwayDirection(knockback, p, 0f);

                        hitTargets.add(p.getUniqueId());
                    }
                }
            }
        };
        chargeTask.runTaskTimer(RogueSmpCore.getInstance(), 0L, 1L);
        activeRunnables.add(chargeTask);
    }
}
