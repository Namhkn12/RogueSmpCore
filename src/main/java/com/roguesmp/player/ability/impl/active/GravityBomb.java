package com.roguesmp.player.ability.impl.active;

import com.destroystokyo.paper.ParticleBuilder;
import com.roguesmp.RogueSmpCore;
import com.roguesmp.constant.DamageType;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.ability.Ability;
import com.roguesmp.player.ability.AbilityInfo;
import com.roguesmp.player.ability.trigger.AbilityResponse;
import com.roguesmp.utils.DamageUtils;
import com.roguesmp.utils.Hitbox;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class GravityBomb extends Ability {
    public static final String ID = "gravity_bomb";

    // Cached Attributes
    private final double radius;
    private final double damage;
    private final int cooldown;
    private final double velocity = 1.1;

    /**
     * Action-based INFO shell.
     */
    public static final AbilityInfo<GravityBomb> INFO = new AbilityInfo<>(
            ID,
            GravityBomb.class,
            GravityBomb::new
    ).registerAction("execute", GravityBomb::handleCast);

    public GravityBomb(SmpPlayer player, int level) {
        super(player, level);
        // Cache attributes from the Info/JSON on initialization
        this.radius = getAbilityInfo().getAttributeForLevel("radius", level);
        this.damage = getAbilityInfo().getAttributeForLevel("damage", level);
        this.cooldown = (int) getAbilityInfo().getAttributeForLevel("cooldown", level);
    }

    /**
     * Replaced cast() with the new action-mapped handleCast().
     */
    public AbilityResponse handleCast() {
        if (isOnCooldown()) return AbilityResponse.continueChain();

        Player player = smpPlayer.getBukkitPlayer();
        Location eyeLocation = player.getEyeLocation();
        World world = player.getWorld();

        // 1. Set Cooldown using cached value
        setCooldownTick(cooldown);

        // 2. Physics & Sound
        Vector vel = eyeLocation.getDirection().normalize().multiply(velocity);
        if (vel.getY() > 0 && vel.getY() < 0.2) vel.setY(0.2);

        world.playSound(eyeLocation, Sound.ENTITY_SHULKER_SHOOT, SoundCategory.PLAYERS, 1.0f, 1.8f);
        world.playSound(eyeLocation, Sound.ENTITY_IRON_GOLEM_HURT, SoundCategory.PLAYERS, 1.0f, 1.8f);

        // 3. Projectile Setup
        Item physicsItem = (Item) world.spawnEntity(eyeLocation, EntityType.ITEM);
        Slime grenade = (Slime) world.spawnEntity(eyeLocation, EntityType.SLIME);
        ItemStack itemStack = new ItemStack(Material.GUNPOWDER);

        grenade.setSize(1);
        grenade.setSilent(true);
        physicsItem.setItemStack(itemStack);
        physicsItem.setCanPlayerPickup(false);
        physicsItem.setCanMobPickup(false);
        physicsItem.setVelocity(vel);
        physicsItem.setInvulnerable(true);
        physicsItem.addPassenger(grenade);

        new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cleanup();
                    return;
                }

                // Collision or Expiration check
                if (!grenade.isValid() || physicsItem.isOnGround() || tick > 120 || grenade.isInLava() || hasCollidedWithEnemy(grenade)) {
                    Location explodeLoc = grenade.getLocation();
                    cleanup();
                    explode(explodeLoc, damage, radius);
                    return;
                }

                // Visuals (Optional: Add gravity pull particles here)
                tick++;
            }

            private void cleanup() {
                grenade.remove();
                physicsItem.remove();
                this.cancel();
            }
        }.runTaskTimer(RogueSmpCore.getInstance(), 0, 1);

        return AbilityResponse.consume();
    }

    private boolean hasCollidedWithEnemy(Slime grenade) {
        Hitbox hitbox = new Hitbox.AABBHitbox(grenade.getWorld(), grenade.getBoundingBox());
        return !hitbox.getHitMobs(grenade).isEmpty();
    }

    private void explode(Location location, double damage, double radius) {
        World world = location.getWorld();
        world.playSound(location, Sound.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 1.5f, 2.0f);
        world.playSound(location, Sound.BLOCK_BELL_RESONATE, SoundCategory.PLAYERS, 1.5f, 2.0f);
        world.playSound(location, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, SoundCategory.PLAYERS, 1.5f, 0.5f);
        world.playSound(location, Sound.ENTITY_IRON_GOLEM_DEATH, SoundCategory.PLAYERS, 1.5f, 1.2f);
        world.playSound(location, Sound.ENTITY_WARDEN_ATTACK_IMPACT, SoundCategory.PLAYERS, 1.5f, 0.5f);
        world.playSound(location, Sound.ENTITY_WARDEN_SONIC_CHARGE, SoundCategory.PLAYERS, 1.5f, 1.2f);

        world.spawnParticle(Particle.PORTAL, location, 150, 0, 0, 0, 7);
        new ParticleBuilder(Particle.SQUID_INK).count(30).offset(5, 5, 5).location(location).spawn();
        new ParticleBuilder(Particle.FALLING_DUST).count(30).offset(5, 5, 5).data(Material.GRAY_CONCRETE.createBlockData()).location(location).spawn();
        world.spawnParticle(Particle.FLASH, location, 1, Color.AQUA);

        // Logic using scaled radius and damage
        List<LivingEntity> mobs = new Hitbox.SphereHitbox(location, radius).getHitMobs();
        for (LivingEntity mob : mobs) {
            DamageUtils.damage(mob, smpPlayer.getBukkitPlayer(), damage, new DamageEvent.Metadata(ID, DamageType.PROJECTILE_ABILITY));

            Vector dir = mob.getLocation().subtract(location.toVector()).toVector().multiply(-0.4);
            if (dir.getY() < 0 && mob.isOnGround()) {
                dir.setY(0.5f);
            }
            mob.setVelocity(dir);
        }
    }

    @Override
    public @NotNull AbilityInfo<?> getAbilityInfo() {
        return INFO;
    }
}
