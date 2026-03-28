package com.roguesmp.player.ability.impl.shiftswap;

import com.destroystokyo.paper.ParticleBuilder;
import com.roguesmp.RogueSmpCore;
import com.roguesmp.constant.AbilityTrigger;
import com.roguesmp.constant.DamageType;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.particle.DoubleSpiralShape;
import com.roguesmp.particle.ParticleShape;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.ability.Ability;
import com.roguesmp.player.ability.AbilityInfo;
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
import org.joml.Quaternionf;

import java.util.List;

public class VolcanicMeteor extends Ability {
    public static final String ID = "volcanic_meteor";

    // Level Scaling
    private static final List<Double> IMPACT_DAMAGE = List.of(35.0, 45.0, 55.0, 65.0, 80.0);
    private static final List<Double> RADIUS_LEVELS = List.of(5.0, 5.5, 6.0, 6.5, 7.5);
    private static final List<Integer> COOLDOWN_LEVELS = List.of(600, 540, 480, 400, 300); // 30s down to 15s

    private static final List<ParticleBuilder> METEOR_TAIL_PALETTE = List.of(
            new ParticleBuilder(Particle.FLAME).count(0).extra(0.05),
            new ParticleBuilder(Particle.SOUL_FIRE_FLAME).count(0).extra(0.02)
    );

    private static final List<ParticleBuilder> CORE_PALETTE = List.of(
            new ParticleBuilder(Particle.LAVA).count(1).offset(0.2, 0.2, 0.2),
            new ParticleBuilder(Particle.LARGE_SMOKE).count(3).offset(0.3, 0.3, 0.3).extra(0.05)
    );

    public static final AbilityInfo<VolcanicMeteor> INFO = new AbilityInfo.Builder<VolcanicMeteor>()
            .id(ID)
            .displayText(Component.text("Volcanic Meteor", NamedTextColor.GOLD, TextDecoration.BOLD))
            .descriptionProvider((p, l) -> List.of(
                    Utils.text("Triệu hồi một thiên thạch dung nham từ bầu trời.", NamedTextColor.GRAY),
                    Utils.text("Sát thương va chạm: ", NamedTextColor.GRAY)
                            .append(Utils.text(Utils.formatDecimal(IMPACT_DAMAGE.get(l - 1)), NamedTextColor.RED)),
                    Utils.text("Bán kính nổ: ", NamedTextColor.GRAY)
                            .append(Utils.text(Utils.formatDecimal(RADIUS_LEVELS.get(l - 1)) + "m", NamedTextColor.YELLOW)),
                    Utils.text("Hồi chiêu: ", NamedTextColor.GRAY)
                            .append(Utils.text(Utils.formatDecimal(COOLDOWN_LEVELS.get(l - 1) / 20.0) + "s", NamedTextColor.GREEN))
            ))
            .displayIcon(Material.MAGMA_BLOCK)
            .factory(VolcanicMeteor::new)
            .trigger(AbilityTrigger.SHIFT_SWAP)
            .build();

    public VolcanicMeteor(SmpPlayer player, int level) { super(player, level); }

    @Override
    public void cast() {
        if (isOnCooldown()) return;

        double impactDmg = IMPACT_DAMAGE.get(level - 1);
        double radius = RADIUS_LEVELS.get(level - 1);
        setCooldownTick(COOLDOWN_LEVELS.get(level - 1));

        Player p = smpPlayer.getBukkitPlayer();
        World world = p.getWorld();

        RayTraceResult ray = p.rayTraceBlocks(25);
        Location target = (ray != null) ? ray.getHitPosition().toLocation(world)
                : p.getLocation().add(p.getEyeLocation().getDirection().multiply(10));

        // Physics setup
        Location spawnLoc = target.clone().add(5, 20, 5);
        Vector velocity = target.toVector().subtract(spawnLoc.toVector()).normalize().multiply(1.2);

        ParticleShape spiral = new DoubleSpiralShape(1.2, 0.4);
        ParticleShape core = (t) -> List.of(new Vector(0, 0, 0));

        ItemDisplay meteorDisplay = world.spawn(spawnLoc, ItemDisplay.class, display -> {
            display.setItemStack(new ItemStack(Material.MAGMA_BLOCK));
            display.setBrightness(new Display.Brightness(15, 15));
            Transformation trans = display.getTransformation();
            trans.getScale().set(1.5f, 1.5f, 1.5f);
            display.setTransformation(trans);
        });

        new BukkitRunnable() {
            int tick = 0;
            final Location currentLoc = spawnLoc.clone();

            @Override
            public void run() {
                currentLoc.add(velocity);
                meteorDisplay.teleport(currentLoc);

                Transformation transform = meteorDisplay.getTransformation();
                transform.getLeftRotation().rotateXYZ((float)(tick*0.1), (float)(tick*0.2), (float)(tick*0.15));
                meteorDisplay.setTransformation(transform);

                if (currentLoc.getY() <= target.getY() || currentLoc.getBlock().getType().isSolid() || tick > 100) {
                    impact(currentLoc, meteorDisplay, impactDmg, radius);
                    this.cancel();
                    return;
                }

                currentLoc.setDirection(velocity);
                ParticleUtils.spawnShape(currentLoc, core, CORE_PALETTE, tick);
                ParticleUtils.spawnShape(currentLoc, spiral, METEOR_TAIL_PALETTE, tick);

                tick++;
            }
        }.runTaskTimer(RogueSmpCore.getInstance(), 0, 1);

        world.playSound(p.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 2.0f, 1.0f);
        world.playSound(p.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, 0.4f, 2.0f);
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
                        if (e instanceof LivingEntity victim && !e.equals(caster)) {
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

    @Override public @NotNull AbilityInfo<? extends Ability> getAbilityInfo() { return INFO; }
}
