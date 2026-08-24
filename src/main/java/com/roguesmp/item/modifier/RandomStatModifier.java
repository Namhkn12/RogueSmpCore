package com.roguesmp.item.modifier;

import com.roguesmp.attribute.Attributes;
import com.roguesmp.item.SmpItem;
import com.roguesmp.item.component.ItemComponentKeys;
import com.roguesmp.item.component.impl.DurabilityComponent;
import com.roguesmp.item.component.impl.EquipAttributeComponent;
import com.roguesmp.item.component.impl.MagicPowerComponent;
import com.roguesmp.item.component.impl.RandomStatComponent;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.Utils;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

public class RandomStatModifier implements ItemModifier {

    private static final EnumSet<Attributes> AFFECTED_ATTRIBUTE = EnumSet.of(
            Attributes.MELEE_DAMAGE_BASE,
            Attributes.PROJECTILE_DAMAGE_BASE,
            Attributes.DEFENSE_FLAT
    );

    @Override
    public void collectAndApply(SmpItem smpItem, @Nullable SmpPlayer player, boolean isPreview) {
        RandomStatComponent rdc = smpItem.getComponent(ItemComponentKeys.RANDOM_STAT);
        if (rdc == null) return;

        double quality = rdc.getCurrentQuality();
        if (isPreview) quality = 1d;

        // Update lore provider state for MagicPowerComponent
        MagicPowerComponent magicPowerComponent = smpItem.getComponent(ItemComponentKeys.MAGIC_POWER);
        if (magicPowerComponent != null) {
            magicPowerComponent.setShouldProvideLore(false);

            // Calculate current magical power based on Quality %
            double mpMultiplier = -0.5 * (1.0 - quality);
            int maxMp = magicPowerComponent.getMax();
            int calculatedMp = (int) Math.round(maxMp + (maxMp * mpMultiplier));
            magicPowerComponent.setCurrent(calculatedMp);

            // Update with new mp
            rdc.setCurrentMagicPower(calculatedMp);
        }

        // Apply Attribute Modifier
        EquipAttributeComponent attributeComponent = smpItem.getComponent(ItemComponentKeys.ATTRIBUTE);
        if (attributeComponent == null) return;

        Map<Attributes, Double> attributeModifiers = new EnumMap<>(Attributes.class);
        Map<Attributes, Double> baseAttributes = attributeComponent.getBaseAttributes();

        double attributeMultiplier = -0.20 * (1.0 - quality);
        for (Attributes attr : AFFECTED_ATTRIBUTE) {
            Double baseValue = baseAttributes.get(attr);
            if (baseValue != null && !Utils.isEffectiveZero(baseValue)) {
                // Modifier calculated with base attribute value
                double modifierValue = baseValue * attributeMultiplier;
                attributeModifiers.put(attr, modifierValue);
            }
        }

        if (!attributeModifiers.isEmpty()) {
            attributeComponent.putModifier("quality_modifier", attributeModifiers);
        } else {
            attributeComponent.removeModifier("quality_modifier");
        }

        //Apply currentDurability change - only on this item's actual first-ever load, never again
        DurabilityComponent durabilityComponent = smpItem.getComponent(ItemComponentKeys.DURABILITY);
        if (durabilityComponent != null && rdc.wasFreshlyRolled()) {
            double durabilityMultiplier = -0.5 * (1.0 - quality);
            int maxDurability = durabilityComponent.maxDurability();
            int reducedDurability = (int) Math.round(maxDurability + (maxDurability * durabilityMultiplier));
            durabilityComponent.setCurrentDurability(Math.clamp(reducedDurability, 1, maxDurability));
        }
        rdc.consumeFreshRoll();
    }
}
