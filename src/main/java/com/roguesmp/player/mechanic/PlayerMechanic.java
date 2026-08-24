package com.roguesmp.player.mechanic;

import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent;
import com.roguesmp.constant.EquipSlot;
import com.roguesmp.event.AbilityCastEvent;
import com.roguesmp.event.ArrowConsumeEvent;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.event.DurabilityChangedEvent;
import com.roguesmp.item.SmpItem;
import com.roguesmp.player.SmpPlayer;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.jetbrains.annotations.Nullable;

public interface PlayerMechanic {

    /**
     * Determines the execution order of this mechanic. Lower values run first. <br>
     * Example: Restrictions: ~10 | Core/Attributes/Enchants: ~100 | Consumers/Durability: ~500
     */
    default int getPriority() {
        return 100;
    }

    /**
     * Called on a regular interval loop for active player tracking.
     */
    default void tick(int periodIncrement, SmpPlayer player) {}

    default void onInteract(PlayerInteractEvent event, SmpPlayer player) {}

    default void onEntityInteract(PlayerInteractEntityEvent event, SmpPlayer player) {}

    default void onSwapHand(PlayerSwapHandItemsEvent event, SmpPlayer player) {}

    default void onInput(PlayerInputEvent event, SmpPlayer player) {}

    default void onAbilityCast(AbilityCastEvent event, SmpPlayer player) {}

    default void onDamageEntity(DamageEvent event, SmpPlayer player) {}

    default void onDamageEntityFinal(DamageEvent event, SmpPlayer player) {}

    default void onKillEntity(EntityDeathEvent event, SmpPlayer player) {}

    default void onHurt(DamageEvent event, SmpPlayer player) {}

    default void onHurtFinal(DamageEvent event, SmpPlayer player) {}

    default void onHurtFatal(DamageEvent event, SmpPlayer player) {}

    default void onConsume(PlayerItemConsumeEvent event, SmpPlayer player) {}

    default void onExpChange(PlayerExpChangeEvent event, SmpPlayer player) {}

    default void onBlockBreak(BlockBreakEvent event, SmpPlayer player) {}

    default void onBlockPlace(BlockPlaceEvent event, SmpPlayer player) {}

    /**
     * Called when player is set on fire
     */
    default void onCombust(EntityCombustEvent event, SmpPlayer player) {}

    /**
     * Called when player set other entities (not player) on fire (including from projectiles)
     */
    default void onCombustEntity(EntityCombustByEntityEvent event, SmpPlayer player) {}

    default void onProjectileHit(ProjectileHitEvent event, SmpPlayer player) {}

    default void onProjectileLaunch(PlayerLaunchProjectileEvent event, SmpPlayer player) {}

    default void onShootArrow(EntityShootBowEvent event, SmpPlayer player) {}

    default void onConsumeArrow(ArrowConsumeEvent event, SmpPlayer player) {}

    default void onDurabilityChange(DurabilityChangedEvent event, SmpPlayer player) {}

    /**
     * Called once per {@code SmpPlayer.updateSlotStat} call, after the slot's item has already
     * been swapped - a generic "this equipment slot changed" notification.
     */
    default void onEquipmentChange(EquipSlot slot, @Nullable SmpItem newItem, SmpPlayer player) {}
}
