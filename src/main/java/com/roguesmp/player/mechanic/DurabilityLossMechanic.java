package com.roguesmp.player.mechanic;

import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent;
import com.roguesmp.constant.ComponentKeys;
import com.roguesmp.constant.DamageType;
import com.roguesmp.constant.EquipSlot;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.item.SmpItem;
import com.roguesmp.item.component.impl.DurabilityComponent;
import com.roguesmp.player.PlayerProjectile;
import com.roguesmp.player.SmpPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.Set;

public class DurabilityLossMechanic implements PlayerMechanic {

    @Override public int getPriority() { return 600; } // Runs near the absolute end

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
        if (smpItem != null) {
            DurabilityComponent durabilityComponent = smpItem.getComponent(ComponentKeys.DURABILITY);
            if (durabilityComponent != null) {
                durabilityComponent.setCurrentDurability(durabilityComponent.currentDurability() - amount);
                Player bukkitPlayer = player.getBukkitPlayer();
                if (bukkitPlayer != null) {
                    EquipmentSlot equipmentSlot = equipSlot.getVanillaSlot();
                    if (equipmentSlot != null) {
                        bukkitPlayer.getEquipment().getItem(equipmentSlot).editPersistentDataContainer(durabilityComponent::save);
                    }

                }
            }
        }
    }
}
