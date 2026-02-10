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
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class SmpPlayer {
    private final UUID uuid;
    private final Map<Enchants, Integer> activeEnchants;
    private final Map<Attributes, Double> activeAttributes;

    public SmpPlayer(UUID uuid) {
        this.uuid = uuid;
        activeEnchants = new EnumMap<>(Enchants.class);
        activeAttributes = new EnumMap<>(Attributes.class);
    }

    public void updateSlotStat(Player player, EquipSlot slot, @Nullable SmpItem oldItem, @Nullable SmpItem newItem) {
        if (oldItem != null) {
            EnchantComponent oldEnchant = oldItem.getComponent(ComponentKeys.ENCHANT);
            EquipAttributeComponent oldAttribute = oldItem.getComponent(ComponentKeys.ATTRIBUTE);

            if (oldEnchant != null) {
                oldEnchant.getEnchants().forEach((enchants, integer) -> {
                    Set<EquipSlot> activeSlot = enchants.getEnchant().getActiveSlots();
                    if (activeSlot.contains(slot)) {
                        activeEnchants.merge(enchants, -integer, (integer1, integer2) -> {
                            int res = integer1 + integer2;
                            if (res == 0) return null;
                            return res;
                        });
                    }
                });
            }
            if (oldAttribute != null) {
                oldAttribute.getAttributes().forEach((attributes, aDouble) -> {
                    if (oldAttribute.getSlot() == slot) {
                        attributes.getAttribute().removeVanillaAttribute(player);
                        activeAttributes.merge(attributes, -aDouble, (aDouble1, aDouble2) -> {
                            double res = aDouble1 + aDouble2;
                            if (Utils.isEffectiveZero(res)) return null;
                            return res;
                        });
                    }

                });
            }

        }

        if (newItem != null) {
            EnchantComponent newEnchant = newItem.getComponent(ComponentKeys.ENCHANT);
            EquipAttributeComponent newAttribute = newItem.getComponent(ComponentKeys.ATTRIBUTE);

            if (newEnchant != null) {
                newEnchant.getEnchants().forEach((enchants, integer) -> {
                    Set<EquipSlot> activeSlot = enchants.getEnchant().getActiveSlots();
                    if (activeSlot.contains(slot)) {
                        activeEnchants.merge(enchants, integer, (integer1, integer2) -> {
                            int res = integer1 + integer2;
                            if (res == 0) return null;
                            return res;
                        });
                    }
                });
            }
            if (newAttribute != null) {
                newAttribute.getAttributes().forEach((attributes, aDouble) -> {
                    if (newAttribute.getSlot() == slot) {
                        attributes.getAttribute().addVanillaAttribute(player, aDouble);
                        activeAttributes.merge(attributes, aDouble, (aDouble1, aDouble2) -> {
                            double res = aDouble1 + aDouble2;
                            if (Utils.isEffectiveZero(res)) return null;
                            return res;
                        });
                    }
                });
            }
        }


    }

    public SmpPlayer(Player player) {
        this(player.getUniqueId());
    }

    public Map<Enchants, Integer> getActiveEnchants() {
        return Map.copyOf(activeEnchants);
    }

    public Map<Attributes, Double> getActiveAttributes() {
        return Map.copyOf(activeAttributes);
    }

    public @Nullable Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }
}
