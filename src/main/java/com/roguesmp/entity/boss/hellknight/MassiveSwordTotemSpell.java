package com.roguesmp.entity.boss.hellknight;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.constant.DamageType;
import com.roguesmp.entity.spell.ChargeUpManager;
import com.roguesmp.entity.spell.Spell;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.utils.DamageUtils;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

// Unused
public class MassiveSwordTotemSpell extends Spell {

    private final HellKnight boss;
    private final double explosionDamage;
    private final List<WitherSkeleton> activeTotems = new ArrayList<>();
    private ItemDisplay giantSword;
    private ChargeUpManager chargeBar;

    public MassiveSwordTotemSpell(HellKnight boss, double explosionDamage) {
        this.boss = boss;
        this.explosionDamage = explosionDamage;
    }

    @Override
    public int cooldownTicks() {
        return 600; // 30 giây
    }

    @Override
    public void run(int interval) {

        LivingEntity caster = boss.getEntity();

        caster.setAI(false);
        caster.setInvulnerable(true);

        caster.teleport(boss.getAltarLocation());
        Location center = caster.getLocation();

        // 1. Tạo Thanh Trọng Kiếm (Giant Sword Display)
        giantSword = caster.getWorld().spawn(center.clone().add(0, 5, 0), ItemDisplay.class, display -> {
            display.setItemStack(new ItemStack(Material.NETHERITE_SWORD));
            display.setBrightness(new Display.Brightness(15, 15));
            display.setPersistent(false);
            display.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    new AxisAngle4f(0, 0, 0, 0),
                    new Vector3f(5f, 5f, 5f), // Kiếm khổng lồ
                    new AxisAngle4f((float) Math.toRadians(180), 1, 0, 0) // Quay ngược mũi kiếm xuống
            ));
        });

        // 2. Triệu hồi 4 Wither Skeleton Totems xung quanh
        spawnTotems(center);

        // 3. Khởi tạo ChargeUpManager (Gồng lâu - 10 giây = 200 ticks)
        chargeBar = new ChargeUpManager(
                caster, 200,
                Utils.fromString("<red><bold>NGHI THỨC DIỆT CHỦNG"),
                BossBar.Color.PURPLE, BossBar.Overlay.NOTCHED_20, 50
        );

        // 4. Logic xử lý chính
        BukkitRunnable mechanicTask = new BukkitRunnable() {
            float rotation = 0;

            @Override
            public void run() {
                // Kiểm tra nếu Boss chết hoặc bị mất thực thể
                if (!caster.isValid() || boss.dead) {
                    failAndCleanup();
                    return;
                }

                // Kiểm tra xem các Totem còn sống không
                activeTotems.removeIf(totem -> !totem.isValid() || totem.isDead());

                // NẾU TẤT CẢ TOTEM BỊ PHÁ HỦY: Hủy Spell thành công
                if (activeTotems.isEmpty()) {
                    boss.dialogue("<green>Nghi thức đã bị phá vỡ!");
                    caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_GLASS_BREAK, 2f, 0.5f);
                    successCleanup();
                    return;
                }

                // NẾU THANH GỒNG ĐẦY: Người chơi thất bại, nhận sát thương lớn
                if (chargeBar.nextTick()) {
                    executeMassiveDamage(caster);
                    failAndCleanup();
                    return;
                }

                // Hiệu ứng xoay kiếm và hạt năng lượng từ totem truyền vào kiếm
                updateVFX(caster);
            }

            private void successCleanup() {
                if (giantSword != null) giantSword.remove();
                chargeBar.remove();
                activeRunnables.remove(this);
                this.cancel();
            }

            private void failAndCleanup() {
                activeTotems.forEach(Entity::remove);
                successCleanup();
            }

            private void updateVFX(LivingEntity caster) {
                rotation += 0.1f;
                Transformation t = giantSword.getTransformation();
                t.getLeftRotation().set(new AxisAngle4f(rotation, 0, 1, 0));
                giantSword.setTransformation(t);

                // Vẽ tia sét từ mỗi totem truyền vào thanh kiếm
                for (WitherSkeleton totem : activeTotems) {
                    drawEnergyBeam(totem.getEyeLocation(), giantSword.getLocation());
                }
            }
        };

        mechanicTask.runTaskTimer(RogueSmpCore.getInstance(), 0L, 1L);
        activeRunnables.add(mechanicTask);
    }

    private void spawnTotems(Location center) {
        double radius = 6.0;
        for (int i = 0; i < 4; i++) {
            double angle = i * Math.PI / 2;
            Location spawnLoc = center.clone().add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
            spawnLoc.setDirection(center.toVector().subtract(spawnLoc.toVector()));

            WitherSkeleton totem = spawnLoc.getWorld().spawn(spawnLoc, WitherSkeleton.class, skeleton -> {
                skeleton.setAI(false); // Đứng yên như totem
                skeleton.customName(Utils.fromString("<b><dark_red>Vật Tế Linh Hồn"));
                skeleton.setCustomNameVisible(true);
                skeleton.getAttribute(Attribute.MAX_HEALTH).setBaseValue(40.0);
                skeleton.setHealth(40.0);
                skeleton.setGlowing(true);
            });
            activeTotems.add(totem);
        }
    }

    private void drawEnergyBeam(Location start, Location end) {
        Vector dir = end.toVector().subtract(start.toVector());
        double dist = start.distance(end);
        dir.normalize().multiply(0.5);

        Location current = start.clone();
        for (double d = 0; d < dist; d += 0.5) {
            start.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, current, 1, 0, 0, 0, 0);
            current.add(dir);
        }
    }

    private void executeMassiveDamage(LivingEntity caster) {
        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 5f, 0.5f);
        caster.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, caster.getLocation(), 10, 3, 3, 3);

        for (Player p : boss.getParticipants()) {
            DamageUtils.damage(p, caster, explosionDamage, new DamageEvent.Metadata("massive_sword", null, DamageType.MAGIC, true));
            p.sendMessage(Utils.fromString("<red>Bạn đã thất bại ngăn chặn nghi thức!"));
        }
    }
}
