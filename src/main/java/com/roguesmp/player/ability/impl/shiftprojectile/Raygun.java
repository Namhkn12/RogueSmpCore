package com.roguesmp.player.ability.impl.shiftprojectile;

import com.destroystokyo.paper.ParticleBuilder;
import com.roguesmp.constant.AbilityTrigger;
import com.roguesmp.constant.DamageType;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.particle.LineShape;
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
import org.apache.commons.math3.util.FastMath;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class Raygun extends Ability {
    public static final String ID = "ray_gun";

    // Level Scaling Lists (1-5)
    private static final List<Double> DAMAGE_LEVELS = List.of(12.0, 15.0, 18.0, 22.0, 26.0);
    private static final List<Double> RANGE_LEVELS = List.of(20.0, 25.0, 30.0, 35.0, 40.0);
    private static final List<Integer> COOLDOWN_LEVELS = List.of(120, 100, 80, 70, 60); // In ticks

    public static final AbilityInfo<Raygun> INFO = new AbilityInfo.Builder<Raygun>()
            .id(ID)
            .displayText(Component.text("Ray Gun", NamedTextColor.AQUA, TextDecoration.BOLD))
            .descriptionProvider((p, level) -> List.of(
                    Utils.text("Bắn một tia năng lượng điện từ gây ", NamedTextColor.GRAY)
                            .append(Utils.text(Utils.formatDecimal(DAMAGE_LEVELS.get(level - 1)) + " sát thương", NamedTextColor.RED)),
                    Utils.text("Tầm xa: ", NamedTextColor.GRAY)
                            .append(Utils.text(Utils.formatDecimal(RANGE_LEVELS.get(level - 1)) + "m", NamedTextColor.YELLOW)),
                    Utils.text("Hồi chiêu: ", NamedTextColor.GRAY)
                            .append(Utils.text(Utils.formatDecimal(COOLDOWN_LEVELS.get(level - 1) / 20.0) + "s", NamedTextColor.GREEN))
            ))
            .displayIcon(Material.END_ROD)
            .factory(Raygun::new)
            .trigger(AbilityTrigger.SHIFT_PROJECTILE)
            .build();

    public Raygun(SmpPlayer player, int level) { super(player, level); }

    @Override
    public void cast() {
        Player p = smpPlayer.getBukkitPlayer();
        if (isOnCooldown()) return;

        // Grab values based on current level
        double damage = DAMAGE_LEVELS.get(level - 1);
        double range = RANGE_LEVELS.get(level - 1);
        int cooldown = COOLDOWN_LEVELS.get(level - 1);

        setCooldownTick(cooldown);

        Location start = p.getEyeLocation().subtract(0, 0.2, 0);
        Vector direction = start.getDirection();

        // 1. Raytrace using scaled range
        RayTraceResult ray = p.getWorld().rayTrace(start, direction, range,
                FluidCollisionMode.NEVER, true, 0.5, e -> !e.equals(p));

        Location end = ray != null
                ? ray.getHitPosition().toLocation(p.getWorld())
                : start.clone().add(direction.clone().multiply(range));

        // 2. Visuals (Electric Sound & Tilted Arcs)
        playElectricLaunchSounds(start);
        spawnLaserBeam(start, end);

        // 3. Impact Logic
        if (ray != null && !(ray.getHitEntity() instanceof Player) && ray.getHitEntity() instanceof LivingEntity victim) {
            DamageUtils.damage(victim, p, damage, new DamageEvent.Metadata(ID, DamageType.PROJECTILE_ABILITY));
            playImpactEffects(end, victim);
        }
    }

    private void playElectricLaunchSounds(Location loc) {
        World w = loc.getWorld();
        w.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 1.5f);
        w.playSound(loc, Sound.ENTITY_GUARDIAN_ATTACK, 1.0f, 2.0f);
        w.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.4f, 2.0f);
        w.playSound(loc, Sound.ITEM_TRIDENT_RIPTIDE_1, 1.0f, 1.6f);
    }

    private void playImpactEffects(Location loc, Entity victim) {
        World w = loc.getWorld();
        w.spawnParticle(Particle.FLASH, loc, 1, Color.AQUA);
        w.spawnParticle(Particle.ELECTRIC_SPARK, loc, 5, 0.1, 0.1, 0.1, 0.05);
    }

    private void spawnLaserBeam(Location start, Location end) {
        double dist = start.distance(end);
        Vector dir = end.toVector().subtract(start.toVector()).normalize();
        Location cursor = start.clone().setDirection(dir);

        List<ParticleBuilder> builders = List.of(
                new ParticleBuilder(Particle.SOUL_FIRE_FLAME).count(0),
                new ParticleBuilder(Particle.DUST).data(new Particle.DustOptions(Color.AQUA, 0.5f)).count(0)
        );

        double spacing = 0.2;
        double maxBulge = 0.4 + (level * 0.05); // Arcs get slightly wider as level increases

        for (double d = 0; d < dist; d += spacing) {
            double progress = d / dist;
            double offset = Math.sin(progress * Math.PI) * maxBulge;

            double jitter = (Math.random() - 0.5) * 0.04;

            double finalD = d;
            ParticleShape arcs = (tick) -> List.of(
                    new Vector(offset + jitter, offset + jitter, finalD),
                    new Vector(-offset + jitter, offset + jitter, finalD)
            );

            ParticleUtils.spawnShape(cursor, arcs, builders, 0);
        }
    }

    @Override public @NotNull AbilityInfo<? extends Ability> getAbilityInfo() { return INFO; }
}