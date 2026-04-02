package com.roguesmp.player.ability.impl.rightclick;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.constant.AbilityTrigger;
import com.roguesmp.constant.DamageType;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.ability.Ability;
import com.roguesmp.player.ability.AbilityInfo;
import com.roguesmp.utils.DamageUtils;
import com.roguesmp.utils.EntityUtils;
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

public class Fireball extends Ability {
    public static final String ID = "fireball";

    // Level Scaling Lists (1-5)
    private static final List<Double> DAMAGE_LEVELS = List.of(10.0, 14.0, 18.0, 22.0, 28.0);
    private static final List<Double> RADIUS_LEVELS = List.of(3.0, 3.5, 4.0, 4.5, 5.5);
    private static final List<Double> VELOCITY_LEVELS = List.of(1.1, 1.3, 1.5, 1.7, 2.0);
    private static final List<Integer> COOLDOWN_LEVELS = List.of(60, 50, 40, 35, 30); // Ticks

    public static final AbilityInfo<Fireball> INFO = new AbilityInfo.Builder<Fireball>()
            .id(ID)
            .displayText(Component.text("Quả Cầu Lửa", NamedTextColor.GOLD, TextDecoration.BOLD))
            .descriptionProvider((p, level) -> List.of(
                    Utils.text("Ném một khối dung nham nổ tung khi va chạm.", NamedTextColor.GRAY),
                    Utils.text("Sát thương: ", NamedTextColor.GRAY)
                            .append(Utils.text(Utils.formatDecimal(DAMAGE_LEVELS.get(level - 1)), NamedTextColor.RED)),
                    Utils.text("Bán kính nổ: ", NamedTextColor.GRAY)
                            .append(Utils.text(Utils.formatDecimal(RADIUS_LEVELS.get(level - 1)) + "m", NamedTextColor.YELLOW)),
                    Utils.text("Hồi chiêu: ", NamedTextColor.GRAY)
                            .append(Utils.text(Utils.formatDecimal(COOLDOWN_LEVELS.get(level - 1) / 20.0) + "s", NamedTextColor.GREEN))
            ))
            .displayIcon(Material.FIRE_CHARGE)
            .factory(Fireball::new)
            .trigger(AbilityTrigger.RIGHT_CLICK)
            .build();

    public Fireball(SmpPlayer player, int level) { super(player, level); }

    @Override
    public void cast() {
        Player p = smpPlayer.getBukkitPlayer();
        if (isOnCooldown()) return;

        // Scaling variables
        double damage = DAMAGE_LEVELS.get(level - 1);
        double radius = RADIUS_LEVELS.get(level - 1);
        double velocity = VELOCITY_LEVELS.get(level - 1);
        setCooldownTick(COOLDOWN_LEVELS.get(level - 1));

        Location start = p.getEyeLocation();
        World world = start.getWorld();

        // 1. Projectile Physics
        Item physicsItem = world.spawn(start, Item.class, item -> {
            item.setItemStack(new ItemStack(Material.MAGMA_BLOCK));
            item.setCanPlayerPickup(false);
            item.setCanMobPickup(false);
            item.setInvulnerable(true);
            item.setGravity(true);
            item.setTicksLived(1); // Prevent instant despawn
        });

        Vector vel = start.getDirection().normalize().multiply(velocity);
        if (vel.getY() > 0 && vel.getY() < 0.3) vel.setY(0.3); // Slight lift
        physicsItem.setVelocity(vel);

        // 2. Projectile Loop
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

                // Fireball Visuals
                Location loc = physicsItem.getLocation();
                world.spawnParticle(Particle.FLAME, loc, 3, 0.1, 0.1, 0.1, 0.05);
                world.spawnParticle(Particle.LARGE_SMOKE, loc, 1, 0, 0, 0, 0.02);

                // Add a "Glow" effect at high levels
                if (level >= 4) {
                    world.spawnParticle(Particle.LAVA, loc, 1, 0, 0, 0, 0);
                }

                tick++;
            }
        }.runTaskTimer(RogueSmpCore.getInstance(), 0, 1);

        world.playSound(start, Sound.ITEM_FIRECHARGE_USE, 1f, 0.8f);
        world.playSound(start, Sound.ENTITY_GHAST_SHOOT, 0.6f, 1.2f);
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

    @Override public @NotNull AbilityInfo<? extends Ability> getAbilityInfo() { return INFO; }
}
