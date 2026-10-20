package com.roguesmp.loot.function;

import com.roguesmp.codec.Codec;
import com.roguesmp.loot.context.LootContext;
import com.roguesmp.registry.Registries;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public interface LootFunction {
    Codec<LootFunction> CODEC = Codec.dispatch(LootFunction::getTypeId, Registries.LOOT_FUNCTION_CODEC::getOrThrow);

    String getTypeId();

    List<ItemStack> apply(List<ItemStack> items, LootContext context);
}
