package com.roguesmp.listener;

import com.roguesmp.constant.EquipSlot;
import com.roguesmp.event.ArrowConsumeEvent;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.island.IslandData;
import com.roguesmp.island.IslandManager;
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
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;

public class PlayerListener implements Listener {

    private final PlayerManager playerManager;
    private final PlayerDataManager playerDataManager;
    private final IslandManager islandManager;

    public PlayerListener(PlayerManager playerManager, IslandManager islandManager) {
        this.playerManager = playerManager;
        this.playerDataManager = playerManager.getDataManager();
        this.islandManager = islandManager;
    }

    @EventHandler
    public void onPlayerPreJoin(AsyncPlayerPreLoginEvent event) {
        UUID uuid = event.getUniqueId();
        PlayerData playerData = playerDataManager.loadPlayerData(uuid);
        IslandData islandData = islandManager.getIslandDataManager().loadIslandData(playerData.getIslandId());
        Utils.runLater(() -> {
            playerDataManager.cacheData(playerData);
            if (islandData != null) {
                islandManager.getIslandDataManager().cache(islandData);
            }
        });
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        playerManager.loadAndTrackPlayer(player.getUniqueId());
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        SmpPlayer smpPlayer = playerManager.getSmpPlayer(player);
        if (smpPlayer == null) return;

        PlayerData playerData = playerManager.getDataManager().removeCachedData(smpPlayer.getUuid());
        playerManager.untrackPlayer(smpPlayer.getUuid());

        IslandData islandData = islandManager.getIslandDataManager().getCachedData(playerData.getIslandId());
        if (islandData != null) {
            boolean islandEmpty = true;
            for (UUID memberId : islandData.getMembers()) {
                if (memberId.equals(player.getUniqueId())) continue;

                Player onlineMember = Bukkit.getPlayer(memberId);
                if (onlineMember != null && onlineMember.isOnline()) {
                    islandEmpty = false;
                    break; // Keep it loaded, some member is still online
                }
            }
            if (islandEmpty) {
                islandManager.getIslandDataManager().removeCache(playerData.getIslandId());
                Utils.runAsync(() -> islandManager.getIslandDataManager().saveIslandData(islandData));
            }
        }

        Utils.runAsync(() -> {
            playerManager.getDataManager().savePlayerData(playerData);
        });
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

//    @EventHandler
//    public void onWorldChange(PlayerChangedWorldEvent event) {
//        Player player = event.getPlayer();
//        SmpPlayer smpPlayer = playerManager.getSmpPlayer(player.getUniqueId());
//        if (smpPlayer == null) return;
//
//        for (EquipSlot slot : EquipSlot.values()) {
//            smpPlayer.markForInstantUpdate(slot);
//        }
//    }
//
//    @EventHandler
//    public void onItemDamage(PlayerItemDamageEvent event) {
//        Player player = event.getPlayer();
//        SmpPlayer smpPlayer = playerManager.getSmpPlayer(player.getUniqueId());
//        if (smpPlayer == null) return;
//        smpPlayer.
//        event.setDamage(1); //Always 1.
//    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        SmpPlayer smpPlayer = playerManager.getSmpPlayer(event.getPlayer().getUniqueId());
        if (smpPlayer == null) return;
        smpPlayer.onInteract(event);
    }

    @EventHandler
    public void onInput(PlayerInputEvent event) {
        SmpPlayer smpPlayer = playerManager.getSmpPlayer(event.getPlayer().getUniqueId());
        if (smpPlayer == null) return;
        smpPlayer.onInput(event);
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
    public void onBlockPlace(BlockPlaceEvent event) {
        SmpPlayer smpPlayer = playerManager.getSmpPlayer(event.getPlayer().getUniqueId());
        if (smpPlayer == null) return;
        smpPlayer.onBlockPlace(event);
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
