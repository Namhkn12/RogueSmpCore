package com.roguesmp.player.ability.impl.active;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.constant.DamageType;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.ability.Ability;
import com.roguesmp.player.ability.AbilityInfo;
import com.roguesmp.player.ability.trigger.AbilityResponse;
import com.roguesmp.utils.DamageUtils;
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

import java.util.List;

public class Flamestrike extends Ability {
    public static final String ID = "flamestrike";

    // Mechanics Constants
    private static final int HALF_ANGLE = 70;
    private static final float BASE_KNOCKBACK = 0.5f;

    /**
     * Updated INFO registration.
     * Metadata, scaling, and triggers are now defined in JSON.
     */
    public static final AbilityInfo<Flamestrike> INFO = new AbilityInfo<>(
            ID,
            Flamestrike.class,
            Flamestrike::new
    ).registerAction("execute", Flamestrike::handleCast);

    private final double damage;
    private final double radius;
    private final int cooldown;

    public Flamestrike(SmpPlayer player, int level) {
        super(player, level);
        damage = getAbilityInfo().getAttributeForLevel("damage", level);
        radius = getAbilityInfo().getAttributeForLevel("radius", level);
        cooldown = (int) getAbilityInfo().getAttributeForLevel("cooldown", level);
    }

    public AbilityResponse handleCast() {
        if (isOnCooldown()) return AbilityResponse.continueChain();

        Player p = smpPlayer.getBukkitPlayer();
        Location origin = p.getLocation();
        World world = p.getWorld();

        // Custom logic for fire ticks based on level
        int fireTicks = 80 + (level * 20);

        setCooldownTick(cooldown);

        // --- Damage & Knockback Logic ---
        Vector direction = p.getEyeLocation().getDirection().setY(0).normalize();

        for (LivingEntity target : world.getNearbyLivingEntities(origin, radius)) {
            if (target instanceof Player) continue;

            Vector toTarget = target.getLocation().toVector().subtract(origin.toVector()).setY(0).normalize();
            double dot = direction.dot(toTarget);
            double angle = Math.toDegrees(FastMath.acos(Math.max(-1.0, Math.min(1.0, dot)))); // Clamp dot for safety

            if (angle <= HALF_ANGLE) {
                DamageUtils.damage(target, p, damage, new DamageEvent.Metadata(ID, DamageType.MAGIC));
                target.setFireTicks(fireTicks);

                Vector kb = target.getLocation().toVector().subtract(origin.toVector()).normalize().multiply(BASE_KNOCKBACK);
                target.setVelocity(target.getVelocity().add(kb.setY(0.3)));
            }
        }

        // --- Visual Effects: Expanding Flame Wave ---
        new BukkitRunnable() {
            double currentRadius = 0;
            final Location waveCenter = p.getLocation().add(0, 0.1, 0);
            final float centerYaw = p.getLocation().getYaw();

            @Override
            public void run() {
                currentRadius += 1.25;

                for (double degree = -HALF_ANGLE; degree <= HALF_ANGLE; degree += 10) {
                    double totalYaw = Math.toRadians(-(centerYaw + degree));

                    double x = FastMath.sin(totalYaw) * currentRadius;
                    double z = FastMath.cos(totalYaw) * currentRadius;

                    Location particleLoc = waveCenter.clone().add(x, 0, z);

                    world.spawnParticle(Particle.FLAME, particleLoc, 2, 0.15, 0.15, 0.15, 0.05);
                    world.spawnParticle(Particle.SMOKE, particleLoc, 1, 0.1, 0.1, 0.1, 0.02);
                }

                if (currentRadius >= radius || !p.isOnline()) {
                    this.cancel();
                }
            }
        }.runTaskTimer(RogueSmpCore.getInstance(), 0, 1);

        // --- Audio ---
        world.playSound(origin, Sound.ENTITY_BLAZE_SHOOT, 1f, 0.75f);
        world.playSound(origin, Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 1f, 1.25f);
        world.playSound(origin, Sound.ITEM_FIRECHARGE_USE, 1f, 0.5f);

        return AbilityResponse.consume();
    }

    @Override public @NotNull AbilityInfo<?> getAbilityInfo() { return INFO; }
}
