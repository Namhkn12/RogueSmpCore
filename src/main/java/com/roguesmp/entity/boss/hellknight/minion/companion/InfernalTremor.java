package com.roguesmp.entity.boss.hellknight.minion.companion;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.constant.DamageType;
import com.roguesmp.entity.spell.ChargeUpManager;
import com.roguesmp.entity.spell.Spell;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.utils.*;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class InfernalTremor extends Spell {
    private final LivingEntity boss;
    private final double baseDamage;
    private final int totalStomps;
    private final int period;
    private final double radius;
    private final double travelSpeed;
    private final ChargeUpManager charge;

    public InfernalTremor(LivingEntity boss, double damage, int stomps, int period, double radius, double travelSpeed, int chargeTime) {
        this.boss = boss;
        this.baseDamage = damage;
        this.totalStomps = stomps;
        this.period = period;
        this.radius = radius;
        this.travelSpeed = travelSpeed;
        // BossBar and title to warn players
        this.charge = new ChargeUpManager(boss, chargeTime, Utils.fromString("<red><bold>ĐỊA CHẤN TRỖI DẬY..."),
                BossBar.Color.YELLOW, BossBar.Overlay.NOTCHED_10, 40);
    }

    @Override
    public int cooldownTicks() { return 300; }

    @Override
    public void run(int interval) {
        boss.setAI(false); // Root the ravager during the heavy attack

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!boss.isValid()) {
                    charge.remove();
                    this.cancel();
                    return;
                }

                if (charge.getTime() % 5 == 0) {
                    boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_RAVAGER_ROAR, 1.0f, 0.5f + (charge.getProgress() * 0.5f));
                    // Dust clouds around feet
                    spawnChargeParticles();
                }
                GlowUtils.glow(boss, ChatColor.DARK_RED);
                if (charge.nextTick()) {
                    executeStomps();
                    GlowUtils.unsetGlow(boss);
                    charge.reset();
                    this.cancel();
                }
            }
        }.runTaskTimer(RogueSmpCore.getInstance(), 0L, 1L);
    }

    private void executeStomps() {
        new BukkitRunnable() {
            int stompsDone = 0;

            @Override
            public void run() {
                if (!boss.isValid() || stompsDone >= totalStomps) {
                    boss.setAI(true);
                    this.cancel();
                    return;
                }

                // The Impact
                boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_RAVAGER_ATTACK, 2f, 0.5f);
                boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1f, Utils.RANDOM.nextFloat());

                // Fire the ripple shockwave
                createShockwave(boss.getLocation(), radius);

                stompsDone++;
            }
        }.runTaskTimer(RogueSmpCore.getInstance(), 0L, period);
    }

    private void spawnChargeParticles() {
        Location loc = boss.getLocation();
        // Sucking in "heat" from the arena
        for (int i = 0; i < 5; i++) {
            double offset = 3.0 * (1.0 - charge.getProgress());
            Vector v = new Vector((Math.random()-0.5) * offset, 0, (Math.random()-0.5) * offset);
            loc.getWorld().spawnParticle(Particle.FLAME, loc.clone().add(v), 0, 0, 0.2, 0, 0.1);
        }
    }

    private void createShockwave(Location center, double maxRadius) {
        new BukkitRunnable() {
            double currentRadius = 1.0;

            @Override
            public void run() {
                if (currentRadius > maxRadius) {
                    this.cancel();
                    return;
                }

                // Draw the ring
                for (double i = 0; i < 360; i += 8) {
                    double rad = Math.toRadians(i);
                    double x = Math.cos(rad) * currentRadius;
                    double z = Math.sin(rad) * currentRadius;
                    Location pLoc = center.clone().add(x, 0.1, z);

                    // Main "Danger" Ring (Flat on ground)
                    pLoc.getWorld().spawnParticle(Particle.LAVA, pLoc, 1, 0, 0, 0, 0);

                    // Outward "Dust" (Gives the sense of traveling speed)
                    if (Utils.RANDOM.nextInt(3) == 0) {
                        Vector dir = new Vector(x, 0, z).normalize().multiply(0.2);
                        pLoc.getWorld().spawnParticle(Particle.FLAME, pLoc, 0, dir.getX(), 0.05, dir.getZ(), 0.1);
                    }

                    // Ground shattering
                    if (Utils.RANDOM.nextBoolean()) {
                        pLoc.getWorld().spawnParticle(Particle.BLOCK, pLoc, 1, 0.1, 0.1, 0.1, 0, Material.MAGMA_BLOCK.createBlockData());
                    }
                }

                // Damage logic with Jump Check
                Hitbox hb = new Hitbox.SphereHitbox(center, currentRadius + 1.0);
                hb.getHitPlayers(true).forEach(p -> {
                    double dist = p.getLocation().distance(center);

                    // Only hit if they are on the current expansion ring
                    if (Math.abs(dist - currentRadius) < 1.2) {

                        // JUMP CHECK: If player is 0.5 blocks above the ground, they dodge
                        if (p.getLocation().getY() > center.getY() + 0.5) {
                            return;
                        }

                        DamageUtils.damage(p, boss, baseDamage, new DamageEvent.Metadata("ravager_stomp", null, DamageType.MELEE, false));

                        // Knockback: Away from center
                        Vector kb = p.getLocation().toVector().subtract(center.toVector()).normalize().multiply(0.8).setY(0.4);
                        MovementUtils.knockAwayDirection(kb, p, 0f, true);
                    }
                });

                currentRadius += travelSpeed; // Controls the "travel" speed
            }
        }.runTaskTimer(RogueSmpCore.getInstance(), 0L, 1L);
    }
}
