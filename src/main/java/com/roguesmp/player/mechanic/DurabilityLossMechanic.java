package com.roguesmp.player.mechanic;

import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent;
import com.roguesmp.item.component.ItemComponentKeys;
import com.roguesmp.constant.DamageType;
import com.roguesmp.constant.EquipSlot;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.item.SmpItem;
import com.roguesmp.item.component.impl.DurabilityComponent;
import com.roguesmp.item.component.impl.NameComponent;
import com.roguesmp.player.PlayerProjectile;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.Utils;
import io.papermc.paper.registry.keys.SoundEventKeys;
import net.kyori.adventure.sound.Sound;
import org.bukkit.GameMode;
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
        if (player.getBukkitPlayer().getGameMode() == GameMode.CREATIVE) return;
        if (smpItem == null || amount <= 0) return;
        if (smpItem.hasComponent(ItemComponentKeys.BROKEN)) return;

        DurabilityComponent durabilityComponent = smpItem.getComponent(ItemComponentKeys.DURABILITY);
        if (durabilityComponent == null) return;

        Player bukkitPlayer = player.getBukkitPlayer();
        EquipmentSlot equipmentSlot = equipSlot.getVanillaSlot();
        if (bukkitPlayer == null || equipmentSlot == null) return;

        int maxDurability = durabilityComponent.maxDurability();
        int oldDurability = durabilityComponent.currentDurability();
        int newDurability = Math.max(0, durabilityComponent.currentDurability() - amount);
        durabilityComponent.setCurrentDurability(newDurability);

        EntityEquipment equipment = bukkitPlayer.getEquipment();
        ItemStack currentItem = equipment.getItem(equipmentSlot);

        NameComponent nameComponent = smpItem.getComponent(ItemComponentKeys.ITEM_NAME);
        String itemName = "";
        if (nameComponent != null) {
            itemName = nameComponent.value();
        }

        if (newDurability == 0) {
            // Handle broken item replacement
            ItemStack brokenItemStack = smpItem.generateItemStack(player, currentItem.getAmount());
            equipment.setItem(equipmentSlot, brokenItemStack);

            bukkitPlayer.sendMessage(Utils.fromString("<red>HỎNG! " + itemName + "<red> của bạn đã hỏng hoàn toàn. Các chỉ số sẽ bị vô hiệu!"));
            bukkitPlayer.playSound(Sound.sound(SoundEventKeys.ENTITY_ITEM_BREAK, Sound.Source.PLAYER, 1f, 1f));
        } else {
            // Save the new durability to the Persistent Data Container
            currentItem.editPersistentDataContainer(durabilityComponent::save);

            if (maxDurability > 0) {
                double oldPercent = (double) oldDurability / maxDurability;
                double newPercent = (double) newDurability / maxDurability;
                double threshold = 0.05; // 5%

                if (oldPercent > threshold && newPercent <= threshold) {
                    bukkitPlayer.sendMessage(Utils.fromString("<red>CHÚ Ý! " + itemName + "<red> của bạn sắp hỏng. Các chỉ số của " + itemName + "<red> sẽ bị vô hiệu khi bị hỏng!"));
                    bukkitPlayer.playSound(Sound.sound(SoundEventKeys.ENTITY_ITEM_BREAK, Sound.Source.PLAYER, 0.5f, 1.5f));
                }
            }
        }

    }
}
