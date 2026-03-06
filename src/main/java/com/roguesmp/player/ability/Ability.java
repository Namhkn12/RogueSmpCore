package com.roguesmp.player.ability;

import com.roguesmp.event.DamageEvent;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.Utils;
import io.papermc.paper.registry.keys.SoundEventKeys;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
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

    public abstract void cast();

    public abstract @NotNull AbilityInfo<? extends Ability> getAbilityInfo();

    public int getCooldownTick() {
        return cooldownTick;
    }

    public boolean isOnCooldown() {
        return cooldownTick > 0;
    }

    public void setCooldownTick(int cooldownTick) {
        this.cooldownTick = cooldownTick;
    }

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

    public void onCooldownRefreshed() {
        Player bukkitPlayer = smpPlayer.getBukkitPlayer();
        bukkitPlayer.playSound(Sound.sound(SoundEventKeys.ITEM_TRIDENT_RETURN, Sound.Source.PLAYER, 1f, Utils.RANDOM.nextFloat(0.6f, 1.4f)));
        bukkitPlayer.sendActionBar(getAbilityInfo().getDisplayText().append(Component.text(" đã hồi chiêu", NamedTextColor.YELLOW)));

    }

    public void tick(boolean twoHz, boolean oneHz) {

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

    public void onProjectileHit(ProjectileHitEvent event) {

    }

    public void onProjectileLaunch(ProjectileLaunchEvent event) {

    }
}
