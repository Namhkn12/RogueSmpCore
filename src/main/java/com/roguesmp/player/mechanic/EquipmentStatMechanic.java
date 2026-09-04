package com.roguesmp.player.mechanic;

import com.roguesmp.attribute.Attributes;
import com.roguesmp.constant.EquipSlot;
import com.roguesmp.enchant.Enchants;
import com.roguesmp.item.SmpItem;
import com.roguesmp.item.component.ItemComponentKeys;
import com.roguesmp.item.component.impl.EnchantComponent;
import com.roguesmp.item.component.impl.EquipAttributeComponent;
import com.roguesmp.player.SmpPlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Recomputes a slot's contribution to {@link SmpPlayer#getActiveAttributes()} /
 * {@link SmpPlayer#getActiveEnchants()} whenever its item changes, and syncs the net result to
 * vanilla Bukkit attributes. Runs via {@link #onEquipSlotChange}, which fires (for every mechanic,
 * though only this one does anything) before {@link #onEquipmentChange} is broadcast - every other
 * mechanic reacting to that broadcast (e.g. {@link AttributeMechanic}, {@link EnchantMechanic})
 * depends on this having already updated the player's active totals.
 * <p>
 * {@code lastAppliedAttributes}/{@code lastAppliedEnchants} are this mechanic's own per-slot
 * bookkeeping (one instance per {@code SmpPlayer}, same shape as {@link AttributeMechanic}'s own
 * {@code lastKnownValues}) - what was actually folded in for each slot last time, as owned
 * snapshots rather than live references into the item's components, so removing a slot's
 * contribution subtracts THIS snapshot rather than re-reading the (possibly already-mutated) item
 * live - stays correct even when the "old" and "new" {@code SmpItem} for a slot turn out to be the
 * same cached instance (e.g. a unique item whose attributes changed in place, then got its stack
 * regenerated and re-equipped with itself).
 */
public class EquipmentStatMechanic implements PlayerMechanic {

    private final Map<EquipSlot, Map<Attributes, Double>> lastAppliedAttributes = new EnumMap<>(EquipSlot.class);
    private final Map<EquipSlot, Map<Enchants, Integer>> lastAppliedEnchants = new EnumMap<>(EquipSlot.class);

    @Override
    public int getPriority() {
        return 85; // Just ahead of AttributeMechanic/EnchantMechanic (~100/90)
    }

    @Override
    public void onEquipSlotChange(Player player, EquipSlot slot, @Nullable SmpItem newItem, SmpPlayer smpPlayer) {
        Set<Attributes> affected = new HashSet<>();

        Map<Attributes, Double> previousAttributes = lastAppliedAttributes.remove(slot);
        if (previousAttributes != null) {
            previousAttributes.forEach((attr, val) -> {
                affected.add(attr);
                smpPlayer.mergeAttributeDelta(attr, -val);
            });
        }

        Map<Enchants, Integer> previousEnchants = lastAppliedEnchants.remove(slot);
        if (previousEnchants != null) {
            previousEnchants.forEach((ench, level) -> smpPlayer.mergeEnchantDelta(ench, -level));
        }

        boolean itemActive = newItem != null && !newItem.hasComponent(ItemComponentKeys.BROKEN);
        smpPlayer.setEquipmentSlot(slot, itemActive ? newItem : null);

        if (itemActive) {
            // A wrong-weapon-type mainhand item still equips (so ClassRestrictionMechanic can find
            // and block damage dealt with it) but contributes no attribute bonus - wielding it
            // shouldn't buff you if your class won't even let you swing it.
            boolean weaponRestricted = slot == EquipSlot.MAINHAND && ClassRestrictionMechanic.isRestrictedWeapon(smpPlayer, newItem.getBaseItem());

            EquipAttributeComponent newComp = newItem.getComponent(ItemComponentKeys.ATTRIBUTE);
            if (!weaponRestricted && newComp != null && newComp.getSlot() == slot) {
                // Owned copy, not the live unmodifiable view - must stay a fixed point-in-time
                // snapshot even after the component's own finalAttributes mutate further.
                Map<Attributes, Double> snapshot = new EnumMap<>(newComp.getFinalAttributes());
                snapshot.forEach((attr, val) -> {
                    affected.add(attr);
                    smpPlayer.mergeAttributeDelta(attr, val);
                });
                if (!snapshot.isEmpty()) lastAppliedAttributes.put(slot, snapshot);
            }

            Map<Enchants, Integer> enchantSnapshot = collectApplicableEnchants(newItem, slot);
            enchantSnapshot.forEach(smpPlayer::mergeEnchantDelta);
            if (!enchantSnapshot.isEmpty()) lastAppliedEnchants.put(slot, enchantSnapshot);
        }

        for (Attributes attr : affected) {
            Double total = smpPlayer.getActiveAttributes().get(attr); // Null if pruned above
            if (total == null) {
                attr.getAttribute().removeVanillaAttribute(player);
            } else {
                attr.getAttribute().addVanillaAttribute(player, total);
            }
        }
    }

    private Map<Enchants, Integer> collectApplicableEnchants(SmpItem item, EquipSlot slot) {
        EnchantComponent enchantComp = item.getComponent(ItemComponentKeys.ENCHANT);
        if (enchantComp == null) return Map.of();

        Map<Enchants, Integer> result = new EnumMap<>(Enchants.class);
        enchantComp.getTotalEnchants().forEach((ench, level) -> {
            // Only apply if the enchant is valid for the current equipment slot
            if (ench.getEnchant().getActiveSlots().contains(slot)) {
                result.put(ench, level);
            }
        });
        return result;
    }
}
