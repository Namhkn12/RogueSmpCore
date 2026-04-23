package com.roguesmp.entity.boss.primordialslime;

import com.roguesmp.constant.DamageType;
import com.roguesmp.entity.spell.ChargeUpManager;
import com.roguesmp.entity.spell.Spell;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.utils.DamageUtils;
import com.roguesmp.utils.MovementUtils;
import com.roguesmp.utils.PlayerUtils;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class AccumulateDamage extends Spell {
    private final LivingEntity owner;
    private final double aoeDamage;
    private final double damageThreshold;
    private final int internalCooldown;

    private double accumulatedDamage = 0;
    private long lastTriggerTime = 0;
    private ChargeUpManager chargeUp; // Dùng để hiển thị thanh tích lũy

    public AccumulateDamage(LivingEntity owner, double aoeDamage, double damageThreshold, int internalCooldown) {
        this.owner = owner;
        this.aoeDamage = aoeDamage;
        this.damageThreshold = damageThreshold;
        this.internalCooldown = internalCooldown;

        // Khởi tạo ChargeUp với max ticks là threshold (ép kiểu về int để tượng trưng)
        // Chúng ta sẽ tự điều khiển "tick" của nó dựa trên damage.
        this.chargeUp = new ChargeUpManager(
                owner,
                (int) damageThreshold,
                Component.text("NĂNG LƯỢNG MA CHẤT", NamedTextColor.DARK_GREEN, TextDecoration.BOLD),
                BossBar.Color.GREEN,
                BossBar.Overlay.PROGRESS,
                30 // Bán kính người chơi có thể nhìn thấy thanh này
        );
    }

    @Override
    public void run(int interval) {
        // Spell passive
    }

    @Override
    public void onDamage(DamageEvent event) {
        if (!owner.isValid()) return;

        // Chạy sau 1 tick để lấy FinalDamage chính xác
        Utils.runLater(() -> {
            long currentTime = Bukkit.getCurrentTick();

            // Nếu đang trong cooldown nội bộ, không tích lũy thêm (tùy chọn logic của bạn)
            if ((currentTime - lastTriggerTime) < internalCooldown) return;

            accumulatedDamage += event.getFinalDamage();

            // Cập nhật hiển thị BossBar của ChargeUpManager
            updateChargeIndicator();

            if (accumulatedDamage >= damageThreshold) {
                triggerAoeBurst();
                accumulatedDamage = 0;
                lastTriggerTime = currentTime;

                // Reset thanh bossbar về 0 hoặc ẩn đi
                chargeUp.setProgress(0);
                chargeUp.update();
            }
        });
    }

    private void updateChargeIndicator() {
        // 1. Tính toán tiến trình từ 0.0 đến 1.0
        float progress = (float) Math.min(accumulatedDamage / damageThreshold, 1.0);

        // 2. Cập nhật giá trị vào Manager
        chargeUp.setProgress(progress);

        // 3. Gọi update để đảm bảo BossBar hiển thị cho mọi người xung quanh
        chargeUp.update();

        // Visual feedback: Càng gần đầy, hạt xuất hiện càng nhiều
        if (progress > 0.3) {
            owner.getWorld().spawnParticle(
                    Particle.ENTITY_EFFECT,
                    owner.getLocation().add(0, 1.2, 0),
                    (int) (progress * 5),
                    0.4, 0.4, 0.4, 0,
                    Color.fromRGB(50, 200, 50)
            );
        }
    }

    private void triggerAoeBurst() {
        Location loc = owner.getLocation();

        // Hiệu ứng âm thanh khi bùng nổ
        loc.getWorld().playSound(loc, Sound.ENTITY_SLIME_DEATH, 2f, 0.5f);
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.5f);
        loc.getWorld().playSound(loc, Sound.BLOCK_MUD_BREAK, 2f, 0.5f); // Tiếng bùn vỡ

        // Visuals
        loc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, loc, 1);
        loc.getWorld().spawnParticle(Particle.ITEM_SLIME, loc, 150, 3, 1, 3, 0.5);
        loc.getWorld().spawnParticle(Particle.SNEEZE, loc, 80, 4, 1, 4, 0.1);

        // Gây sát thương và Impact
        for (Player p : PlayerUtils.playersInRange(loc, 4, true)) {
            DamageUtils.damage(p, owner, aoeDamage, new DamageEvent.Metadata(DamageType.BLAST));

            // Knockback & Impact Sound
            Vector knockback = p.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(1.2).setY(0.5);
            MovementUtils.knockAwayDirection(knockback, p, 0f, true);

            p.getWorld().playSound(p.getLocation(), Sound.BLOCK_SLIME_BLOCK_STEP, 1.5f, 0.5f);
            p.sendMessage(Component.text("!!! MA CHẤT PHÁT NỔ !!!", NamedTextColor.RED, TextDecoration.BOLD));
        }
        // Reset
        chargeUp.setProgress(0);
        chargeUp.update();
    }

    @Override
    public int cooldownTicks() {
        return 0;
    }
}
