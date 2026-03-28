package com.roguesmp.player.ability;

import com.roguesmp.event.DamageEvent;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.Utils;
import io.papermc.paper.registry.keys.SoundEventKeys;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.jetbrains.annotations.NotNull;

public abstract class Ability {

    protected final SmpPlayer smpPlayer;
    protected final int level;

    protected int cooldownTick = 0;

    public Ability(SmpPlayer smpPlayer, int level) {
        this.smpPlayer = smpPlayer;
        this.level = level;
    }

    /**
     * Cast the ability, cooldown should also be handled in this method via {@link #setCooldownTick(int)}
     */
    public abstract void cast();

    /**
     * Get the {@link AbilityInfo} of this ability, every ability should have a static field of this
     * @return {@link AbilityInfo}
     */
    public abstract @NotNull AbilityInfo<? extends Ability> getAbilityInfo();

    /**
     * Get the current cooldown of this ability
     * @return Cooldown in ticks
     */
    public int getCooldownTick() {
        return cooldownTick;
    }

    public boolean isOnCooldown() {
        return cooldownTick > 0;
    }

    public void setCooldownTick(int cooldownTick) {
        this.cooldownTick = cooldownTick;
    }

    /**
     * Reduce cooldown by an amount
     * @param reduction The amount of ticks to reduce
     * @return Whether the ability should be off cooldown
     */
    public boolean tickCooldown(int reduction) {
        if (cooldownTick <= 0) return false;
        cooldownTick -= reduction;
        if (cooldownTick <= 0) {
            cooldownTick = 0;
            return true; // JUST finished
        }
        return false;
    }

    public int getLevel() {
        return level;
    }

    public SmpPlayer getSmpPlayer() {
        return smpPlayer;
    }

    /**
     * Used to notify the player that the ability is off cooldown
     */
    public void onCooldownRefreshed() {
        Player bukkitPlayer = smpPlayer.getBukkitPlayer();
        bukkitPlayer.playSound(Sound.sound(SoundEventKeys.ITEM_TRIDENT_RETURN, Sound.Source.PLAYER, 1f, Utils.RANDOM.nextFloat(0.6f, 1.4f)));
        bukkitPlayer.sendActionBar(getAbilityInfo().displayText().append(Component.text(" đã hồi chiêu", NamedTextColor.YELLOW)));

    }

    /**
     * Ticking, run every 'periodIncrement' ticks
     *
     * @param periodIncrement The period in tick from the last call to current
     */
    public void tick(int periodIncrement) {

    }

    public void onDamageEntity(DamageEvent event) {

    }

    public void onKillEntity(EntityDeathEvent event) {

    }

    public void onHurt(DamageEvent event) {

    }

    public void onHurtFatal(DamageEvent event) {

    }

    public void onConsume(PlayerItemConsumeEvent event) {

    }

    public void onExpChange(PlayerExpChangeEvent event) {

    }

    public void onBlockBreak(BlockBreakEvent event) {

    }

    public void onCombust(EntityCombustEvent event) {

    }

    public void onProjectileHit(ProjectileHitEvent event) {

    }

    public void onProjectileLaunch(ProjectileLaunchEvent event) {

    }

    /**
     * Finds the closest LivingEntity within a cubical range, excluding the caster.
     * @param loc The center point to search from.
     * @param range The radius of the search box.
     * @return The nearest LivingEntity, or null if none found.
     */
    protected LivingEntity findNearestEnemy(Location loc, double range) {
        LivingEntity nearest = null;
        double bestDistanceSq = range * range; // Compare squares to save performance
        Player caster = smpPlayer.getBukkitPlayer();

        // Search for entities in the bounding box
        for (Entity entity : loc.getWorld().getNearbyEntities(loc, range, range, range)) {
            if (entity instanceof LivingEntity living && !entity.equals(caster) && entity.isValid()) {
                // Check if the entity is actually alive and not an armor stand (unless you want to hit those)
                if (living instanceof org.bukkit.entity.Monster) {
                    double distSq = loc.distanceSquared(living.getLocation());
                    if (distSq < bestDistanceSq) {
                        bestDistanceSq = distSq;
                        nearest = living;
                    }
                }
            }
        }
        return nearest;
    }
}
