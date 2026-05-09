package com.roguesmp.item.component.impl;

import com.roguesmp.context.ItemDataContext;
import com.roguesmp.item.component.ItemComponent;
import io.papermc.paper.datacomponent.DataComponentTypes;
import org.jetbrains.annotations.NotNull;

public record DurabilityComponent(int maxDurability) implements ItemComponent {

    @Override
    public @NotNull ItemComponent copy() {
        return new DurabilityComponent(maxDurability);
    }

    @Override
    public void modifyStack(ItemDataContext context) {
        context.newStack().setData(DataComponentTypes.MAX_DAMAGE, maxDurability);
        Integer duraDmg = context.oldStack().getData(DataComponentTypes.DAMAGE);
        if (duraDmg != null && duraDmg > 0) {
            context.newStack().setData(DataComponentTypes.DAMAGE, duraDmg);
        }

    }
}
