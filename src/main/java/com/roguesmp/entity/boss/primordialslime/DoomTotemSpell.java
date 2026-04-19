package com.roguesmp.entity.boss.primordialslime;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.constant.DamageType;
import com.roguesmp.entity.boss.PrimordialSlime;
import com.roguesmp.entity.spell.ChargeUpManager;
import com.roguesmp.entity.spell.Spell;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.utils.DamageUtils;
import com.roguesmp.utils.EntityUtils;
import com.roguesmp.utils.PlayerUtils;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Random;

public class DoomTotemSpell extends Spell {
    private final LivingEntity owner;
    private final PrimordialSlime boss;
    private final Location altarLocation;
    private final double spawnRadius;
    private final int timeLimitTicks;
    private final double maxHealth;

    public DoomTotemSpell(LivingEntity owner, PrimordialSlime boss, Location altarLocation, double spawnRadius, int timeLimitTicks, double maxHealth) {
        this.owner = owner;
        this.boss = boss;
        this.altarLocation = altarLocation;
        this.spawnRadius = spawnRadius;
        this.timeLimitTicks = timeLimitTicks;
        this.maxHealth = maxHealth;
    }

    @Override
    public void run() {
        if (owner == null || !owner.isValid()) return;

        // Bắt đầu triệu hồi
        spawnDoomTotem();
    }

    private void spawnDoomTotem() {
        Location spawnLoc = getRandomLocationAroundAltar();

        // 1. Khởi tạo Thanh đếm ngược (Count down)
        ChargeUpManager timerBar = new ChargeUpManager(
                spawnLoc, // Gắn vị trí tại Totem
                null,
                timeLimitTicks,
                Component.text("PHÁ HỦY LÕI!", NamedTextColor.RED, TextDecoration.BOLD),
                BossBar.Color.RED,
                BossBar.Overlay.NOTCHED_10,
                30
        );
        timerBar.setTime(timeLimitTicks);

        // 2. Triệu hồi Living Entity làm Totem (Sử dụng MagmaCube size 2)
        MagmaCube totemEntity = spawnLoc.getWorld().spawn(spawnLoc, MagmaCube.class, magma -> {
            magma.setSize(2);
            magma.setAI(false); // Đứng im một chỗ
            magma.setCustomNameVisible(true);
            magma.setRemoveWhenFarAway(false);
            magma.getAttribute(Attribute.MAX_HEALTH).setBaseValue(maxHealth);
            magma.setHealth(maxHealth); // Bạn có thể chỉnh máu tùy độ khó
            magma.customName(Component.text("LÕI DIỆT VONG", NamedTextColor.DARK_RED, TextDecoration.BOLD));
        });

        // Thông báo
        broadcast(Component.text("!!! MA CHẤT ĐANG NGƯNG TỤ TẠI MỘT ĐIỂM !!!", NamedTextColor.GOLD, TextDecoration.BOLD));

        new BukkitRunnable() {
            int ticksLeft = timeLimitTicks;

            @Override
            public void run() {
                // Kiểm tra nếu Totem đã bị giết (LivingEntity.isDead)
                if (totemEntity.isDead() || !totemEntity.isValid()) {
                    timerBar.remove();
                    handleSuccess(spawnLoc);
                    this.cancel();
                    return;
                }

                // Cập nhật thanh BossBar đếm ngược
                timerBar.previousTick();

                // Hiệu ứng hạt xung quanh Totem để tạo điểm nhấn
                spawnLoc.getWorld().spawnParticle(Particle.FLAME, totemEntity.getLocation().add(0, 0.5, 0), 10, 0.5, 0.5, 0.5, 0.02);
                spawnLoc.getWorld().spawnParticle(Particle.LAVA, totemEntity.getLocation().add(0, 0.5, 0), 2, 0.3, 0.3, 0.3, 0);

                // Khi hết thời gian
                if (ticksLeft <= 0) {
                    timerBar.remove();
                    if (totemEntity.isValid()) {
                        executeDoom(totemEntity.getLocation());
                        totemEntity.remove();
                    }
                    this.cancel();
                    return;
                }

                // Âm thanh cảnh báo
                if (ticksLeft < timeLimitTicks && ticksLeft % 10 == 0) {
                    spawnLoc.getWorld().playSound(spawnLoc, Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 1f, 0.5f);
                    totemEntity.setGlowing(true); // Nháy sáng để cảnh báo
                } else if (ticksLeft < timeLimitTicks) {
                    totemEntity.setGlowing(false);
                }

                ticksLeft--;
            }
        }.runTaskTimer(RogueSmpCore.getInstance(), 0L, 1L);
    }

    private void handleSuccess(Location loc) {
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 1.2f);
        loc.getWorld().spawnParticle(Particle.EXPLOSION, loc, 5);
        broadcast(Component.text("Phá hủy thành công! Năng lượng diệt vong đã tan biến.", NamedTextColor.GREEN));
    }

    private void executeDoom(Location totemLoc) {
        broadcast(Component.text("QUÁ MUỘN! LINH HỒN CÁC NGƯƠI SẼ BỊ THIÊU RỤI!", NamedTextColor.DARK_RED, TextDecoration.BOLD));
        totemLoc.getWorld().playSound(totemLoc, Sound.ENTITY_WITHER_DEATH, 2f, 0.5f);

        for (Player p : boss.getParticipants()) {
            // Sát thương "one-shot"
            DamageUtils.damage(p, owner, 100.0, new DamageEvent.Metadata(DamageType.MAGIC));
            p.getWorld().spawnParticle(Particle.FLASH, p.getLocation(), 1, Color.RED);

        }
    }

    private void broadcast(Component msg) {
        boss.broadcastBossMessage(msg);
    }

    private Location getRandomLocationAroundAltar() {
        Random random = new Random();
        double angle = random.nextDouble() * 2 * Math.PI;
        double r = 8 + (random.nextDouble() * (spawnRadius - 8));
        double x = altarLocation.getX() + r * Math.cos(angle);
        double z = altarLocation.getZ() + r * Math.sin(angle);

        Location loc = new Location(altarLocation.getWorld(), x, altarLocation.getY(), z);
        return loc.getWorld().getHighestBlockAt(loc).getLocation().add(0.5, 1, 0.5);
    }

    @Override
    public int cooldownTicks() { return timeLimitTicks; }
}
