package com.roguesmp.entity.boss.hellknight.minion;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.constant.DamageType;
import com.roguesmp.entity.spell.ChargeUpManager;
import com.roguesmp.entity.spell.Spell;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.utils.DamageUtils;
import com.roguesmp.utils.GlowUtils;
import com.roguesmp.utils.Hitbox;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class HellFireBeam extends Spell {
    private final LivingEntity boss;
    private final double damage;
    private final double range;
    private final int chargeTime;
    private final ChargeUpManager charge;

    private Vector lockedDirection;

    public HellFireBeam(LivingEntity boss, double damage, double range, int chargeTime) {
        this.boss = boss;
        this.damage = damage;
        this.range = range;
        this.chargeTime = chargeTime;
        this.charge = new ChargeUpManager(boss, chargeTime, Utils.fromString("<gold>RAILGUN CHARGING..."),
                BossBar.Color.WHITE, BossBar.Overlay.PROGRESS, 30);
    }

    @Override
    public int cooldownTicks() { return 160; }

    @Override
    public void run(int interval) {
        // Lock the direction at the moment the spell starts
        lockedDirection = boss.getLocation().getDirection().normalize();
        boss.setAI(false);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!boss.isValid()) {
                    charge.remove();
                    this.cancel();
                    return;
                }

                GlowUtils.glow(boss, ChatColor.AQUA, chargeTime);

                // charge.nextTick() returns true when the timer is finished
                if (charge.nextTick()) {
                    fireBeam();
                    boss.setAI(true); // Release the boss
                    charge.reset();
                    this.cancel();
                    return;
                }

                // --- DURING CHARGE ---
                // Keep forcing the head to face the locked direction
                Location lookLoc = boss.getLocation();
                lookLoc.setDirection(lockedDirection);
                boss.teleport(lookLoc);

                // Visual "Wind-up"
                if (charge.getTime() % 2 == 0) {
                    drawTelegraph();
                }

                if (charge.getTime() % 5 == 0) {
                    boss.getWorld().playSound(boss.getLocation(), Sound.BLOCK_BEACON_AMBIENT, 1.0f, 0.5f + charge.getProgress());
                }
            }
        }.runTaskTimer(RogueSmpCore.getInstance(), 0L, 1L);
    }

    private void drawTelegraph() {
        Location start = boss.getEyeLocation();
        for (double d = 1.5; d < range; d += 3.0) {
            Location point = start.clone().add(lockedDirection.clone().multiply(d));
            point.getWorld().spawnParticle(Particle.DUST, point, 1, 0, 0, 0, new Particle.DustOptions(Color.RED, 0.6f));
        }
    }

    private void fireBeam() {
        Location start = boss.getEyeLocation();
        boss.getWorld().playSound(start, Sound.ENTITY_IRON_GOLEM_ATTACK, 2f, 0.5f);
        boss.getWorld().playSound(start, Sound.ENTITY_GENERIC_EXPLODE, 2f, 1.8f);

        // Instant Piercing Ray
        for (double d = 0; d < range; d += 0.5) {
            Location point = start.clone().add(lockedDirection.clone().multiply(d));

            // Beam Visuals
            point.getWorld().spawnParticle(Particle.FLAME, point, 2, 0.02, 0.02, 0.02, 0.01);
            if (d % 3 == 0) {
                point.getWorld().spawnParticle(Particle.FLASH, point, 1, 0, 0, 0, 0, Color.AQUA);
            }

            // Damage Logic (Piercing)
            Hitbox hb = new Hitbox.SphereHitbox(point, 1.1);
            hb.getHitPlayers(true).forEach(p -> {
                // Ensure the "railgun_beam" tag handles multi-hit prevention in your DamageUtils
                DamageUtils.damage(p, boss, damage, new DamageEvent.Metadata("railgun_beam", null, DamageType.PROJECTILE, false));
            });
        }

        // Recoil effect: Push the boss back slightly
        boss.setVelocity(lockedDirection.clone().multiply(-0.4).setY(0.1));
    }
}
