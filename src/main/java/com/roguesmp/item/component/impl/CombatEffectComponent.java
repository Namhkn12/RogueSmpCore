package com.roguesmp.item.component.impl;

import com.roguesmp.context.ItemLoreContext;
import com.roguesmp.item.component.ItemComponent;
import org.jetbrains.annotations.NotNull;

public class CombatEffectComponent implements ItemComponent {
    @Override
    public @NotNull ItemComponent copy() {
        return null;
    }

    @Override
    public void contributeLore(ItemLoreContext context) {
        ItemComponent.super.contributeLore(context);
    }
}
