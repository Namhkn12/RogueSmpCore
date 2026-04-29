package com.roguesmp.entity.boss.hellknight;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.entity.spell.Spell;
import com.roguesmp.utils.Utils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class TeleportBehindSpell extends Spell {

    private final HellKnight boss;
    private final double distanceBehind; // Khoảng cách đứng sau lưng (thường là 1.5 - 2.0)
    private final int delay;
    private int currentCooldown;

    public TeleportBehindSpell(HellKnight boss, double distanceBehind, int delay) {
        this.boss = boss;
        this.distanceBehind = distanceBehind;
        this.delay = delay;
    }

    @Override
    public int cooldownTicks() {
        return 300;
    }

    @Override
    public void run(int interval) {
        currentCooldown = currentCooldown + interval;
        if (currentCooldown < cooldownTicks()) return;
        currentCooldown = 0;
        LivingEntity entity = boss.getEntity();
        Player target = boss.getParticipants().stream().findAny().orElse(null);

        if (target == null) return;
        target.playSound(target.getLocation(), Sound.ENTITY_WITCH_AMBIENT, 1f, 0.5f);
        target.sendMessage(Utils.fromString("<b><red>Cẩn thận phía sau!"));

        BukkitRunnable tpTask = new BukkitRunnable() {

            int count = 0;
            @Override
            public void run() {
                if (!entity.isValid() || boss.dead || !target.isOnline()) {
                    cleanup();
                    return;
                }

                // Hiệu ứng hạt đen hội tụ
                playShadowParticles(entity.getLocation());

                if (count >= delay) {
                    target.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 10, 0));
                    teleportBehind(entity, target);
                    cleanup();
                    return;
                }
                count++;
            }

            private void cleanup() {
                activeRunnables.remove(this);
                this.cancel();
            }
        };

        tpTask.runTaskTimer(RogueSmpCore.getInstance(), 0L, 1L);
        activeRunnables.add(tpTask);
    }

    private void teleportBehind(LivingEntity bossEntity, Player target) {
        // Lấy vector hướng nhìn của người chơi (chỉ lấy mặt phẳng XZ để tránh boss bị lún đất/bay lên trời)
        Vector direction = target.getLocation().getDirection();
        direction.setY(0).normalize();

        // Tính toán vị trí mới: Lấy vị trí target trừ đi hướng nhìn * khoảng cách
        Location tpLoc = target.getLocation().subtract(direction.multiply(distanceBehind));

        // Điều chỉnh hướng nhìn của Boss để đối diện với lưng người chơi
        tpLoc.setDirection(direction);

        // Kiểm tra an toàn (tránh TP vào trong block)
        if (tpLoc.getBlock().getType().isSolid()) {
            tpLoc.add(0, 1, 0);
        }

        // Thực hiện dịch chuyển
        bossEntity.teleport(tpLoc);

        // Hiệu ứng sau khi TP
        playTeleportEffects(tpLoc);

    }

    private void playShadowParticles(Location loc) {
        loc.getWorld().spawnParticle(Particle.LARGE_SMOKE, loc.add(0, 1, 0), 5, 0.3, 0.5, 0.3, 0.01);
    }

    private void playTeleportEffects(Location loc) {
        loc.getWorld().playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        loc.getWorld().playSound(loc, Sound.ENTITY_WITHER_SHOOT, 0.5f, 0.5f);
    }
}
