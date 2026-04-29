package com.roguesmp.entity.boss.hellknight;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.entity.spell.Spell;
import com.roguesmp.utils.GlowUtils;
import com.roguesmp.utils.PlayerUtils;
import com.roguesmp.utils.Utils;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

public class WatchfulEye extends Spell {
    private final HellKnight boss;
    private final List<Slime> activeEyes = new ArrayList<>();

    private final int amountPerSpawn;
    private final int fuse;
    private final int cooldown;

    private int currentCooldown;

    public WatchfulEye(HellKnight boss, int amountPerSpawn, int fuse, int cooldown) {
        this.boss = boss;
        this.amountPerSpawn = amountPerSpawn;
        this.fuse = fuse;
        this.cooldown = cooldown;
    }

    @Override
    public int cooldownTicks() { return cooldown; } // 10 seconds

    @Override
    public void run(int interval) {
        currentCooldown += interval;
        if (currentCooldown < cooldownTicks()) return;
        currentCooldown = 0;

        // Get all players to target
        List<Player> targets = boss.getParticipants();
        if (targets.isEmpty()) return;

        new BukkitRunnable() {
            int spawnedCount = 0;

            @Override
            public void run() {
                if (spawnedCount >= amountPerSpawn || !boss.getEntity().isValid()) {
                    this.cancel();
                    return;
                }

                // Target players one by one or randomly
                for (Player player : targets) {
                    spawnWatchfulEye(player);
                }

                spawnedCount++;
            }
        }.runTaskTimer(RogueSmpCore.getInstance(), 0L, 10L); // Spawns every 0.5 seconds
    }

    private void spawnWatchfulEye(Player player) {
        Location spawnLoc = player.getLocation().add(
                (Utils.RANDOM.nextDouble() - 0.5) * 6,
                4,
                (Utils.RANDOM.nextDouble() - 0.5) * 6
        );

        spawnLoc.getWorld().spawnParticle(Particle.FLASH, spawnLoc, 2, Color.RED);

        Slime eye = spawnLoc.getWorld().spawn(spawnLoc, Slime.class, s -> {
            s.setInvisible(true);
            GlowUtils.glow(s, player, ChatColor.RED);
            s.setSilent(true);
            s.setPersistent(false);
            s.setSize(1);
            s.customName(Utils.fromString("<red><b>Nhãn cầu hận thù"));
            s.setCustomNameVisible(true);
            s.setGravity(false);
            s.setAI(false);
        });

        activeEyes.add(eye);
        startEyeFuse(eye);
    }

    private void startEyeFuse(Slime eye) {
        new BukkitRunnable() {
            int currentFuse = 0;

            @Override
            public void run() {
                if (!eye.isValid() || boss.getEntity().isDead()) { // Check boss status too
                    activeEyes.remove(eye);
                    eye.remove();
                    this.cancel();
                    return;
                }

                if (currentFuse % 5 == 0) {
                    eye.getWorld().spawnParticle(Particle.DUST, eye.getLocation().add(0, 0.2, 0), 5, 0.2, 0.2, 0.2, new Particle.DustOptions(Color.RED, 1.0f));
                    eye.getWorld().playSound(eye.getLocation(), Sound.UI_TOAST_IN, 3f, 0.5f + (currentFuse / (float)fuse));
                }

                if (currentFuse >= fuse) {
                    explodeEye(eye);
                    activeEyes.remove(eye);
                    this.cancel();
                    return;
                }
                currentFuse++;
            }
        }.runTaskTimer(RogueSmpCore.getInstance(), 0L, 1L);
    }

    private void explodeEye(Slime eye) {
        Location loc = eye.getLocation();

        // VFX
        loc.getWorld().spawnParticle(Particle.EXPLOSION, loc, 1);
        loc.getWorld().spawnParticle(Particle.SQUID_INK, loc, 20, 0.5, 0.5, 0.5, 0.1);
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1.5f);

        // Apply effects to nearby players
        PlayerUtils.playersInRange(loc, 20, true).forEach(p -> {
            // High Slowness and Wither
            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 2));
            p.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 100, 2));
            p.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 100, 2));
        });

        eye.remove();
    }
}
