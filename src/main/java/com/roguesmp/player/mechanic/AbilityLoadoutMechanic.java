package com.roguesmp.player.mechanic;

import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.ability.trigger.AbilityTrigger;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;

public class AbilityLoadoutMechanic implements PlayerMechanic {

    @Override
    public int getPriority() {
        return 110;
    }

    @Override
    public void tick(int periodIncrement, SmpPlayer player) {
        player.getAbilityLoadout().tick(periodIncrement);
    }

    @Override
    public void onInteract(PlayerInteractEvent event, SmpPlayer player) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        if (event.getAction().isLeftClick()) {
            player.getAbilityLoadout().cast(AbilityTrigger.Key.LEFT_CLICK);
        } else if (event.getAction().isRightClick()) {
            if (event.isBlockInHand()) return;
            player.getAbilityLoadout().cast(AbilityTrigger.Key.RIGHT_CLICK);
        }
    }

    @Override
    public void onSwapHand(PlayerSwapHandItemsEvent event, SmpPlayer player) {
        player.getAbilityLoadout().cast(AbilityTrigger.Key.SWAP);
        event.setCancelled(true);
    }

    @Override
    public void onInput(PlayerInputEvent event, SmpPlayer player) {
        if (event.getInput().isJump()) player.getAbilityLoadout().cast(AbilityTrigger.Key.JUMP);
        else if (event.getInput().isSneak()) player.getAbilityLoadout().cast(AbilityTrigger.Key.SNEAK);
    }

    @Override
    public void onDamageEntity(DamageEvent event, SmpPlayer player) {
        player.getAbilityLoadout().onDamageEntity(event);
    }

    @Override
    public void onKillEntity(EntityDeathEvent event, SmpPlayer player) {
        player.getAbilityLoadout().onKillEntity(event);
    }

    @Override
    public void onHurt(DamageEvent event, SmpPlayer player) {
        player.getAbilityLoadout().onHurt(event);
    }

    @Override
    public void onHurtFatal(DamageEvent event, SmpPlayer player) {
        player.getAbilityLoadout().onHurtFatal(event);
    }

    @Override
    public void onConsume(PlayerItemConsumeEvent event, SmpPlayer player) {
        player.getAbilityLoadout().onConsume(event);
    }

    @Override
    public void onExpChange(PlayerExpChangeEvent event, SmpPlayer player) {
        player.getAbilityLoadout().onExpChange(event);
    }

    @Override
    public void onBlockBreak(BlockBreakEvent event, SmpPlayer player) {
        player.getAbilityLoadout().onBlockBreak(event);
    }

    @Override
    public void onCombust(EntityCombustEvent event, SmpPlayer player) {
        player.getAbilityLoadout().onCombust(event);
    }

    @Override
    public void onCombustEntity(EntityCombustByEntityEvent event, SmpPlayer player) {
        player.getAbilityLoadout().onCombustEntity(event);
    }

    @Override
    public void onProjectileHit(ProjectileHitEvent event, SmpPlayer player) {
        player.getAbilityLoadout().onProjectileHit(event);
    }

    @Override
    public void onProjectileLaunch(PlayerLaunchProjectileEvent event, SmpPlayer player) {
        player.getAbilityLoadout().onProjectileLaunch(event);
    }

    @Override
    public void onShootArrow(EntityShootBowEvent event, SmpPlayer player) {
        player.getAbilityLoadout().onShootArrow(event);
    }
}
