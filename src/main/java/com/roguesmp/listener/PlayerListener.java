package com.roguesmp.listener;

import com.roguesmp.constant.EquipSlot;
import com.roguesmp.context.DamageContext;
import com.roguesmp.item.BaseItem;
import com.roguesmp.registry.ItemRegistry;
import com.roguesmp.item.SmpItem;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.player.SmpPlayer;
import io.papermc.paper.event.entity.EntityEquipmentChangedEvent;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final PlayerManager playerManager;

    public PlayerListener(PlayerManager playerManager) {
        this.playerManager = playerManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        playerManager.addPlayer(player.getUniqueId());

        BaseItem baseItem = ItemRegistry.getInstance().getBaseItem("test");
        if (baseItem == null) {
            player.sendMessage("Not found");
        } else {
            player.getInventory().addItem(baseItem.generateItemStack(playerManager.getSmpPlayer(player.getUniqueId()), 1));
        }

    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        playerManager.removePlayer(player.getUniqueId());
    }

    @EventHandler
    public void onEquipmentChange(EntityEquipmentChangedEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        event.getEquipmentChanges().forEach((equipmentSlot, equipmentChange) -> {
            EquipSlot equipSlot = EquipSlot.fromVanilla(equipmentSlot.getGroup());
            SmpPlayer smpPlayer = playerManager.getSmpPlayer(player.getUniqueId());
            if (smpPlayer != null) {
                smpPlayer.updateSlotStat(player, equipSlot, new SmpItem(equipmentChange.oldItem()), new SmpItem(equipmentChange.newItem()));
                player.sendMessage("EquipmentChange " + equipSlot.name());
            }
        });
        player.sendMessage("----------------");
    }

    @EventHandler
    public void onDamageEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (event.getDamager() instanceof Projectile projectile) {
            if (!(projectile.getShooter() instanceof Player)) return;
            // Logic for projectiles: bow, trident, etc
        }
        SmpPlayer smpPlayer = playerManager.getSmpPlayer(player.getUniqueId());
        if (smpPlayer == null) return;
        DamageContext damageContext = new DamageContext(event.getEntity(), player, 1);

        damageContext.setCritical(event.isCritical());
        smpPlayer.getActiveEnchants().forEach((enchants, integer) -> {
            enchants.getEnchant().onAttackEntity(damageContext, integer);
        });

        smpPlayer.getActiveAttributes().forEach((attributes, aDouble) -> {
            attributes.getAttribute().onAttackEntity(damageContext, aDouble);
        });

        event.setDamage(damageContext.calculateFinalDamage());
        event.getDamager().sendMessage(String.valueOf(event.getDamage()));

    }

    @EventHandler
    public void onKillEntity(EntityDeathEvent event) {
        Entity entity = event.getDamageSource().getCausingEntity();
        if (!(entity instanceof Player player)) return;
        SmpPlayer smpPlayer = playerManager.getSmpPlayer(player.getUniqueId());
        if (smpPlayer == null) return;
        smpPlayer.getActiveEnchants().forEach((enchants, integer) -> {
            enchants.getEnchant().onKillEntity(event, integer);
        });
        smpPlayer.getActiveAttributes().forEach((attributes, aDouble) -> {
            attributes.getAttribute().onKillEntity(event, aDouble);
        });
    }
}
