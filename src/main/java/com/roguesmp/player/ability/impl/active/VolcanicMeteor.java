package com.roguesmp.player.ability.impl.active;

import com.destroystokyo.paper.ParticleBuilder;
import com.roguesmp.RogueSmpCore;
import com.roguesmp.constant.DamageType;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.particle.DoubleSpiralShape;
import com.roguesmp.particle.ParticleShape;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.ability.Ability;
import com.roguesmp.player.ability.AbilityInfo;
import com.roguesmp.player.ability.trigger.AbilityResponse;
import com.roguesmp.utils.DamageUtils;
import com.roguesmp.utils.ParticleUtils;
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

public class VolcanicMeteor extends Ability {
    public static final String ID = "volcanic_meteor";

    // Cached Attributes
    private final double impactDamage;
    private final double radius;
    private final int cooldown;

    private static final List<ParticleBuilder> METEOR_TAIL_PALETTE = List.of(
            new ParticleBuilder(Particle.FLAME).count(0).extra(0.05),
            new ParticleBuilder(Particle.SOUL_FIRE_FLAME).count(0).extra(0.02)
    );

    private static final List<ParticleBuilder> CORE_PALETTE = List.of(
            new ParticleBuilder(Particle.LAVA).count(1).offset(0.2, 0.2, 0.2),
            new ParticleBuilder(Particle.LARGE_SMOKE).count(3).offset(0.3, 0.3, 0.3).extra(0.05)
    );

    /**
     * Action-based INFO registration.
     * Scaling and triggers are now handled by the JSON data.
     */
    public static final AbilityInfo<VolcanicMeteor> INFO = new AbilityInfo<>(
            ID,
            VolcanicMeteor.class,
            VolcanicMeteor::new
    ).registerAction("execute", VolcanicMeteor::handleCast);

    public VolcanicMeteor(SmpPlayer player, int level) {
        super(player, level);
        // Cache attributes from JSON for performance during cast
        this.impactDamage = getAbilityInfo().getAttributeForLevel("impact_damage", level);
        this.radius = getAbilityInfo().getAttributeForLevel("radius", level);
        this.cooldown = (int) getAbilityInfo().getAttributeForLevel("cooldown", level);
    }

    public AbilityResponse handleCast() {
        if (isOnCooldown()) return AbilityResponse.continueChain();

        Player p = smpPlayer.getBukkitPlayer();
        World world = p.getWorld();

        // 1. Raytrace to find ground zero
        RayTraceResult ray = p.rayTraceBlocks(25);
        Location target = (ray != null) ? ray.getHitPosition().toLocation(world)
                : p.getLocation().add(p.getEyeLocation().getDirection().multiply(10));

        // 2. Setup Cooldown & Physics
        setCooldownTick(cooldown);
        Location spawnLoc = target.clone().add(5, 20, 5);
        Vector velocity = target.toVector().subtract(spawnLoc.toVector()).normalize().multiply(1.2);

        ParticleShape spiral = new DoubleSpiralShape(1.2, 0.4);
        ParticleShape core = (t) -> List.of(new Vector(0, 0, 0));

        // 3. Meteor Visual Model
        ItemDisplay meteorDisplay = world.spawn(spawnLoc, ItemDisplay.class, display -> {
            display.setItemStack(new ItemStack(Material.MAGMA_BLOCK));
            display.setBrightness(new Display.Brightness(15, 15));
            Transformation trans = display.getTransformation();
            trans.getScale().set(1.5f, 1.5f, 1.5f);
            display.setTransformation(trans);
        });

        // 4. Flight Logic
        new BukkitRunnable() {
            int tick = 0;
            final Location currentLoc = spawnLoc.clone();

            @Override
            public void run() {
                if (!p.isOnline()) {
                    meteorDisplay.remove();
                    this.cancel();
                    return;
                }

                currentLoc.add(velocity);
                meteorDisplay.teleport(currentLoc);

                // Spin the meteor
                Transformation transform = meteorDisplay.getTransformation();
                transform.getLeftRotation().rotateXYZ((float)(tick*0.1), (float)(tick*0.2), (float)(tick*0.15));
                meteorDisplay.setTransformation(transform);

                // Collision check
                if (currentLoc.getY() <= target.getY() || tick > 100) {
                    impact(currentLoc, meteorDisplay, impactDamage, radius);
                    this.cancel();
                    return;
                }

                // Render Particles
                currentLoc.setDirection(velocity);
                ParticleUtils.spawnShape(currentLoc, core, CORE_PALETTE, tick);
                ParticleUtils.spawnShape(currentLoc, spiral, METEOR_TAIL_PALETTE, tick);

                tick++;
            }
        }.runTaskTimer(RogueSmpCore.getInstance(), 0, 1);

        // Sound cues
        world.playSound(p.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 2.0f, 1.0f);
        world.playSound(p.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, 0.4f, 2.0f);

        return AbilityResponse.consume();
    }

    private void impact(Location loc, ItemDisplay display, double damage, double radius) {
        display.remove();
        World world = loc.getWorld();
        Player caster = smpPlayer.getBukkitPlayer();

        // 1. Initial Blast
        world.spawnParticle(Particle.EXPLOSION_EMITTER, loc, 3);
        world.spawnParticle(Particle.LAVA, loc, 50, 2, 0.5, 2, 0.5);

        world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.5f);
        world.playSound(loc, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.6f, 2.0f);

        // Initial Impact Damage
        for (Entity e : loc.getNearbyEntities(radius, radius, radius)) {
            if (e instanceof LivingEntity victim && !e.equals(caster)) {
                DamageUtils.damage(victim, caster, damage, new DamageEvent.Metadata(ID, DamageType.MAGIC));
                victim.setVelocity(victim.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(1.2));
            }
        }

        // 2. Lingering Volcanic Zone
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                // Visual Circle
                for (double a = 0; a < Math.PI * 2; a += Math.PI / 8) {
                    double x = Math.cos(a + ticks * 0.1) * radius;
                    double z = Math.sin(a + ticks * 0.1) * radius;
                    world.spawnParticle(Particle.FLAME, loc.clone().add(x, 0.1, z), 1, 0, 0, 0, 0.02);
                }

                // Lingering Damage (ticks every 10 ticks / 0.5s)
                if (ticks % 10 == 0) {
                    for (Entity e : loc.getNearbyEntities(radius, 2, radius)) {
                        if (e instanceof LivingEntity victim && !(e instanceof Player)) {
                            // Lingering damage is 1/5th of impact damage per tick
                            DamageUtils.damage(victim, caster, damage / 5, new DamageEvent.Metadata(ID, DamageType.MAGIC));
                            victim.setFireTicks(60);
                        }
                    }
                }

                ticks++;
                if (ticks > 100) cancel();
            }
        }.runTaskTimer(RogueSmpCore.getInstance(), 0, 1);
    }

    @Override public @NotNull AbilityInfo<?> getAbilityInfo() { return INFO; }
}
