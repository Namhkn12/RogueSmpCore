package com.roguesmp.entity.boss.hellknight;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.constant.DamageType;
import com.roguesmp.entity.spell.Spell;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.utils.DamageUtils;
import com.roguesmp.utils.PlayerUtils;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public class PeriodicLightningStrike extends Spell {

    private final HellKnight boss;
    private final double damage;
    private final int periodTicks; // Khoảng cách giữa các lần chọn mục tiêu (vd: 100 ticks)
    private final int warningTicks; // Thời gian vòng tròn xuất hiện trước khi sét đánh (vd: 20 ticks)
    private final double radius;
    private int currentCooldown;

    public PeriodicLightningStrike(HellKnight boss, double damage, int periodTicks, int warningTicks, double radius) {
        this.boss = boss;
        this.damage = damage;
        this.periodTicks = periodTicks;
        this.warningTicks = warningTicks;
        this.radius = radius;
    }

    @Override
    public int cooldownTicks() {
        return 0; // Kỹ năng bị động chạy liên tục
    }

    @Override
    public void run(int interval) {
        currentCooldown = currentCooldown + interval;
        if (currentCooldown < periodTicks) return;
        currentCooldown = 0;

        LivingEntity caster = boss.getEntity();
        List<Player> targets = boss.getParticipants();

        if (targets.isEmpty()) return;

        // Triệu hồi sét cho TẤT CẢ người chơi hoặc chọn ngẫu nhiên 1 người
        for (Player target : targets) {
            createLightningStrike(caster, target);
        }
    }

    private void createLightningStrike(LivingEntity caster, Player target) {
        new BukkitRunnable() {
            int timer = 0;
            // Lưu lại vị trí ban đầu của người chơi để đánh sét (không đuổi theo nếu họ chạy thoát)
            final Location strikeLoc = target.getLocation();

            @Override
            public void run() {
                if (!caster.isValid() || boss.dead || timer > warningTicks) {
                    // Khi hết thời gian cảnh báo -> ĐÁNH SÉT
                    if (timer > warningTicks) {
                        executeStrike(caster, strikeLoc);
                    }
                    this.cancel();
                    return;
                }

                // VẼ VÒNG TRÒN CẢNH BÁO
                drawWarningCircle(strikeLoc, timer);
                strikeLoc.getWorld().playSound(strikeLoc, Sound.BLOCK_END_PORTAL_FRAME_FILL, 1.5f, 0.7f);

                // Hiệu ứng âm thanh tích điện nhỏ
                if (timer % 5 == 0) {
                    strikeLoc.getWorld().playSound(strikeLoc, Sound.BLOCK_NOTE_BLOCK_BIT, 0.5f, 1.5f);
                }

                timer++;
            }
        }.runTaskTimer(RogueSmpCore.getInstance(), 0L, 1L);
    }

    private void drawWarningCircle(Location loc, int ticks) {
        // Hiệu ứng vòng tròn xoay hoặc nhấp nháy đỏ
        for (double i = 0; i < Math.PI * 2; i += Math.PI / 8) {
            double x = Math.cos(i) * radius;
            double z = Math.sin(i) * radius;
            Location particleLoc = loc.clone().add(x, 0.1, z);

            // Màu sắc hạt trở nên dày đặc hơn khi sắp đến lúc đánh sét
            loc.getWorld().spawnParticle(Particle.DUST, particleLoc, 1, 0, 0, 0, 0,
                    new Particle.DustOptions(Color.RED, 1.2f));

            if (ticks > warningTicks - 5) {
                loc.getWorld().spawnParticle(Particle.FLASH, particleLoc, 1, 0, 0, 0, Color.RED);
            }
        }
    }

    private void executeStrike(LivingEntity caster, Location loc) {
        World world = loc.getWorld();

        world.strikeLightningEffect(loc);
        world.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.5f);
        world.spawnParticle(Particle.EXPLOSION, loc.add(0, 1, 0), 1);

        // Kiểm tra sát thương lên những người chơi đứng trong vòng tròn
        for (Player e : PlayerUtils.playersInRange(loc, radius, true)) {

            DamageUtils.damage(e, caster, damage, new DamageEvent.Metadata("lightning_passive", null, DamageType.MAGIC, true));
            // Thêm hiệu ứng choáng nhẹ hoặc giật điện
            e.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20, 1));

        }
    }
}
