package com.roguesmp.item.modifier;

import com.roguesmp.attribute.Attributes;
import com.roguesmp.item.SmpItem;
import com.roguesmp.item.ability.ItemAbility;
import com.roguesmp.item.component.ItemComponentKeys;
import com.roguesmp.item.component.impl.EquipAttributeComponent;
import com.roguesmp.item.component.impl.PassiveAbilityComponent;
import com.roguesmp.player.SmpPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * Feeds every attached {@link ItemAbility#provideAttribute} contribution (e.g. {@code UnyieldingEdge}'s
 * durability-scaling ATK bonus) into {@link EquipAttributeComponent}. Same unconditional
 * put-or-remove shape as {@link GemModifier} - if every ability's contribution goes back to empty
 * (e.g. durability fully repaired), the stale entry still needs clearing, not just skipping.
 */
public class ItemAbilityModifier implements ItemModifier {
    @Override
    public void collectAndApply(SmpItem smpItem, @Nullable SmpPlayer player) {
        PassiveAbilityComponent abilityComponent = smpItem.getComponent(ItemComponentKeys.PASSIVE_ABILITY);
        if (abilityComponent == null) return;

        EquipAttributeComponent equipAttributeComponent = smpItem.getComponent(ItemComponentKeys.ATTRIBUTE);
        if (equipAttributeComponent == null) return;

        Map<Attributes, Double> modifiers = new EnumMap<>(Attributes.class);
        for (ItemAbility ability : abilityComponent.getAbilities()) {
            ability.provideAttribute(player, smpItem).forEach((attribute, value) ->
                    modifiers.merge(attribute, value, Double::sum));
        }

        if (modifiers.isEmpty()) {
            equipAttributeComponent.removeModifier("item_ability_modifier");
        } else {
            equipAttributeComponent.putModifier("item_ability_modifier", modifiers);
        }
    }
}
