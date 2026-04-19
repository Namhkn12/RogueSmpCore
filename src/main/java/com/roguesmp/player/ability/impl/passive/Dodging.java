package com.roguesmp.player.ability.impl.passive;

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

    // Cached Attributes
    private final int cooldown;
    private int lastTriggerTick = -1;

    /**
     * Action-based INFO shell.
     * Note: Passive abilities often use "execute" as a manual trigger for
     * debugging or external force-triggers.
     */
    public static final AbilityInfo<Dodging> INFO = new AbilityInfo<>(
            ID,
            Dodging.class,
            Dodging::new
    );

    public Dodging(SmpPlayer player, int level) {
        super(player, level);
        // Cache cooldown from JSON
        this.cooldown = (int) getAbilityInfo().getAttributeForLevel("cooldown_ticks", level);
    }

    /**
     * Internal logic to check if a dodge should occur.
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
        setCooldownTick(cooldown);

        // Visuals and Sound
        Location loc = p.getLocation().add(0, 1, 0);
        World world = p.getWorld();
        world.spawnParticle(Particle.SMOKE, loc, 30, 0.3, 0.5, 0.3, 0.05);
        world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 1f, 2f);

        p.sendActionBar(Component.text("DODGED!", NamedTextColor.AQUA, TextDecoration.BOLD));
        return true;
    }

    @Override
    public void onHurt(DamageEvent event) {
        // Only dodge Projectiles
        if (event.getDamageType() == DamageType.PROJECTILE) {
            if (attemptDodge()) {
                event.setCancelled(true);
                // Grant brief i-frames (20 ticks = 1s)
                smpPlayer.getBukkitPlayer().setNoDamageTicks(20);
            }
        }
    }

    @Override
    public void onProjectileHit(ProjectileHitEvent event) {
        Player p = smpPlayer.getBukkitPlayer();
        if (!p.equals(event.getHitEntity())) return;

        Projectile proj = event.getEntity();

        // Conditionals: Don't waste dodge if blocking or if immune to fire splash
        if (p.isBlocking()) return;
        if (proj instanceof Fireball && p.hasPotionEffect(PotionEffectType.FIRE_RESISTANCE)) return;

        if (attemptDodge()) {
            proj.remove();
        }
    }

    @Override
    public void onCombust(EntityCombustEvent event) {
        // Only dodge fire caused by projectiles (like flaming arrows)
        if (event instanceof EntityCombustByEntityEvent combustByEntity) {
            if (combustByEntity.getCombuster() instanceof Projectile) {
                if (attemptDodge()) {
                    event.setCancelled(true);
                }
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

    @Override public @NotNull AbilityInfo<?> getAbilityInfo() { return INFO; }
}
