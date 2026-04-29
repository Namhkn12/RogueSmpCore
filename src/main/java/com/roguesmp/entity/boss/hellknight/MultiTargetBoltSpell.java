package com.roguesmp.entity.boss.hellknight;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.constant.DamageType;
import com.roguesmp.entity.spell.ChargeUpManager;
import com.roguesmp.entity.spell.Spell;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.utils.DamageUtils;
import com.roguesmp.utils.Hitbox;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.List;

public class MultiTargetBoltSpell extends Spell {

    private final HellKnight boss;
    private final double damage;
    private final double radius; // Sử dụng để xác định độ dày/bán kính hiển thị
    private final double travelSpeed;
    private final int shootTimes;
    private final int delayPerShot;
    private ChargeUpManager chargeUp;

    public MultiTargetBoltSpell(HellKnight boss, double damage, double radius, double travelSpeed, int shootTimes, int delayPerShot) {
        this.boss = boss;
        this.damage = damage;
        this.radius = radius;
        this.travelSpeed = travelSpeed;
        this.shootTimes = shootTimes;
        this.delayPerShot = delayPerShot;
    }

    @Override
    public int cooldownTicks() {
        return 200;
    }

    @Override
    public void run(int interval) {
        LivingEntity entity = boss.getEntity();

        this.chargeUp = new ChargeUpManager(
                entity,
                40,
                Utils.fromString("<red><bold>MA LỰC ĐANG TÍCH TỤ..."),
                BossBar.Color.RED,
                BossBar.Overlay.NOTCHED_10,
                60
        );

        entity.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 50, 0));
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_BLAZE_HURT, SoundCategory.HOSTILE, 5f, 0.5f);
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ZOMBIFIED_PIGLIN_AMBIENT, SoundCategory.HOSTILE, 5f, 0.5f);

        BukkitRunnable spellTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!entity.isValid() || boss.dead) {
                    cleanup();
                    return;
                }

                if (chargeUp.nextTick()) {
                    boss.dialogue("<dark_red>LINH HỒN CÁC NGƯƠI LÀ CỦA TA!");
                    BukkitRunnable shootingRun = new BukkitRunnable() {
                        int count = 0;
                        @Override
                        public void run() {
                            if (count >= shootTimes) {
                                cancel();
                                return;
                            }
                            count++;
                            launchBolts(entity);
                        }
                    };
                    shootingRun.runTaskTimer(RogueSmpCore.getInstance(), 1, delayPerShot);
                    activeRunnables.add(shootingRun);
                    cleanup();
                    return;
                }

                playChargeEffects(entity);
            }

            private void cleanup() {
                chargeUp.remove();
                entity.removePotionEffect(PotionEffectType.GLOWING);
                activeRunnables.remove(this);
                this.cancel();
            }
        };

        spellTask.runTaskTimer(RogueSmpCore.getInstance(), 0L, 1L);
        activeRunnables.add(spellTask);
    }

    private void playChargeEffects(LivingEntity entity) {
        double progress = (double) chargeUp.getTime() / chargeUp.getChargeTime();
        double radius = 3.0 * (1.0 - progress);
        double angle = chargeUp.getTime() * 0.4;

        Location loc = entity.getLocation().add(0, 1, 0);
        loc.getWorld().spawnParticle(Particle.FLAME,
                loc.clone().add(Math.cos(angle) * radius, Math.sin(chargeUp.getTime() * 0.2), Math.sin(angle) * radius),
                1, 0, 0, 0, 0);

        loc.getWorld().spawnParticle(Particle.SOUL, loc.clone().add(0, -0.5, 0), 2, 0.5, 0.2, 0.5, 0.05);
    }

    private void launchBolts(LivingEntity caster) {
        List<Player> targets = boss.getParticipants();
        if (targets.isEmpty()) return;

        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 2.0f, 0.5f);

        for (Player target : targets) {
            spawnTravelingBolt(caster, target);
        }
    }

    private void spawnTravelingBolt(LivingEntity caster, Player target) {
        // 1. Khởi tạo ItemDisplay ở vị trí xuất phát
        ItemDisplay itemDisplay = caster.getWorld().spawn(caster.getEyeLocation(), ItemDisplay.class, display -> {
            display.setItemStack(new ItemStack(Material.MAGMA_BLOCK)); // Bạn có thể đổi sang material khác
            display.setBrightness(new Display.Brightness(15, 15));
            display.setPersistent(false);

            float scale = (float) radius * 0.5f;
            display.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    new AxisAngle4f(0, 0, 0, 0),
                    new Vector3f(scale, scale, scale),
                    new AxisAngle4f(0, 0, 0, 0)
            ));
        });

        BukkitRunnable travelTask = new BukkitRunnable() {
            final Location currentLoc = caster.getEyeLocation();
            final Vector direction = target.getLocation().add(0, 1.5, 0).toVector().subtract(currentLoc.toVector()).normalize();
            int life = 0;
            float rotation = 0;

            @Override
            public void run() {
                if (life > 100 || !currentLoc.getWorld().getChunkAt(currentLoc).isLoaded()) {
                    stop();
                    return;
                }

                // Cập nhật vị trí di chuyển
                currentLoc.add(direction.clone().multiply(travelSpeed));

                // 2. Cập nhật ItemDisplay (Di chuyển và xoay)
                rotation += 0.5f;
                itemDisplay.teleport(currentLoc);
                Transformation trans = itemDisplay.getTransformation();
                trans.getLeftRotation().set(new AxisAngle4f(rotation, 0, 0, 1)); // Xoay quanh trục Z
                itemDisplay.setInterpolationDuration(2);
                itemDisplay.setTransformation(trans);

                drawBoltEffect(currentLoc, direction);

                Hitbox hb = new Hitbox.SphereHitbox(currentLoc, radius);
                for (Entity e : hb.getHitPlayers(true)) {
                    if (e instanceof Player p) {
                        hit(p, caster);
                        stop();
                        return;
                    }
                }
                life++;
            }

            private void stop() {
                itemDisplay.remove(); // Xóa ItemDisplay khi bolt biến mất
                activeRunnables.remove(this);
                this.cancel();
            }
        };
        travelTask.runTaskTimer(RogueSmpCore.getInstance(), 0L, 1L);
        activeRunnables.add(travelTask);
    }

    private void drawBoltEffect(Location start, Vector direction) {
        World world = start.getWorld();
        int segments = 5;
        Location lastLoc = start.clone();
        Vector segmentStep = direction.clone().multiply(1.0 / segments);

        for (int i = 0; i < segments; i++) {
            Vector jitter = new Vector(Math.random() - 0.5, Math.random() - 0.5, Math.random() - 0.5).multiply(0.6);
            Location nextLoc = lastLoc.clone().add(segmentStep).add(jitter);
            drawThickParticleLine(lastLoc, nextLoc, world);
            lastLoc = nextLoc;
        }
    }

    private void drawThickParticleLine(Location a, Location b, World world) {
        double dist = a.distance(b);
        Vector v = b.toVector().subtract(a.toVector()).normalize().multiply(0.15);
        double covered = 0;
        Location cursor = a.clone();

        while (covered < dist) {
            cursor.add(v);
            world.spawnParticle(Particle.ELECTRIC_SPARK, cursor, 2, 0.05, 0.05, 0.05, 0.01);
            world.spawnParticle(Particle.SOUL_FIRE_FLAME, cursor, 4, 0.15, 0.15, 0.15, 0.02);
            if (Math.random() > 0.8) {
                world.spawnParticle(Particle.SCRAPE, cursor, 1, 0.3, 0.3, 0.3, 0.1);
            }
            covered += 0.15;
        }
    }

    private void hit(Player victim, LivingEntity caster) {
        DamageUtils.damage(victim, caster, damage, new DamageEvent.Metadata("multi_bolt", null, DamageType.BLAST, true));
        victim.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, victim.getLocation().add(0, 1, 0), 5, 0.2, 0.2, 0.2, 0.05);
        victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.0f, 1.2f);
    }
}
