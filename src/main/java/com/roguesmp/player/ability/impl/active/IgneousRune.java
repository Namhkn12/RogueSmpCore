package com.roguesmp.player.ability.impl.active;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.constant.DamageType;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.ability.Ability;
import com.roguesmp.player.ability.AbilityInfo;
import com.roguesmp.player.ability.trigger.AbilityResponse;
import com.roguesmp.utils.DamageUtils;
import com.roguesmp.utils.EntityUtils;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class IgneousRune extends Ability {
    public static final String ID = "igneous_rune";

    private final double damage;
    private final double radius;
    private final int cooldown;

    public static final AbilityInfo<IgneousRune> INFO = new AbilityInfo<>(
            ID,
            IgneousRune.class,
            IgneousRune::new
    ).registerAction("execute", IgneousRune::handleCast);

    public IgneousRune(SmpPlayer player, int level) {
        super(player, level);
        this.damage = getAbilityInfo().getAttributeForLevel("damage", level);
        this.radius = getAbilityInfo().getAttributeForLevel("radius", level);
        this.cooldown = (int) getAbilityInfo().getAttributeForLevel("cooldown", level);
    }

    public AbilityResponse handleCast() {
        if (isOnCooldown()) return AbilityResponse.continueChain();

        Player p = smpPlayer.getBukkitPlayer();
        RayTraceResult ray = p.getWorld().rayTraceBlocks(p.getEyeLocation(), p.getEyeLocation().getDirection(), 15);

        // Position logic
        Location runeLoc = (ray != null && ray.getHitBlock() != null)
                ? ray.getHitBlock().getLocation().add(0.5, 1.1, 0.5)
                : p.getLocation().add(0, 0.1, 0);

        setCooldownTick(cooldown);
        spawnRune(runeLoc, p);
        p.getWorld().playSound(runeLoc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 0.5f);

        return AbilityResponse.consume();
    }

    private void spawnRune(Location loc, Player caster) {
        World world = loc.getWorld();
        final int PREPARE_TIME = 20;
        final int MAX_DURATION = 300;

        ItemDisplay core = world.spawn(loc, ItemDisplay.class, display -> {
            display.setItemStack(new ItemStack(Material.MAGMA_CREAM));
            display.setBrightness(new Display.Brightness(15, 15));
            Transformation trans = display.getTransformation();
            trans.getScale().set(0.5f, 0.5f, 0.5f);
            display.setTransformation(trans);
        });

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks > MAX_DURATION || !core.isValid() || !caster.isOnline()) {
                    core.remove();
                    this.cancel();
                    return;
                }

                drawRuneCircle(loc, ticks, PREPARE_TIME);

                // Spin visual
                Transformation trans = core.getTransformation();
                trans.getLeftRotation().rotateY(0.15f);
                core.setTransformation(trans);

                if (ticks == PREPARE_TIME) {
                    world.spawnParticle(Particle.FLASH, loc, 1);
                    world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_CHIME, 0.8f, 2.0f);
                }

                if (ticks > PREPARE_TIME) {
                    double detectionRadius = 2.0;
                    List<LivingEntity> enemies = EntityUtils.getNearbyMobs(loc, detectionRadius, 1, detectionRadius, living -> true);

                    if (!enemies.isEmpty()) {
                        detonate(loc, caster);
                        core.remove();
                        this.cancel();
                    }
                }
                ticks++;
            }
        }.runTaskTimer(RogueSmpCore.getInstance(), 0, 1);
    }

    private void drawRuneCircle(Location loc, int ticks, int prepareTime) {
        double circleRadius = 3.0;
        World world = loc.getWorld();

        if (ticks <= prepareTime) {
            double startAngle = Math.PI * 2 * ((double) (ticks - 1) / prepareTime);
            double endAngle = Math.PI * 2 * ((double) ticks / prepareTime);

            for (double i = startAngle; i < endAngle; i += Math.PI / 32) {
                double x = Math.cos(i) * circleRadius;
                double z = Math.sin(i) * circleRadius;
                world.spawnParticle(Particle.SOUL_FIRE_FLAME, loc.clone().add(x, 0, z), 1, 0, 0, 0, 0);
            }
        } else if (ticks % 2 == 0) {
            for (int j = 0; j < 3; j++) {
                double randomAngle = Math.random() * Math.PI * 2;
                double x = Math.cos(randomAngle) * circleRadius;
                double z = Math.sin(randomAngle) * circleRadius;
                world.spawnParticle(Particle.FLAME, loc.clone().add(x, 0, z), 1, 0, 0, 0, 0.02);
            }
        }
    }

    private void detonate(Location loc, Player p) {
        World world = loc.getWorld();

        // Pillar Visual
        for (int i = 0; i < 6; i++) {
            double yOffset = i * 0.7;
            world.spawnParticle(Particle.EXPLOSION, loc.clone().add(0, yOffset, 0), 1);
            world.spawnParticle(Particle.FLAME, loc.clone().add(0, yOffset, 0), 15, 0.4, 0.4, 0.4, 0.1);
            world.spawnParticle(Particle.LAVA, loc.clone().add(0, yOffset, 0), 5, 0.2, 0.2, 0.2, 0.05);
        }

        world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.7f);
        world.playSound(loc, Sound.ITEM_FIRECHARGE_USE, 1f, 0.5f);
        world.playSound(loc, Sound.BLOCK_FIRE_AMBIENT, 1.5f, 1.5f);

        for (LivingEntity e : EntityUtils.getNearbyMobs(loc, radius, 2.0, radius, living -> true)) {
            DamageUtils.damage(e, p, damage, new DamageEvent.Metadata(ID, DamageType.MAGIC));

            // Scaled knock-up and burn
            e.setVelocity(e.getVelocity().add(new Vector(0, 0.7 + (level * 0.05), 0)));
            e.setFireTicks(40 + (level * 20));
        }
    }

    @Override public @NotNull AbilityInfo getAbilityInfo() { return INFO; }
}