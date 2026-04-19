package com.roguesmp.entity.boss.primordialslime;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.constant.DamageType;
import com.roguesmp.entity.boss.PrimordialSlime;
import com.roguesmp.entity.spell.ChargeUpManager;
import com.roguesmp.entity.spell.Spell;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.utils.DamageUtils;
import com.roguesmp.utils.MovementUtils;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.awt.*;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class MeteorRainSpell extends Spell {
    private final LivingEntity owner;
    private final PrimordialSlime boss;
    private final Location altarLocation;
    private final double arenaRadius;
    private final double damage;
    private final int durationTicks;
    private final double explosionRadius = 5.0;

    public MeteorRainSpell(LivingEntity owner, PrimordialSlime boss, Location altarLocation, double arenaRadius, double damage, int durationTicks) {
        this.owner = owner;
        this.boss = boss;
        this.altarLocation = altarLocation;
        this.arenaRadius = arenaRadius;
        this.damage = damage;
        this.durationTicks = durationTicks;
    }

    @Override
    public void run() {
        if (owner == null || !owner.isValid()) return;

        ChargeUpManager timerBar = new ChargeUpManager(
                owner,
                durationTicks,
                Component.text("!!! THIÊN THẠCH ĐANG RƠI !!!", NamedTextColor.GOLD, TextDecoration.BOLD),
                BossBar.Color.YELLOW,
                BossBar.Overlay.NOTCHED_10,
                (int) arenaRadius + 20
        );
        timerBar.setTime(durationTicks);

        broadcast(Component.text("Bầu trời rực đỏ... Hãy tìm chỗ trốn!", NamedTextColor.RED, TextDecoration.ITALIC));

        new BukkitRunnable() {
            int ticksElapsed = 0;
            @Override
            public void run() {
                if (!owner.isValid() || ticksElapsed >= durationTicks) {
                    timerBar.remove();
                    this.cancel();
                    return;
                }

                timerBar.previousTick();

                // Dội thiên thạch nhanh dần hoặc đều đặn
                if (ticksElapsed % 25 == 0) {
                    spawnMeteor();
                }

                ticksElapsed++;
            }
        }.runTaskTimer(RogueSmpCore.getInstance(), 0L, 1L);
    }

    private void spawnMeteor() {
        List<Player> targets = boss.getParticipants().stream().toList();
        if (targets.isEmpty()) return;

        Player randomTarget = targets.get(ThreadLocalRandom.current().nextInt(targets.size()));
        Location targetLanding = getRandomLoc(randomTarget.getLocation(), 5);

        Location spawnLoc = targetLanding.clone().add(
                ThreadLocalRandom.current().nextDouble(-4, 4),
                25,
                ThreadLocalRandom.current().nextDouble(-4, 4)
        );

        // 1. Vẽ vòng tròn cảnh báo (Indicator)
        new BukkitRunnable() {
            int timer = 0;
            @Override
            public void run() {
                if (timer > 30) { this.cancel(); return; }
                drawCircleIndicator(targetLanding, explosionRadius);
                timer += 2;
            }
        }.runTaskTimer(RogueSmpCore.getInstance(), 0L, 2L);

        // 2. Tạo ItemDisplay làm Thiên Thạch
        ItemDisplay meteorVisual = spawnLoc.getWorld().spawn(spawnLoc, ItemDisplay.class, id -> {
            id.setItemStack(new ItemStack(Material.FIRE_CHARGE)); // Hoặc MAGMA_BLOCK/MAGMA_CREAM
            id.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    new Quaternionf(),
                    new Vector3f(2.5f, 2.5f, 2.5f), // ItemDisplay thường cần scale lớn hơn Block
                    new Quaternionf()
            ));
            id.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.HEAD);
        });

        Vector velocity = targetLanding.toVector().subtract(spawnLoc.toVector()).normalize().multiply(0.9);

        new BukkitRunnable() {
            int life = 0;
            float rotation = 0;

            @Override
            public void run() {
                Location current = meteorVisual.getLocation();

                // Hiệu ứng hạt
                current.getWorld().spawnParticle(Particle.FLAME, current, 5, 0.2, 0.2, 0.2, 0.05);
                current.getWorld().spawnParticle(Particle.LAVA, current, 1, 0.1, 0.1, 0.1, 0);

                // Di chuyển & Xoay Item cho sinh động
                rotation += 0.2f;
                meteorVisual.teleport(current.add(velocity));

                Transformation trans = meteorVisual.getTransformation();
                trans.getLeftRotation().set(new AxisAngle4f(rotation, 1, 1, 0)); // Xoay đa trục
                meteorVisual.setTransformation(trans);

                // Kiểm tra va chạm
                if (current.getY() <= targetLanding.getY() || current.getBlock().getType().isSolid() || life > 80) {
                    explodeMeteor(current);
                    meteorVisual.remove();
                    this.cancel();
                    return;
                }
                life++;
            }
        }.runTaskTimer(RogueSmpCore.getInstance(), 0L, 1L);
    }

    private void drawCircleIndicator(Location loc, double radius) {
        for (double i = 0; i < Math.PI * 2; i += Math.PI / 10) {
            double x = Math.cos(i) * radius;
            double z = Math.sin(i) * radius;
            loc.getWorld().spawnParticle(Particle.FLAME, loc.clone().add(x, 0.1, z), 1, 0, 0, 0, 0);
        }
    }

    private void explodeMeteor(Location loc) {
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 2f, 0.6f);
        loc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, loc, 1);
        loc.getWorld().spawnParticle(Particle.FLASH, loc, 1, 0, 0, 0, 0, Color.RED);

        for (Player p : boss.getParticipants()) {
            if (p.getLocation().distanceSquared(loc) <= (explosionRadius * explosionRadius)) {
                DamageUtils.damage(p, owner, damage, new DamageEvent.Metadata(DamageType.BLAST));

                // Hiệu ứng hất văng
                Vector push = p.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(0.6).setY(0.4);
                MovementUtils.knockAwayDirection(push, p, 0, true);
            }
        }
    }

    private Location getRandomLoc(Location center, double r) {
        double x = center.getX() + (ThreadLocalRandom.current().nextDouble() - 0.5) * 2 * r;
        double z = center.getZ() + (ThreadLocalRandom.current().nextDouble() - 0.5) * 2 * r;
        return new Location(center.getWorld(), x, center.getWorld().getHighestBlockYAt((int)x, (int)z), z);
    }

    private void broadcast(Component msg) {
        boss.broadcastBossMessage(msg);
    }

    @Override
    public int cooldownTicks() { return durationTicks + 100; }
}
