package com.roguesmp.player.ability.impl.passive;

import com.roguesmp.constant.AbilityTrigger;
import com.roguesmp.constant.DamageType;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.ability.Ability;
import com.roguesmp.player.ability.AbilityInfo;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class Dodging extends Ability {
    public static final String ID = "dodging";

    // Level Scaling (Cooldown in Ticks)
    private static final List<Integer> COOLDOWN_LEVELS = List.of(340, 300, 260, 220, 120); // 17s down to 6s

    private int lastTriggerTick = -1;

    public static final AbilityInfo<Dodging> INFO = new AbilityInfo.Builder<Dodging>()
            .id(ID)
            .displayText(Component.text("Dodging", NamedTextColor.WHITE, TextDecoration.BOLD))
            .descriptionProvider((p, l) -> List.of(
                    Utils.text("Tự động né tránh các đòn tấn công từ xa.", NamedTextColor.GRAY),
                    Utils.text("Hiệu ứng: ", NamedTextColor.GRAY)
                            .append(Utils.text("Vô hiệu hóa sát thương và hiệu ứng của Projectile.", NamedTextColor.AQUA)),
                    Utils.text("Hồi chiêu: ", NamedTextColor.GRAY)
                            .append(Utils.text(COOLDOWN_LEVELS.get(l - 1) / 20 + " giây", NamedTextColor.YELLOW))
            ))
            .displayIcon(Material.COBWEB)
            .factory(Dodging::new)
            .trigger(AbilityTrigger.PASSIVE)
            .build();

    public Dodging(SmpPlayer player, int level) {
        super(player, level);
    }

    /**
     * Internal logic to check if a dodge should occur.
     * Logic: If already triggered this tick, return true. Otherwise, check cooldown.
     */
    private boolean attemptDodge() {
        Player p = smpPlayer.getBukkitPlayer();
        int currentTick = p.getTicksLived();

        // Already dodged something this tick? allow multiple dodges for 1 cooldown
        if (lastTriggerTick == currentTick) {
            return true;
        }

        if (isOnCooldown()) {
            return false;
        }

        // Trigger Dodge
        lastTriggerTick = currentTick;
        setCooldownTick(COOLDOWN_LEVELS.get(level - 1));

        // Visuals and Sound
        Location loc = p.getLocation().add(0, 1, 0);
        World world = p.getWorld();
        world.spawnParticle(Particle.SMOKE, loc, 30, 0.3, 0.5, 0.3, 0.05);
        world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 1f, 2f);

        return true;
    }

    @Override
    public void onHurt(DamageEvent event) {
        // Only dodge Projectiles that aren't blocked by a shield
        if (event.getDamageType() == DamageType.PROJECTILE) {
            if (attemptDodge()) {
                event.setCancelled(true);
                // Grant brief i-frames to prevent follow-up melee
                smpPlayer.getBukkitPlayer().setNoDamageTicks(20);
            }
        }
    }

    public void onProjectileHit(ProjectileHitEvent event) {
        Player p = smpPlayer.getBukkitPlayer();
        if (!event.getHitEntity().equals(p)) return;

        Projectile proj = event.getEntity();

        // Don't dodge if blocking with a shield (save the cooldown)
        if (p.isBlocking()) return;

        // Don't dodge fireball splash if player has Fire Res
        if (proj instanceof Fireball && p.hasPotionEffect(PotionEffectType.FIRE_RESISTANCE)) return;

        if (attemptDodge()) {
            // Remove the projectile so it doesn't drop as an item or stick in the player
            proj.remove();
        }
    }

    public void onCombust(EntityCombustEvent event) {
        if (!(event instanceof EntityCombustByEntityEvent entityCombustByEntityEvent)) return;
        // Only dodge fire caused by projectiles (like flaming arrows)
        if (entityCombustByEntityEvent.getCombuster() instanceof Projectile) {
            if (attemptDodge()) {
                event.setCancelled(true);
            }
        }
    }

//    public void onPotionSplash(PotionSplashEvent event) {
//        Player p = smpPlayer.getBukkitPlayer();
//        if (!event.getAffectedEntities().contains(p)) return;
//
//        // Only dodge hostile potions from mobs/bosses (ignore self or other players)
//        if (event.getPotion().getShooter() instanceof Player) return;
//
//        if (attemptDodge()) {
//            // Effectively remove the player from the affected list for this splash
//            event.setIntensity(p, 0);
//        }
//    }

    @Override public void cast() { /* Passive */ }

    @Override public @NotNull AbilityInfo<? extends Ability> getAbilityInfo() { return INFO; }
}
