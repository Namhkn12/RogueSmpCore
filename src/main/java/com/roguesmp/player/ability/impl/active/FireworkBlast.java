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
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

public class FireworkBlast extends Ability {
    public static final String ID = "firework_blast";

    private static final double RADIUS = 5.0;

    private static final List<ParticleBuilder> SPIRAL_PALETTE = List.of(
            new ParticleBuilder(Particle.FIREWORK).count(0),
            new ParticleBuilder(Particle.ELECTRIC_SPARK).count(0)
    );

    private static final List<ParticleBuilder> ENGINE_PALETTE = List.of(
            new ParticleBuilder(Particle.FLAME).count(3).offset(0.05, 0.05, 0.05).extra(0.02),
            new ParticleBuilder(Particle.CAMPFIRE_COSY_SMOKE).count(1).offset(0.02, 0.02, 0.02).extra(0.01)
    );

    /**
     * Using the new constructor style.
     * Scaling and Metadata are handled by JSON.
     */
    public static final AbilityInfo<FireworkBlast> INFO = new AbilityInfo<>(
            ID,
            FireworkBlast.class,
            FireworkBlast::new
    ).registerAction("execute", FireworkBlast::handleCast);

    private final double baseDamage;
    private final double damageCap;
    private final int cooldown;

    public FireworkBlast(SmpPlayer player, int level) {
        super(player, level);
        baseDamage = INFO.getAttributeForLevel("base_damage", level);
        damageCap = INFO.getAttributeForLevel("damage_cap", level);
        cooldown = (int) INFO.getAttributeForLevel("cooldown", level);
    }

    public AbilityResponse handleCast() {
        if (isOnCooldown()) return AbilityResponse.continueChain();

        setCooldownTick(cooldown);

        Player p = smpPlayer.getBukkitPlayer();
        Location currentLoc = p.getEyeLocation();
        Vector velocity = currentLoc.getDirection().multiply(1.0);
        World world = currentLoc.getWorld();

        ParticleShape spiral = new DoubleSpiralShape(0.7, 0.3);
        ParticleShape core = (t) -> List.of(new Vector(0, 0, 0));

        world.playSound(currentLoc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1f, 1f);

        new BukkitRunnable() {
            int tick = 0;
            final Location start = currentLoc.clone();

            @Override
            public void run() {
                currentLoc.add(velocity);

                // Collision with blocks
                if (currentLoc.getBlock().getType().isSolid()) {
                    explode(currentLoc, start, tick, baseDamage, damageCap);
                    this.cancel();
                    return;
                }

                // Collision with entities
                Collection<LivingEntity> targets = currentLoc.getNearbyLivingEntities(0.6, e -> !(e instanceof Player));

                if (!targets.isEmpty() || tick > 100) {
                    explode(currentLoc, start, tick, baseDamage, damageCap);
                    this.cancel();
                    return;
                }

                // Visuals
                ParticleUtils.spawnShape(currentLoc, core, ENGINE_PALETTE, tick);
                ParticleUtils.spawnShape(currentLoc, spiral, SPIRAL_PALETTE, tick);

                Vector exhaust = velocity.clone().normalize().multiply(-0.2);
                world.spawnParticle(Particle.FIREWORK, currentLoc, 2,
                        exhaust.getX(), exhaust.getY(), exhaust.getZ(), 0.1);

                tick++;
            }
        }.runTaskTimer(RogueSmpCore.getInstance(), 0, 1);

        return AbilityResponse.consume();
    }

    private void explode(Location loc, Location start, int ticks, double baseDamage, double damageCap) {
        Player p = smpPlayer.getBukkitPlayer();
        double dist = start.distance(loc);

        // Scaling remains: 5% increase per block travel past 6 blocks
        double mult = 1.0 + (Math.max(0, dist - 6) * 0.05);
        double damage = Math.min(baseDamage * mult, damageCap);

        World world = loc.getWorld();
        world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, SoundCategory.PLAYERS, 2.0f, 1.2f);
        world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.PLAYERS, 2.0f, 0.5f);
        world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, SoundCategory.PLAYERS, 2.0f, 1.0f);

        Color randomColor = List.of(Color.WHITE, Color.GRAY, Color.fromRGB(0, 0, 0)).get(Utils.RANDOM.nextInt(0, 3));
        Firework rocket = (Firework) world.spawnEntity(loc, EntityType.FIREWORK_ROCKET);
        FireworkEffect effect = FireworkEffect.builder().with(FireworkEffect.Type.BALL).withColor(randomColor).build();
        FireworkMeta meta = rocket.getFireworkMeta();
        meta.addEffect(effect);
        rocket.setFireworkMeta(meta);
        rocket.detonate();
        world.spawnParticle(Particle.FLASH, loc, 1, randomColor);

        for (Entity e : loc.getNearbyEntities(RADIUS, RADIUS, RADIUS)) {
            if (e instanceof LivingEntity victim && !e.equals(p)) {
                DamageUtils.damage(victim, p, damage, new DamageEvent.Metadata(ID, DamageType.PROJECTILE_ABILITY));
                Vector kb = victim.getLocation().subtract(loc.toVector()).toVector().multiply(0.4);
                victim.setVelocity(kb);
            }
        }

        int extraCount = (ticks > 8) ? 3 : (ticks > 3) ? 2 : 0;
        if (extraCount > 0) {
            new BukkitRunnable() {
                int count = 0;
                @Override
                public void run() {
                    Location offsetLoc = loc.clone().add(Math.random() * 2, Math.random() * 0.5, Math.random() * 2);

                    Color randomColor = List.of(Color.WHITE, Color.GRAY, Color.fromRGB(0, 0, 0)).get(Utils.RANDOM.nextInt(0, 3));
                    Firework rocket = (Firework) world.spawnEntity(loc, EntityType.FIREWORK_ROCKET);
                    FireworkEffect effect = FireworkEffect.builder().with(FireworkEffect.Type.BALL).withColor(randomColor).build();
                    FireworkMeta meta = rocket.getFireworkMeta();
                    meta.addEffect(effect);
                    rocket.setFireworkMeta(meta);
                    rocket.detonate();

                    offsetLoc.getWorld().playSound(offsetLoc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1f, 1.2f);
                    offsetLoc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, offsetLoc, 10, 0.2, 0.2, 0.2, 0.1);
                    world.spawnParticle(Particle.FLASH, offsetLoc, 1, randomColor);

                    count++;
                    if (count >= extraCount) cancel();
                }
            }.runTaskTimer(RogueSmpCore.getInstance(), 5, 2);
        }
    }

    @Override public @NotNull AbilityInfo<?> getAbilityInfo() { return INFO; }
}
