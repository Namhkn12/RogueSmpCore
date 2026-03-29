package com.roguesmp.player.ability.impl.swap;

import com.destroystokyo.paper.ParticleBuilder;
import com.roguesmp.RogueSmpCore;
import com.roguesmp.constant.AbilityTrigger;
import com.roguesmp.constant.DamageType;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.ability.Ability;
import com.roguesmp.player.ability.AbilityInfo;
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

    // Level Scaling Lists
    public static final List<Integer> DAMAGES = List.of(10, 20, 30, 40, 55);
    public static final List<Double> RADIUS_LEVELS = List.of(8.0, 10.0, 12.0, 14.0, 16.0);
    public static final List<Integer> COOLDOWN_LEVELS = List.of(160, 140, 120, 100, 80); // 8s down to 4s

    public static final double BASE_VELOCITY = 1.1;

    public static final AbilityInfo<GravityBomb> INFO = new AbilityInfo.Builder<GravityBomb>()
            .id(ID)
            .descriptionProvider((p, level) -> List.of(
                    Utils.text("Thả bom trọng lực hút kẻ địch, gây ", NamedTextColor.GRAY)
                            .append(Utils.text(DAMAGES.get(level - 1) + " sát thương", NamedTextColor.BLUE)),
                    Utils.text("Bán kính: ", NamedTextColor.GRAY)
                            .append(Utils.text(Utils.formatDecimal(RADIUS_LEVELS.get(level - 1)) + "m", NamedTextColor.AQUA))
            ))
            .displayText(Component.text("Gravity Bomb", NamedTextColor.BLUE, TextDecoration.BOLD))
            .displayIcon(Material.TNT)
            .factory(GravityBomb::new)
            .trigger(AbilityTrigger.SWAP)
            .build();

    public GravityBomb(SmpPlayer player, int level) {
        super(player, level);
    }

    @Override
    public void cast() {
        Player player = smpPlayer.getBukkitPlayer();
        Location eyeLocation = player.getEyeLocation();

        // Use level-scaled values
        double radius = RADIUS_LEVELS.get(level - 1);
        int damage = DAMAGES.get(level - 1);
        setCooldownTick(COOLDOWN_LEVELS.get(level - 1));

        Vector vel = eyeLocation.getDirection().normalize().multiply(BASE_VELOCITY);
        double velY = vel.getY();
        if (velY > 0 && velY < 0.2) {
            vel.setY(0.2);
        }

        World world = player.getWorld();
        world.playSound(eyeLocation, Sound.ENTITY_SHULKER_SHOOT, SoundCategory.PLAYERS, 1.0f, 1.8f);
        world.playSound(eyeLocation, Sound.ENTITY_IRON_GOLEM_HURT, SoundCategory.PLAYERS, 1.0f, 1.8f);

        Item physicsItem = (Item) world.spawnEntity(eyeLocation, EntityType.ITEM);
        Slime grenade = (Slime) world.spawnEntity(eyeLocation, EntityType.SLIME);
        ItemStack itemStack = ItemStack.of(Material.GUNPOWDER);

        grenade.setSize(1);
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
                    grenade.remove();
                    physicsItem.remove();
                    this.cancel();
                    return;
                }

                if (!grenade.isValid() || physicsItem.isOnGround() || tick > 120 || grenade.isInLava() || hasCollidedWithEnemy(grenade)) {
                    Location location = grenade.getLocation();
                    grenade.remove();
                    physicsItem.remove();
                    explode(location, damage, radius);
                    this.cancel();
                    return;
                }
                tick++;
            }
        }.runTaskTimer(RogueSmpCore.getInstance(), 0, 1);
    }

    private boolean hasCollidedWithEnemy(Slime grenade) {
        Hitbox hitbox = new Hitbox.AABBHitbox(grenade.getWorld(), grenade.getBoundingBox());
        return !hitbox.getHitMobs(grenade).isEmpty();
    }

    private void explode(Location location, int damage, double radius) {
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
    public @NotNull AbilityInfo<? extends Ability> getAbilityInfo() {
        return INFO;
    }
}
