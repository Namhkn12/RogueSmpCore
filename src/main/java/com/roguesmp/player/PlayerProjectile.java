package com.roguesmp.player;

import com.roguesmp.constant.Attributes;
import com.roguesmp.constant.ComponentKeys;
import com.roguesmp.constant.Enchants;
import com.roguesmp.constant.EquipSlot;
import com.roguesmp.item.SmpItem;
import com.roguesmp.item.component.impl.EnchantComponent;
import com.roguesmp.item.component.impl.EquipAttributeComponent;
import com.roguesmp.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ThrowableProjectile;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * For tracking projectiles (snapshot stat,...)
 */
public class PlayerProjectile {
    // Arrow entity uuid
    private final UUID uuid;
    private final Map<Enchants, Integer> activeEnchants = new EnumMap<>(Enchants.class);
    private final Map<Attributes, Double> activeAttributes = new EnumMap<>(Attributes.class);

    public PlayerProjectile(SmpPlayer player, Projectile projectile, Map<Enchants, Integer> snapshotEnchant, Map<Attributes, Double> snapshotAttribute) {
        this.uuid = projectile.getUniqueId();
        this.activeEnchants.putAll(snapshotEnchant);
        this.activeAttributes.putAll(snapshotAttribute);
        SmpItem smpItem = null;
        if (projectile instanceof ThrowableProjectile throwable) {
            smpItem = new SmpItem(throwable.getItem());
        } else if (projectile instanceof AbstractArrow arrow) {
            smpItem = new SmpItem(arrow.getItemStack());
        }
        if (smpItem != null) {
            mergeProjectileStat(player, this.activeEnchants, this.activeAttributes, smpItem);
        }

    }

    private static void mergeProjectileStat(SmpPlayer player, Map<Enchants, Integer> activeEnchants, Map<Attributes, Double> activeAttributes, SmpItem smpItem) {
        smpItem.applyModifiers(player);
        EnchantComponent enchantComponent = smpItem.getComponent(ComponentKeys.ENCHANT);
        if (enchantComponent != null) {
            enchantComponent.getEnchants().forEach((enchants, integer) -> {
                Set<EquipSlot> activeSlot = enchants.getEnchant().getActiveSlots();
                if (activeSlot.contains(EquipSlot.PROJECTILE)) {
                    activeEnchants.merge(enchants, integer, (integer1, integer2) -> {
                        int res = integer1 + integer2;
                        if (res == 0) return null;
                        return res;
                    });
                }
            });
        }
        EquipAttributeComponent attributeComponent = smpItem.getComponent(ComponentKeys.ATTRIBUTE);
        if (attributeComponent != null) {
            attributeComponent.getAttributes().forEach((attributes, aDouble) -> {
                if (attributeComponent.getSlot() == EquipSlot.PROJECTILE) {
                    activeAttributes.merge(attributes, aDouble, (aDouble1, aDouble2) -> {
                        double res = aDouble1 + aDouble2;
                        if (Utils.isEffectiveZero(res)) return null;
                        return res;
                    });
                }
            });
        }
    }

    public Map<Enchants, Integer> getActiveEnchants() {
        return activeEnchants;
    }

    public Map<Attributes, Double> getActiveAttributes() {
        return activeAttributes;
    }

    public @Nullable Projectile getProjectile() {
        return (Projectile) Bukkit.getEntity(uuid);
    }
}
