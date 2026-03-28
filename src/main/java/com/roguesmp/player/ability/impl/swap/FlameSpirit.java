package com.roguesmp.player.ability.impl.swap;

import com.destroystokyo.paper.ParticleBuilder;
import com.roguesmp.RogueSmpCore;
import com.roguesmp.constant.AbilityTrigger;
import com.roguesmp.constant.DamageType;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.particle.ParticleShape;
import com.roguesmp.particle.SphereShape;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.ability.Ability;
import com.roguesmp.player.ability.AbilityInfo;
import com.roguesmp.utils.DamageUtils;
import com.roguesmp.utils.ParticleUtils;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.apache.commons.math3.util.FastMath;
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

    // Level Scaling
    private static final List<Double> DAMAGE_LEVELS = List.of(8.0, 10.0, 12.0, 14.0, 18.0);
    private static final List<Double> RANGE_LEVELS = List.of(12.0, 15.0, 18.0, 22.0, 25.0);
    private static final List<Integer> BURST_LEVELS = List.of(3, 3, 4, 4, 5);
    private static final List<Integer> COOLDOWN_LEVELS = List.of(120, 110, 100, 90, 80); // 6s to 4s

    private static final List<ParticleBuilder> SPIRIT_CORE = List.of(
            new ParticleBuilder(Particle.SOUL_FIRE_FLAME).count(0).extra(0.02),
            new ParticleBuilder(Particle.FLAME).count(2).offset(0.05, 0.05, 0.05).extra(0.02)
    );

    private static final List<ParticleBuilder> SPIRIT_TRAIL = List.of(
            new ParticleBuilder(Particle.SOUL).count(1).offset(0.1, 0.1, 0.1).extra(0.02)
    );

    public static final AbilityInfo<FlameSpirit> INFO = new AbilityInfo.Builder<FlameSpirit>()
            .id(ID)
            .displayText(Component.text("Flame Spirit", NamedTextColor.GOLD, TextDecoration.BOLD))
            .descriptionProvider((p, l) -> List.of(
                    Utils.text("Triệu hồi linh hồn lửa đuổi theo kẻ địch.", NamedTextColor.GRAY),
                    Utils.text("Sát thương mỗi lần nổ: ", NamedTextColor.GRAY)
                            .append(Utils.text(Utils.formatDecimal(DAMAGE_LEVELS.get(l - 1)), NamedTextColor.RED)),
                    Utils.text("Số lần nổ: ", NamedTextColor.GRAY)
                            .append(Utils.text(BURST_LEVELS.get(l - 1) + " lần", NamedTextColor.GOLD)),
                    Utils.text("Tầm truy quét: ", NamedTextColor.GRAY)
                            .append(Utils.text(Utils.formatDecimal(RANGE_LEVELS.get(l - 1)) + "m", NamedTextColor.AQUA))
            ))
            .displayIcon(Material.MAGMA_CREAM)
            .factory(FlameSpirit::new)
            .trigger(AbilityTrigger.SWAP)
            .build();

    public FlameSpirit(SmpPlayer player, int level) { super(player, level); }

    @Override
    public void cast() {
        if (isOnCooldown()) return;

        double damage = DAMAGE_LEVELS.get(level - 1);
        double detectionRange = RANGE_LEVELS.get(level - 1);
        int maxBursts = BURST_LEVELS.get(level - 1);
        setCooldownTick(COOLDOWN_LEVELS.get(level - 1));

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
                if (!p.isOnline()) {
                    this.cancel();
                    return;
                }

                // 1. Movement Logic
                if (tick < 10) {
                    current.add(0, 0.04 * Math.sin(tick * 0.8), 0);
                } else {
                    // Target Re-acquisition
                    if (target == null || !target.isValid() || target.isDead() || current.distanceSquared(target.getLocation()) > 900) {
                        target = current.getNearbyLivingEntities(detectionRange, e -> !(e instanceof Player) && e.isValid())
                                .stream().findFirst().orElse(null);
                    }

                    if (target != null) {
                        Vector toTarget = target.getEyeLocation().toVector().subtract(current.toVector()).normalize();
                        velocity.add(toTarget.multiply(0.22)).normalize().multiply(0.7 + (level * 0.05)); // Speed scales slightly with level
                    } else {
                        velocity.multiply(0.98); // Slow drift if no target
                    }
                    current.add(velocity);
                }

                // 2. Burst Logic
                if (hitCooldown > 0) hitCooldown--;
                if (hitCooldown <= 0) {
                    Collection<LivingEntity> hits = current.getNearbyLivingEntities(1.8, e -> !(e instanceof Player));
                    if (!hits.isEmpty()) {
                        doBurst(current, hits, damage);
                        burstsDone++;
                        hitCooldown = 15; // Refractory period between bursts
                        velocity.multiply(-0.3).add(new Vector(0, 0.3, 0)); // Dynamic bounce
                    }
                }

                // 3. Termination Check
                if (tick > 160 || burstsDone >= maxBursts) {
                    doFinalExtinguish(current);
                    this.cancel();
                    return;
                }

                // 4. Visual Rendering
                current.setDirection(velocity);
                ParticleUtils.spawnShape(current, coreShape, SPIRIT_CORE, tick);
                if (tick % 2 == 0) {
                    ParticleUtils.spawnShape(current, (t) -> List.of(new Vector(0,0,0)), SPIRIT_TRAIL, tick);
                }

                tick++;
            }
        }.runTaskTimer(RogueSmpCore.getInstance(), 0, 1);
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

    @Override public @NotNull AbilityInfo<? extends Ability> getAbilityInfo() { return INFO; }
}
