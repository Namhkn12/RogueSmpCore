package com.roguesmp.item.component.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.context.ItemLoreContext;
import com.roguesmp.item.BaseItem;
import com.roguesmp.item.component.ItemComponent;
import com.roguesmp.item.component.ItemComponentKeys;
import com.roguesmp.registry.Registries;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record EnergySourceComponent(int energyPerTick, int burnTicks) implements ItemComponent {

    private static final int LORE_PRIORITY = 80;

    public static final Codec<EnergySourceComponent> CODEC = Codec.composite(
            Codec.INT.fieldOf("energy_per_tick").forGetter(EnergySourceComponent::energyPerTick),
            Codec.INT.fieldOf("burn_ticks").forGetter(EnergySourceComponent::burnTicks),
            EnergySourceComponent::new
    );

    public static @Nullable EnergySourceComponent of(String itemId) {
        BaseItem item = Registries.ITEM.get(itemId);
        return item == null ? null : item.getComponent(ItemComponentKeys.ENERGY_SOURCE);
    }

    public int totalEnergy() {
        return energyPerTick * burnTicks;
    }

    @Override
    public @NotNull ItemComponent copy() {
        return this;
    }

    @Override
    public void contributeLore(ItemLoreContext context) {
        context.builder().putLines(LORE_PRIORITY, List.of(
                Utils.text("Năng lượng: " + energyPerTick + "/tick trong " + Utils.formatDecimal(burnTicks / 20d) + "s", NamedTextColor.YELLOW),
                Utils.text("Tổng: " + Utils.formatMoney(totalEnergy()), NamedTextColor.GRAY)
        ));
    }
}
