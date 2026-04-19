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
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class Fireball extends Ability {
    public static final String ID = "fireball";

    public static final AbilityInfo<Fireball> INFO = new AbilityInfo<>(
            ID,
            Fireball.class,
            Fireball::new
    ).registerAction("execute", Fireball::handleCast);

    private final double damage;
    private final double radius;
    private final double velocity;
    private final int cooldown;

    public Fireball(SmpPlayer player, int level) {
        super(player, level);
        damage = INFO.getAttributeForLevel("damage", level);
        radius = INFO.getAttributeForLevel("radius", level);
        velocity = INFO.getAttributeForLevel("velocity", level);
        cooldown = (int) INFO.getAttributeForLevel("cooldown", level);
    }

    /**
     * This replaces the old cast() method to match the AbilityAction functional interface.
     */
    public AbilityResponse handleCast() {
        Player p = smpPlayer.getBukkitPlayer();

        // 1. Check Cooldown using the internal Ability system
        if (isOnCooldown()) {
            return AbilityResponse.continueChain();
        }

        setCooldownTick(cooldown);

        Location start = p.getEyeLocation();
        World world = start.getWorld();

        // --- Projectile Physics ---
        Item physicsItem = world.spawn(start, Item.class, item -> {
            item.setItemStack(new ItemStack(Material.MAGMA_BLOCK));
            item.setCanPlayerPickup(false);
            item.setCanMobPickup(false);
            item.setInvulnerable(true);
            item.setGravity(true);
            item.setTicksLived(1);
        });

        Vector vel = start.getDirection().normalize().multiply(velocity);
        if (vel.getY() > 0 && vel.getY() < 0.3) vel.setY(0.3);
        physicsItem.setVelocity(vel);

        // --- Projectile Loop ---
        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (!physicsItem.isValid() || tick > 100 || physicsItem.isOnGround() || hasCollided(physicsItem)) {
                    explode(physicsItem.getLocation(), damage, radius);
                    physicsItem.remove();
                    this.cancel();
                    return;
                }

                Location loc = physicsItem.getLocation();
                world.spawnParticle(Particle.FLAME, loc, 3, 0.1, 0.1, 0.1, 0.05);
                world.spawnParticle(Particle.LARGE_SMOKE, loc, 1, 0, 0, 0, 0.02);

                if (level >= 4) {
                    world.spawnParticle(Particle.LAVA, loc, 1, 0, 0, 0, 0);
                }

                tick++;
            }
        }.runTaskTimer(RogueSmpCore.getInstance(), 0, 1);

        world.playSound(start, Sound.ITEM_FIRECHARGE_USE, 1f, 0.8f);
        world.playSound(start, Sound.ENTITY_GHAST_SHOOT, 0.6f, 1.2f);

        // Tell the Loadout we consumed the click
        return AbilityResponse.consume();
    }

    private boolean hasCollided(Item item) {
        // Simple radius check for entities
        return item.getNearbyEntities(0.8, 0.8, 0.8).stream()
                .anyMatch(e -> e instanceof LivingEntity && !e.equals(smpPlayer.getBukkitPlayer()));
    }

    private void explode(Location loc, double damage, double radius) {
        Player p = smpPlayer.getBukkitPlayer();
        World world = loc.getWorld();

        // 1. Massive Visual Impact
        world.spawnParticle(Particle.EXPLOSION_EMITTER, loc, 1); // Large explosion
        world.spawnParticle(Particle.FLAME, loc, 30, 0.5, 0.5, 0.5, 0.3);
        world.spawnParticle(Particle.LAVA, loc, 15, 0.3, 0.3, 0.3, 0.1);

        world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 1.1f);

        // 2. Damage & Knockback Logic
        List<LivingEntity> targets = EntityUtils.getNearbyMobs(loc, radius, radius, radius, living -> true);

        for (LivingEntity victim : targets) {
            DamageUtils.damage(victim, p, damage, new DamageEvent.Metadata(ID, DamageType.MAGIC));

            // Scaled fire duration
            victim.setFireTicks(40 + (level * 20));

            // Knockback away from explosion center
            Vector kb = victim.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(0.5);
            victim.setVelocity(victim.getVelocity().add(kb));
        }
    }

    @Override public @NotNull AbilityInfo<?> getAbilityInfo() { return INFO; }
}
