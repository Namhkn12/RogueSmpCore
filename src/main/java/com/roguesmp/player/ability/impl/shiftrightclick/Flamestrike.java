package com.roguesmp.player.ability.impl.shiftrightclick;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.constant.AbilityTrigger;
import com.roguesmp.constant.DamageType;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.ability.Ability;
import com.roguesmp.player.ability.AbilityInfo;
import com.roguesmp.utils.DamageUtils;
import com.roguesmp.utils.Utils;
import com.roguesmp.utils.VectorUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class Flamestrike extends Ability {
    public static final String ID = "flamestrike";

    // Level Scaling
    private static final List<Double> DAMAGE_LEVELS = List.of(14.0, 17.0, 20.0, 23.0, 32.0);
    private static final List<Double> RADIUS_LEVELS = List.of(5.0, 6.0, 7.0, 8.0, 9.0);
    private static final List<Integer> COOLDOWN_LEVELS = List.of(200, 180, 160, 140, 120); // 10s to 6s

    // Mechanics Constants
    private static final int HALF_ANGLE = 70;
    private static final float BASE_KNOCKBACK = 0.5f;

    public static final AbilityInfo<Flamestrike> INFO = new AbilityInfo.Builder<Flamestrike>()
            .id(ID)
            .displayText(Component.text("Flamestrike", NamedTextColor.GOLD, TextDecoration.BOLD))
            .descriptionProvider((p, l) -> List.of(
                    Utils.text("Phóng ra một luồng lửa hình nón về phía trước.", NamedTextColor.GRAY),
                    Utils.text("Sát thương: ", NamedTextColor.GRAY)
                            .append(Utils.text(Utils.formatDecimal(DAMAGE_LEVELS.get(l - 1)), NamedTextColor.RED)),
                    Utils.text("Hiệu ứng: ", NamedTextColor.GRAY)
                            .append(Utils.text("Đẩy lùi và thiêu đốt kẻ địch.", NamedTextColor.GOLD))
            ))
            .displayIcon(Material.FLINT_AND_STEEL)
            .factory(Flamestrike::new)
            .trigger(AbilityTrigger.SHIFT_RIGHT_CLICK)
            .build();

    public Flamestrike(SmpPlayer player, int level) { super(player, level); }

    @Override
    public void cast() {
        if (isOnCooldown()) return;

        Player p = smpPlayer.getBukkitPlayer();
        Location origin = p.getLocation();
        World world = p.getWorld();

        double damage = DAMAGE_LEVELS.get(level - 1);
        double radius = RADIUS_LEVELS.get(level - 1);
        int fireTicks = 80 + (level * 20); // 4s + 1s per level
        setCooldownTick(COOLDOWN_LEVELS.get(level - 1));

        // 1. Damage & Knockback Logic
        // We use a cone check: within radius AND within the angle from the player's facing direction
        Vector direction = p.getEyeLocation().getDirection().setY(0).normalize();

        for (LivingEntity target : p.getWorld().getNearbyLivingEntities(origin, radius)) {
            if (target.equals(p)) continue;

            Vector toTarget = target.getLocation().toVector().subtract(origin.toVector()).setY(0).normalize();
            double dot = direction.dot(toTarget); // Cosine of the angle
            double angle = Math.toDegrees(Math.acos(dot));

            if (angle <= HALF_ANGLE) {
                DamageUtils.damage(target, p, damage, new DamageEvent.Metadata(ID, DamageType.MAGIC));
                target.setFireTicks(fireTicks);

                // Knockback away from player
                Vector kb = target.getLocation().toVector().subtract(origin.toVector()).normalize().multiply(BASE_KNOCKBACK);
                target.setVelocity(target.getVelocity().add(kb.setY(0.3)));
            }
        }

        // 2. Visual Effects: Expanding Flame Wave
        new BukkitRunnable() {
            double currentRadius = 0;
            // Capture the initial direction so the wave doesn't "bend" if the player turns
            final Location waveCenter = p.getLocation().add(0, 0.1, 0);
            final float centerYaw = p.getLocation().getYaw();

            @Override
            public void run() {
                currentRadius += 1.25;

                // Iterate through the angle spread
                for (double degree = -HALF_ANGLE; degree <= HALF_ANGLE; degree += 10) {
                    // We add the degree offset directly to the player's yaw
                    double totalYaw = Math.toRadians(-(centerYaw + degree));

                    // Standard trigonometry to find X and Z based on angle and radius
                    double x = Math.sin(totalYaw) * currentRadius;
                    double z = Math.cos(totalYaw) * currentRadius;

                    Location particleLoc = waveCenter.clone().add(x, 0, z);

                    world.spawnParticle(Particle.FLAME, particleLoc, 2, 0.15, 0.15, 0.15, 0.05);
                    world.spawnParticle(Particle.SMOKE, particleLoc, 1, 0.1, 0.1, 0.1, 0.02);
                }

                if (currentRadius >= radius || !p.isOnline()) {
                    this.cancel();
                }
            }
        }.runTaskTimer(RogueSmpCore.getInstance(), 0, 1);

        // 3. Audio
        world.playSound(origin, Sound.ENTITY_BLAZE_SHOOT, 1f, 0.75f);
        world.playSound(origin, Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 1f, 1.25f);
        world.playSound(origin, Sound.ITEM_FIRECHARGE_USE, 1f, 0.5f);
    }

    @Override public @NotNull AbilityInfo<? extends Ability> getAbilityInfo() { return INFO; }
}
