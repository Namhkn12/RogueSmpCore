package com.roguesmp.item.component.impl;

import com.roguesmp.context.ItemDataContext;
import com.roguesmp.item.component.ItemComponent;
import com.roguesmp.player.SmpPlayer;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.persistence.PersistentDataContainerView;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

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
