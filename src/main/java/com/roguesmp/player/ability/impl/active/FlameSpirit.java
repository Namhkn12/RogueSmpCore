package com.roguesmp.player.ability.impl.active;

import com.destroystokyo.paper.ParticleBuilder;
import com.roguesmp.RogueSmpCore;
import com.roguesmp.constant.DamageType;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.particle.ParticleShape;
import com.roguesmp.particle.SphereShape;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.ability.Ability;
import com.roguesmp.player.ability.AbilityInfo;
import com.roguesmp.player.ability.trigger.AbilityResponse;
import com.roguesmp.utils.DamageUtils;
import com.roguesmp.utils.ParticleUtils;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

public class FlameSpirit extends Ability {
    public static final String ID = "flame_spirit";

    private static final List<ParticleBuilder> SPIRIT_CORE = List.of(
            new ParticleBuilder(Particle.SOUL_FIRE_FLAME).count(0).extra(0.02),
            new ParticleBuilder(Particle.FLAME).count(2).offset(0.05, 0.05, 0.05).extra(0.02)
    );

    private static final List<ParticleBuilder> SPIRIT_TRAIL = List.of(
            new ParticleBuilder(Particle.SOUL).count(1).offset(0.1, 0.1, 0.1).extra(0.02)
    );

    /**
     * Data-driven INFO shell.
     * Description and Scaling are now in the JSON.
     */
    public static final AbilityInfo<FlameSpirit> INFO = new AbilityInfo<>(
            ID,
            FlameSpirit.class,
            FlameSpirit::new
    ).registerAction("execute", FlameSpirit::handleCast);

    private final double damage;
    private final double detectionRange;
    private final int maxBursts;
    private final int cooldown;

    public FlameSpirit(SmpPlayer player, int level) {
        super(player, level);
        damage = getAbilityInfo().getAttributeForLevel("damage", level);
        detectionRange = getAbilityInfo().getAttributeForLevel("range", level);
        maxBursts = (int) getAbilityInfo().getAttributeForLevel("burst_count", level);
        cooldown = (int) getAbilityInfo().getAttributeForLevel("cooldown", level);
    }

    public AbilityResponse handleCast() {
        if (isOnCooldown()) return AbilityResponse.continueChain();

        setCooldownTick(cooldown);

        Player p = smpPlayer.getBukkitPlayer();
        World world = p.getWorld();
        Location current = p.getEyeLocation();

        final Vector velocity = current.getDirection().clone().multiply(0.2);
        ParticleShape coreShape = new SphereShape(0.4, 10);

        world.playSound(current, Sound.ENTITY_VEX_CHARGE, 1f, 0.6f);

        new BukkitRunnable() {
            int tick = 0;
            int burstsDone = 0;
            int hitCooldown = 0;
            LivingEntity target = null;

            @Override
            public void run() {
                if (!p.isOnline() || !p.isValid()) {
                    this.cancel();
                    return;
                }

                // --- Movement Logic ---
                if (tick < 10) {
                    current.add(0, 0.04 * Math.sin(tick * 0.8), 0);
                } else {
                    if (target == null || !target.isValid() || target.isDead() || current.distanceSquared(target.getLocation()) > 900) {
                        target = current.getNearbyLivingEntities(detectionRange, e -> !(e instanceof Player) && e.isValid())
                                .stream().findFirst().orElse(null);
                    }

                    if (target != null) {
                        Vector toTarget = target.getEyeLocation().toVector().subtract(current.toVector()).normalize();
                        // Speed scales slightly based on level index
                        velocity.add(toTarget.multiply(0.22)).normalize().multiply(0.7 + (level * 0.05));
                    } else {
                        velocity.multiply(0.98);
                    }
                    current.add(velocity);
                }

                // --- Burst Logic ---
                if (hitCooldown > 0) hitCooldown--;
                if (hitCooldown <= 0) {
                    Collection<LivingEntity> hits = current.getNearbyLivingEntities(1.8, e -> !(e instanceof Player));
                    if (!hits.isEmpty()) {
                        doBurst(current, hits, damage);
                        burstsDone++;
                        hitCooldown = 15;
                        velocity.multiply(-0.3).add(new Vector(0, 0.3, 0));
                    }
                }

                // --- Termination ---
                if (tick > 160 || burstsDone >= maxBursts) {
                    doFinalExtinguish(current);
                    this.cancel();
                    return;
                }

                // --- Visuals ---
                current.setDirection(velocity);
                ParticleUtils.spawnShape(current, coreShape, SPIRIT_CORE, tick);
                if (tick % 2 == 0) {
                    ParticleUtils.spawnShape(current, (t) -> List.of(new Vector(0,0,0)), SPIRIT_TRAIL, tick);
                }

                tick++;
            }
        }.runTaskTimer(RogueSmpCore.getInstance(), 0, 1);

        return AbilityResponse.consume();
    }

    private void doBurst(Location loc, Collection<LivingEntity> targets, double damage) {
        World world = loc.getWorld();
        Player p = smpPlayer.getBukkitPlayer();

        world.spawnParticle(Particle.EXPLOSION, loc, 1);
        world.spawnParticle(Particle.FLAME, loc, 20, 0.4, 0.4, 0.4, 0.15);
        world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1f, 1.3f);
        world.playSound(loc, Sound.ENTITY_GENERIC_BURN, 0.8f, 1.8f);

        for (LivingEntity victim : targets) {
            DamageUtils.damage(victim, p, damage, new DamageEvent.Metadata(ID, DamageType.MAGIC));
            victim.setFireTicks(60); // 3 seconds of fire
        }
    }

    private void doFinalExtinguish(Location loc) {
        World world = loc.getWorld();
        world.spawnParticle(Particle.LARGE_SMOKE, loc, 15, 0.3, 0.3, 0.3, 0.05);
        world.spawnParticle(Particle.SOUL, loc, 8, 0.2, 0.2, 0.2, 0.1);
        world.playSound(loc, Sound.BLOCK_FIRE_EXTINGUISH, 1f, 0.5f);
        world.playSound(loc, Sound.ENTITY_VEX_DEATH, 0.7f, 0.5f);
    }

    @Override public @NotNull AbilityInfo<?> getAbilityInfo() { return INFO; }
}
