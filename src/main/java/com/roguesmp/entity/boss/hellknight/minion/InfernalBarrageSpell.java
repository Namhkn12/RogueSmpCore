package com.roguesmp.entity.boss.hellknight.minion;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.constant.DamageType;
import com.roguesmp.entity.SmpEntity;
import com.roguesmp.entity.spell.Spell;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.utils.DamageUtils;
import com.roguesmp.utils.Hitbox;
import com.roguesmp.utils.PlayerUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.List;

public class InfernalBarrageSpell extends Spell {

    private final SmpEntity boss;
    private final double damage;
    private final int totalProjectiles; // Tổng số đạn bắn ra
    private final int delayBetweenShots; // Khoảng cách giữa các phát bắn (ticks)
    private final double range;

    public InfernalBarrageSpell(SmpEntity boss, double damage, int totalProjectiles, int delayBetweenShots, double range) {
        this.boss = boss;
        this.damage = damage;
        this.totalProjectiles = totalProjectiles;
        this.delayBetweenShots = delayBetweenShots;
        this.range = range;
    }

    @Override
    public int cooldownTicks() {
        return 300;
    }

    @Override
    public void run(int interval) {
        LivingEntity caster = boss.getEntity();

        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 2f, 0.5f);

        new BukkitRunnable() {
            int shotsFired = 0;
            final List<Player> players = PlayerUtils.playersInRange(caster.getLocation(), range, false);
            @Override
            public void run() {
                if (!caster.isValid() || boss.dead || shotsFired >= totalProjectiles) {
                    this.cancel();
                    return;
                }

                Player target = players.stream().findAny().orElse(null);
                if (target != null) {
                    fireBolt(caster, target);
                }

                shotsFired++;
            }
        }.runTaskTimer(RogueSmpCore.getInstance(), 20L, delayBetweenShots);
    }

    private void fireBolt(LivingEntity caster, Player target) {
        Location start = caster.getEyeLocation().subtract(0, 0.5, 0);

        Vector direction = target.getLocation().add(0, 1, 0).toVector().subtract(start.toVector()).normalize();

        // TẠO ĐỘ LỆCH NGẪU NHIÊN (Random Accuracy)
        direction.add(new Vector(
                (Math.random() - 0.5) * 1.2,
                (Math.random() - 0.5) * 1.2,
                (Math.random() - 0.5) * 1.2
        )).normalize();

        caster.getWorld().playSound(start, Sound.ENTITY_BLAZE_SHOOT, 1f, 1.5f);

        // Task điều khiển viên đạn bay
        BukkitRunnable task = new BukkitRunnable() {
            final Location current = start.clone();
            int life = 0;

            @Override
            public void run() {
                // Di chuyển đạn 0.8 block mỗi tick
                current.add(direction.clone().multiply(0.8));

                // VFX cho viên đạn (Lửa linh hồn hoặc Lửa thường)
                current.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, current, 3, 0.05, 0.05, 0.05, 0.02);
                current.getWorld().spawnParticle(Particle.SOUL, current, 1, 0, 0, 0, 0);

                // Kiểm tra va chạm với Block
                if (current.getBlock().getType().isSolid()) {
                    this.cancel();
                    return;
                }

                // Kiểm tra va chạm với Player
                Hitbox hb = new Hitbox.SphereHitbox(current, 0.5);
                for (Player p : hb.getHitPlayers(true)) {
                    DamageUtils.damage(p, caster, damage, new DamageEvent.Metadata("barrage", null, DamageType.MAGIC, true));
                    this.cancel();
                    return;
                }

                if (life++ > 60) this.cancel();
            }
        };
        task.runTaskTimer(RogueSmpCore.getInstance(), 0L, 1L);
        activeRunnables.add(task);
    }
}