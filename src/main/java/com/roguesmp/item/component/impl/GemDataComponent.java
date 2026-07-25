package com.roguesmp.item.component.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.constant.Attributes;
import com.roguesmp.constant.EquipSlot;
import com.roguesmp.context.ItemLoreContext;
import com.roguesmp.item.component.ItemComponent;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;

public class GemDataComponent implements ItemComponent {

    private final Map<EquipSlot, Map<Attributes, Double>> attributes = new EnumMap<>(EquipSlot.class);
    private final double successChance;

    public static final Codec<GemDataComponent> CODEC = Codec.composite(
            Codec.unboundedMap(
                    Codec.enumOf(EquipSlot.class),
                    Codec.unboundedMap(Codec.enumOf(Attributes.class), Codec.DOUBLE)
            ).fieldOf("attributes").forGetter(GemDataComponent::getAttributes),
            Codec.DOUBLE.optionalFieldOf("success_chance", 1d).forGetter(GemDataComponent::getSuccessChance),
            GemDataComponent::new
    );

    public GemDataComponent(Map<EquipSlot, Map<Attributes, Double>> attributes, double successChance) {
        if (successChance <= 0d) this.successChance = 0f;
        else this.successChance = successChance;
        attributes.forEach((equipSlot, attributesDoubleMap) -> {
            Map<Attributes, Double> copy = new EnumMap<>(Attributes.class);
            copy.putAll(attributesDoubleMap);
            this.attributes.put(equipSlot, copy);
        });
    }

    @Override
    public void contributeLore(ItemLoreContext context) {

        List<Component> lores = new ArrayList<>();

        attributes.forEach((slot, statMap) -> {
            lores.add(Utils.text("Khi khảm trang bị " + slot.getSimpleName() + ":", NamedTextColor.GRAY));

            statMap.forEach((attribute, value) -> {
                String sign = value >= 0 ? "+" : "";
                TextColor textColor = value >= 0 ? NamedTextColor.GREEN : NamedTextColor.RED;
                lores.add(Utils.text(" " + sign + Utils.formatDecimal(value) + " " + attribute.getAttribute().getSimpleName(), textColor));
            });

            lores.add(Component.empty());
        });

        TextColor chanceColor;
        if (successChance >= 0.9) {
            chanceColor = NamedTextColor.GREEN;
        } else if (successChance >= 0.7) {
            chanceColor = NamedTextColor.YELLOW;
        } else if (successChance >= 0.5) {
            chanceColor = NamedTextColor.GOLD;
        } else {
            chanceColor = NamedTextColor.RED;
        }

        lores.add(Utils.fromString(
                "<!i><gold>Tỉ lệ khảm thành công<white>: <" +
                        chanceColor.asHexString() + ">" +
                        Utils.formatDecimal(this.successChance * 100) +
                        "%</" + chanceColor.asHexString() + ">"
        ));

        context.builder().putLines(105, lores);
    }

    @Override
    public @NotNull ItemComponent copy() {
        return this;
    }

    public @Unmodifiable Map<EquipSlot, Map<Attributes, Double>> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    public double getSuccessChance() {
        return successChance;
    }
}
