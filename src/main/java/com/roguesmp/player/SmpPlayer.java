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
import org.bukkit.entity.Projectile;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;

public class SmpPlayer {
    private final UUID uuid;
    private final Map<Enchants, Integer> activeEnchants;
    private final Map<Attributes, Double> activeAttributes;

    private final Map<UUID, PlayerProjectile> projectiles = new HashMap<>();

    public SmpPlayer(UUID uuid) {
        this.uuid = uuid;
        activeEnchants = new EnumMap<>(Enchants.class);
        activeAttributes = new EnumMap<>(Attributes.class);
    }

    public SmpPlayer(Player player) {
        this(player.getUniqueId());
    }


    public void updateSlotStat(Player player, EquipSlot slot, @Nullable SmpItem oldItem, @Nullable SmpItem newItem) {
        activeAttributes.forEach((attributes, aDouble) -> {
            attributes.getAttribute().removeVanillaAttribute(player);
        });

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
                        activeAttributes.merge(attributes, aDouble, (aDouble1, aDouble2) -> {
                            double res = aDouble1 + aDouble2;
                            if (Utils.isEffectiveZero(res)) return null;
                            return res;
                        });
                    }
                });
            }
        }

        activeAttributes.forEach((attributes, aDouble) -> {
            attributes.getAttribute().addVanillaAttribute(player, aDouble);
        });

    }

    public @Unmodifiable Map<Enchants, Integer> getActiveEnchants() {
        return Map.copyOf(activeEnchants);
    }

    public @Unmodifiable Map<Attributes, Double> getActiveAttributes() {
        return Map.copyOf(activeAttributes);
    }

    public @Nullable Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }

    public @Nullable PlayerProjectile getProjectile(UUID uuid) {
        return projectiles.get(uuid);
    }

    public void trackProjectile(Projectile projectile) {
        projectiles.put(projectile.getUniqueId(), new PlayerProjectile(this, projectile, activeEnchants, activeAttributes));
    }

    public @Nullable PlayerProjectile untrackProjectile(UUID uuid) {
        return projectiles.remove(uuid);
    }
}
