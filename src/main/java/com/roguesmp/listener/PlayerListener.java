package com.roguesmp.listener;

import com.roguesmp.constant.EquipSlot;
import com.roguesmp.event.ArrowConsumeEvent;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.item.SmpItem;
import com.roguesmp.player.*;
import com.roguesmp.utils.ItemStackUtils;
import com.roguesmp.utils.Utils;
import io.papermc.paper.event.entity.EntityEquipmentChangedEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;

public class PlayerListener implements Listener {

    private final PlayerManager playerManager;
    private final PlayerDataManager playerDataManager;

    public PlayerListener(PlayerManager playerManager, PlayerDataManager playerDataManager) {
        this.playerManager = playerManager;
        this.playerDataManager = playerDataManager;
    }

    @EventHandler
    public void onPlayerPreJoin(AsyncPlayerPreLoginEvent event) {
        UUID uuid = event.getUniqueId();
        PlayerData playerData = playerDataManager.loadPlayerData(uuid);
        Utils.runLater(() -> playerDataManager.cacheData(playerData));
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        playerManager.loadPlayer(player.getUniqueId());
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        playerManager.unloadPlayer(player.getUniqueId());
    }

    @EventHandler
    public void onEquipmentChange(EntityEquipmentChangedEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        SmpPlayer smpPlayer = playerManager.getSmpPlayer(player.getUniqueId());
        if (smpPlayer == null) return;
        for (Map.Entry<EquipmentSlot, EntityEquipmentChangedEvent.EquipmentChange> entry : event.getEquipmentChanges().entrySet()) {
            EquipmentSlot equipmentSlot = entry.getKey();
            EntityEquipmentChangedEvent.EquipmentChange equipmentChange = entry.getValue();
            EquipSlot equipSlot = EquipSlot.fromVanilla(equipmentSlot.getGroup());
            SmpItem newItem = null;
            ItemStack newStack = equipmentChange.newItem();
            if (ItemStackUtils.isValidItem(newStack)) {
                newItem = new SmpItem(newStack);
                player.getEquipment().setItem(equipmentSlot, newItem.generateItemStack(smpPlayer, newStack.getAmount()));
            }
            smpPlayer.updateSlotStat(player, equipSlot, newItem);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        SmpPlayer smpPlayer = playerManager.getSmpPlayer(event.getPlayer().getUniqueId());
        if (smpPlayer == null) return;
        smpPlayer.onInteract(event);
    }

    @EventHandler
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        SmpPlayer smpPlayer = playerManager.getSmpPlayer(event.getPlayer().getUniqueId());
        if (smpPlayer == null) return;
        smpPlayer.onSwapHand(event);
    }

    @EventHandler
    public void onCustomDamage(DamageEvent event) {
        Entity victim = event.getVictim();
        Entity damager = event.getDamager();
        if (victim instanceof Player player) {
            // Player got damaged
            SmpPlayer smpPlayer = playerManager.getSmpPlayer(player.getUniqueId());
            if (smpPlayer == null) return;
            smpPlayer.onHurt(event);
            return;
        }

        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player player) {
            // Player damage an entity with projectile
            SmpPlayer smpPlayer = playerManager.getSmpPlayer(player.getUniqueId());
            if (smpPlayer == null) return;
            smpPlayer.onDamageEntity(event);
            return;
        }

        if (damager instanceof Player player) {
            // Player damage (melee) an entity
            SmpPlayer smpPlayer = playerManager.getSmpPlayer(player.getUniqueId());
            if (smpPlayer == null) return;
            smpPlayer.onDamageEntity(event);
        }

    }

    @EventHandler(priority = EventPriority.HIGH) // Should run later to catch final damage dealt
    public void onHurtFatal(DamageEvent event) {
        if (event.getVictim() instanceof Player player && event.getFinalDamage() >= player.getHealth()) {
            SmpPlayer smpPlayer = playerManager.getSmpPlayer(player.getUniqueId());
            if (smpPlayer == null) return;
            smpPlayer.onHurtFatal(event);
        }
    }

    @EventHandler
    public void onKillEntity(EntityDeathEvent event) {
        Entity entity = event.getDamageSource().getCausingEntity();
        if (!(entity instanceof Player player)) return;
        SmpPlayer smpPlayer = playerManager.getSmpPlayer(player.getUniqueId());
        if (smpPlayer == null) return;
        smpPlayer.onKillEntity(event);
    }

    @EventHandler
    public void onConsumeItem(PlayerItemConsumeEvent event) {
        SmpPlayer smpPlayer = playerManager.getSmpPlayer(event.getPlayer().getUniqueId());
        if (smpPlayer == null) return;
        smpPlayer.onConsume(event);
    }

    @EventHandler
    public void onExpChange(PlayerExpChangeEvent event) {
        SmpPlayer smpPlayer = playerManager.getSmpPlayer(event.getPlayer().getUniqueId());
        if (smpPlayer == null) return;
        smpPlayer.onExpChange(event);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        SmpPlayer smpPlayer = playerManager.getSmpPlayer(event.getPlayer().getUniqueId());
        if (smpPlayer == null) return;
        smpPlayer.onBlockBreak(event);
    }

    @EventHandler
    public void onCombust(EntityCombustEvent event) {
        SmpPlayer smpPlayer = playerManager.getSmpPlayer(event.getEntity().getUniqueId());
        if (smpPlayer == null) return;
        smpPlayer.onCombust(event);
    }

    @EventHandler
    public void onCombustEntity(EntityCombustByEntityEvent event) {
        Entity combuster = event.getCombuster();
        Entity combustee = event.getEntity();
        if (combuster instanceof Player player) {
            if (combustee instanceof Player) {
                event.setCancelled(true);
                return;
            }
            SmpPlayer smpPlayer = playerManager.getSmpPlayer(player.getUniqueId());
            if (smpPlayer == null) return;
            smpPlayer.onCombustEntity(event);
            return;
        }
        if (combuster instanceof Projectile projectile && projectile.getShooter() instanceof Player player) {
            if (combustee instanceof Player) {
                event.setCancelled(true);
                return;
            }
            SmpPlayer smpPlayer = playerManager.getSmpPlayer(player.getUniqueId());
            if (smpPlayer == null) return;
            smpPlayer.onCombustEntity(event);

        }

    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player player)) return;
        SmpPlayer smpPlayer = playerManager.getSmpPlayer(player.getUniqueId());
        if (smpPlayer == null) return;
        smpPlayer.onProjectileHit(event);

    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getEntity().getShooter() instanceof Player player)) return;
        SmpPlayer smpPlayer = playerManager.getSmpPlayer(player.getUniqueId());
        if (smpPlayer == null) return;
        smpPlayer.onProjectileLaunch(event);
    }

    @EventHandler
    public void onConsumeArrow(ArrowConsumeEvent event) {
        SmpPlayer smpPlayer = playerManager.getSmpPlayer(event.getPlayer().getUniqueId());
        if (smpPlayer == null) return;
        smpPlayer.onConsumeArrow(event);
    }

    @EventHandler
    public void onShootBow(EntityShootBowEvent event) {
        if (event.getEntity() instanceof Player player
                && event.getBow() != null
                && event.getProjectile() instanceof AbstractArrow arrow
                && arrow.getPickupStatus() == AbstractArrow.PickupStatus.ALLOWED) {
            ArrowConsumeEvent arrowConsumeEvent = new ArrowConsumeEvent(player, event.getConsumable());
            Bukkit.getPluginManager().callEvent(arrowConsumeEvent);
            if (arrowConsumeEvent.isCancelled()) {
                ItemStack consumable = event.getConsumable();
                if (consumable == null) return;
                player.getInventory().addItem(consumable.clone());
                arrow.setPickupStatus(AbstractArrow.PickupStatus.CREATIVE_ONLY);
                player.updateInventory();
            }
        }
    }
}
