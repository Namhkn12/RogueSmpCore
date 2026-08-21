package com.roguesmp.player.mechanic;

import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent;
import com.roguesmp.item.component.ItemComponentKeys;
import com.roguesmp.constant.DamageType;
import com.roguesmp.constant.EquipSlot;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.event.DurabilityChangedEvent;
import com.roguesmp.item.SmpItem;
import com.roguesmp.item.component.impl.DurabilityComponent;
import com.roguesmp.item.component.impl.NameComponent;
import com.roguesmp.player.PlayerProjectile;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.Utils;
import io.papermc.paper.registry.keys.SoundEventKeys;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Set;

public class DurabilityLossMechanic implements PlayerMechanic {

    @Override public int getPriority() { return 600; } // Runs near the absolute end

    @Override
    public void onBlockBreak(BlockBreakEvent event, SmpPlayer player) {
        if (event.isCancelled()) return;
        Block block = event.getBlock();
        if (!block.getType().isCollidable()) return;
        SmpItem currentMainhand = player.getItemAtEquipSlot(EquipSlot.MAINHAND);
        damageItem(player, currentMainhand, 1, EquipSlot.MAINHAND);

    }

    @Override
    public void onProjectileLaunch(PlayerLaunchProjectileEvent event, SmpPlayer player) {
        if (event.isCancelled()) return;

        PlayerProjectile playerProjectile = player.getProjectile(event.getProjectile().getUniqueId());
        if (playerProjectile != null && playerProjectile.shouldReduceDurability()) {
            SmpItem currentMainhand = player.getItemAtEquipSlot(EquipSlot.MAINHAND);
            damageItem(player, currentMainhand, 1, EquipSlot.MAINHAND);
        }
    }

    @Override
    public void onShootArrow(EntityShootBowEvent event, SmpPlayer player) {
        if (event.isCancelled()) return;

        PlayerProjectile playerProjectile = player.getProjectile(event.getProjectile().getUniqueId());
        if (playerProjectile != null && playerProjectile.shouldReduceDurability()) {
            SmpItem currentMainhand = player.getItemAtEquipSlot(EquipSlot.MAINHAND);
            damageItem(player, currentMainhand, 1, EquipSlot.MAINHAND);
        }
    }

    @Override
    public void onDamageEntity(DamageEvent event, SmpPlayer player) {
        if (event.isCancelled()) return;

        if (event.getDamager() instanceof Player && event.getDamageType() == DamageType.MELEE) {
            SmpItem currentMainhand = player.getItemAtEquipSlot(EquipSlot.MAINHAND);
            damageItem(player, currentMainhand, 1, EquipSlot.MAINHAND);
        }
    }

    private static final Set<EquipSlot> affectedSlot = Set.of(EquipSlot.CHEST, EquipSlot.HEAD, EquipSlot.LEGS, EquipSlot.FEET, EquipSlot.OFFHAND);

    @Override
    public void onHurt(DamageEvent event, SmpPlayer player) {
        if (event.isCancelled()) return;

        DamageType damageType = event.getDamageType();
        if (damageType != DamageType.AILMENT && damageType != DamageType.FALL && damageType != DamageType.THORNS) {
            for (EquipSlot equipSlot : affectedSlot) {
                SmpItem smpItem = player.getItemAtEquipSlot(equipSlot);
                damageItem(player, smpItem, 1, equipSlot);
            }
        }
    }

    private static void damageItem(SmpPlayer player, SmpItem smpItem, int amount, EquipSlot equipSlot) {
        if (smpItem == null || amount <= 0) return;
        if (smpItem.hasComponent(ItemComponentKeys.BROKEN)) return;

        DurabilityComponent durabilityComponent = smpItem.getComponent(ItemComponentKeys.DURABILITY);
        if (durabilityComponent == null) return;

        Player bukkitPlayer = player.getBukkitPlayer();
        EquipmentSlot equipmentSlot = equipSlot.getVanillaSlot();
        if (bukkitPlayer == null || equipmentSlot == null) return;

        int maxDurability = durabilityComponent.maxDurability();
        int oldDurability = durabilityComponent.currentDurability();

        // Fired before anything is applied - ItemAbilityMechanic (via PlayerListener/SmpPlayer)
        // dispatches this synchronously to every ItemAbility on this item, so by the time
        // callEvent returns, an ability may have adjusted the change amount (reduce/increase
        // wear) or vetoed it outright by zeroing it.
        DurabilityChangedEvent durabilityChangedEvent = new DurabilityChangedEvent(smpItem, player, -amount);
        Bukkit.getPluginManager().callEvent(durabilityChangedEvent);

        int newDurability = Math.max(0, Math.min(maxDurability, oldDurability + durabilityChangedEvent.getChangeAmount()));
        if (newDurability == oldDurability) return; // Nothing actually changed

        durabilityComponent.setCurrentDurability(newDurability);

        EntityEquipment equipment = bukkitPlayer.getEquipment();
        ItemStack currentItem = equipment.getItem(equipmentSlot);

        NameComponent nameComponent = smpItem.getComponent(ItemComponentKeys.ITEM_NAME);
        String itemName = "";
        if (nameComponent != null) {
            itemName = nameComponent.value();
        }

        if (newDurability == 0) {
            // Always regenerates - the item is always swapped to its broken form.
            equipment.setItem(equipmentSlot, smpItem.generateItemStack(player, currentItem.getAmount()));

            bukkitPlayer.sendMessage(Utils.fromString("<red>HỎNG! " + itemName + "<red> của bạn đã hỏng hoàn toàn. Các chỉ số sẽ bị vô hiệu!"));
            bukkitPlayer.playSound(Sound.sound(SoundEventKeys.ENTITY_ITEM_BREAK, Sound.Source.PLAYER, 1f, 1f));
        } else if (durabilityChangedEvent.shouldUpdateItem()) {
            equipment.setItem(equipmentSlot, smpItem.generateItemStack(player, currentItem.getAmount()));
        } else {
            // Cheap path - persist the new durability without rebuilding the whole stack.
            currentItem.editPersistentDataContainer(durabilityComponent::save);
        }

        if (newDurability != 0 && maxDurability > 0) {
            double oldPercent = (double) oldDurability / maxDurability;
            double newPercent = (double) newDurability / maxDurability;
            double threshold = 0.05; // 5%

            if (oldPercent > threshold && newPercent <= threshold) {
                bukkitPlayer.sendMessage(Utils.fromString("<red>CHÚ Ý! " + itemName + "<red> của bạn sắp hỏng (Còn " + newDurability + " độ bền). Các chỉ số của " + itemName + "<red> sẽ bị vô hiệu khi bị hỏng!"));
                bukkitPlayer.playSound(Sound.sound(SoundEventKeys.ENTITY_ITEM_BREAK, Sound.Source.PLAYER, 0.5f, 1.5f));
            }
        }
    }
}
