package com.roguesmp.entity.boss.primordialslime;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.constant.DamageType;
import com.roguesmp.effect.EffectManager;
import com.roguesmp.effect.impl.SpeedEffect;
import com.roguesmp.entity.spell.ChargeUpManager;
import com.roguesmp.entity.spell.Spell;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.utils.DamageUtils;
import com.roguesmp.utils.MovementUtils;
import com.roguesmp.utils.PlayerUtils;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class MagmaTendrils extends Spell {
    private final LivingEntity owner;
    private final double damage;
    private final ChargeUpManager chargeUp;

    private final int strikesPerPlayer;
    private final int ticksBetweenStrikes;
    private final int indicatorDelay;
    private final double scanRadius;   // How far the boss looks for players
    private final double strikeRadius; // The size of the actual damage/indicator zone

    public MagmaTendrils(LivingEntity owner, double damage, int strikesPerPlayer, int ticksBetweenStrikes, int indicatorDelay, double scanRadius, double strikeRadius) {
        this.owner = owner;
        this.damage = damage;
        this.scanRadius = scanRadius;
        this.strikeRadius = strikeRadius;
        this.chargeUp = new ChargeUpManager(
                owner,
                50,
                Component.text("DUNG NHAM ĐANG SÔI SỤC...", NamedTextColor.GOLD, TextDecoration.BOLD),
                BossBar.Color.RED,
                BossBar.Overlay.NOTCHED_10,
                50
        );
        this.strikesPerPlayer = strikesPerPlayer;
        this.ticksBetweenStrikes = ticksBetweenStrikes;
        this.indicatorDelay = indicatorDelay;
    }

    @Override
    public void run() {
        owner.setAI(false);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (owner.isDead() || !owner.isValid()) {
                    chargeUp.remove();
                    this.cancel();
                    return;
                }

                owner.getWorld().spawnParticle(Particle.LAVA, owner.getLocation(), 3, 0.5, 0.1, 0.5, 0);

                if (chargeUp.nextTick()) {
                    owner.setAI(true);
                    chargeUp.reset();

                    for (Player target : PlayerUtils.playersInRange(owner.getLocation(), scanRadius, false)) {
                        startMultiStrike(target);
                    }
                    this.cancel();
                }
            }
        }.runTaskTimer(RogueSmpCore.getInstance(), 0, 1);
    }

    private void startMultiStrike(Player target) {
        new BukkitRunnable() {
            int strikesDone = 0;

            @Override
            public void run() {
                if (!target.isOnline() || target.isDead() || strikesDone >= strikesPerPlayer || !owner.isValid()) {
                    this.cancel();
                    return;
                }

                spawnSingleTendril(target);
                strikesDone++;
            }
        }.runTaskTimer(RogueSmpCore.getInstance(), 0, ticksBetweenStrikes);
    }

    private void spawnSingleTendril(Player target) {
        final Location strikeLoc = target.getLocation().clone();

        // Define Hitbox based on the strikeRadius config
        // Height is kept constant at 5.0 (2.5 half-extent)
        final BoundingBox tendrilHitbox = BoundingBox.of(strikeLoc, strikeRadius, 2.5, strikeRadius);

        new BukkitRunnable() {
            int delayTicks = 0;
            final int maxDelay = indicatorDelay;

            @Override
            public void run() {
                if (delayTicks >= maxDelay) {
                    performActualStrike(strikeLoc, tendrilHitbox, target);
                    this.cancel();
                    return;
                }

                // Visual Indicator matching the configured strikeRadius
                // Higher particle density for larger circles
                int particleCount = (int) (strikeRadius * 12);
                for (int i = 0; i < particleCount; i++) {
                    double angle = i * (Math.PI * 2 / particleCount);
                    double x = Math.cos(angle) * strikeRadius;
                    double z = Math.sin(angle) * strikeRadius;
                    strikeLoc.getWorld().spawnParticle(Particle.FLAME, strikeLoc.clone().add(x, 0.1, z), 1, 0, 0, 0, 0.02);
                }

                if (delayTicks % 4 == 0) {
                    strikeLoc.getWorld().playSound(strikeLoc, Sound.BLOCK_LAVA_AMBIENT, 0.8f, 1.5f);
                }

                delayTicks++;
            }
        }.runTaskTimer(RogueSmpCore.getInstance(), 0, 1);
    }

    private void performActualStrike(Location strikeLoc, BoundingBox tendrilHitbox, Player target) {
        strikeLoc.getWorld().spawnParticle(Particle.LAVA, strikeLoc, 10, strikeRadius / 2, 0.2, strikeRadius / 2, 0.1);
        strikeLoc.getWorld().playSound(strikeLoc, Sound.BLOCK_LAVA_POP, 1.2f, 0.8f);

        float modelWidth = (float) (strikeRadius * 2.0);

        ItemDisplay tendril = strikeLoc.getWorld().spawn(strikeLoc.clone().subtract(0, 4.0, 0), ItemDisplay.class, display -> {
            display.setItemStack(new ItemStack(Material.MAGMA_BLOCK));
            display.setInterpolationDuration(2);
            display.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    new Quaternionf(),
                    new Vector3f(modelWidth, 5.0f, modelWidth),
                    new Quaternionf()
            ));
            display.setBrightness(new Display.Brightness(15, 15));
        });

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks > 25 || !owner.isValid()) {
                    tendril.remove();
                    this.cancel();
                    return;
                }

                if (ticks < 4) {
                    tendril.teleport(tendril.getLocation().add(0, 1.2, 0));

                    if (ticks == 2) {
                        if (target.getBoundingBox().overlaps(tendrilHitbox)) {
                            // --- IMPACT SOUNDS & EFFECTS ---
                            // Tiếng va chạm nặng nề
                            target.getWorld().playSound(target.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 0.6f);
                            // Tiếng vỡ vụn của magma
                            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 0.8f, 0.5f);
                            // Tiếng nổ nhỏ khi tiếp xúc
                            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.2f);

//                            MovementUtils.knockAwayDirection(new Vector(0, 1.2, 0), target, 0f);
                            DamageUtils.damage(target, owner, damage / strikesPerPlayer, new DamageEvent.Metadata(DamageType.FIRE));
                            target.getWorld().spawnParticle(Particle.EXPLOSION, target.getLocation(), 1, 0, 0, 0, 0);
                        }
                    }
                }

                float wobble = (float) Math.sin(ticks * 0.8f) * 0.2f;
                tendril.setTransformation(new Transformation(
                        new Vector3f(0, 0, 0),
                        new AxisAngle4f(wobble, 1, 0, 1).get(new Quaternionf()),
                        new Vector3f(modelWidth, 5.0f, modelWidth),
                        new Quaternionf()
                ));

                ticks++;
            }
        }.runTaskTimer(RogueSmpCore.getInstance(), 0, 1);
    }

    @Override
    public int cooldownTicks() { return 250; }
}
