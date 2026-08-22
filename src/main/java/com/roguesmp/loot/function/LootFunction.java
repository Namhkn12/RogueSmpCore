package com.roguesmp.loot.function;

import com.roguesmp.codec.Codec;
import com.roguesmp.loot.context.LootContext;
import com.roguesmp.registry.Registries;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Post-processes the {@link ItemStack}s produced by a picked {@link com.roguesmp.loot.LootEntry}
 * before they're added to the roll's results — count scaling, etc. Functions attached to an
 * entry run in declaration order, each receiving the previous function's output.
 *
 * <p>New function types register a {@code Codec} into
 * {@link com.roguesmp.loot.function.LootFunctions} to become usable from loot table JSON under
 * an entry's {@code "functions"} array, dispatched by the {@code "type"} field.
 */
public interface LootFunction {

    Codec<LootFunction> CODEC = Codec.dispatch(LootFunction::getTypeId, Registries.LOOT_FUNCTION_CODEC::getOrThrow);

    String getTypeId();

    /**
     * @param items   the stacks produced so far (by the entry itself, or by a previous function
     *                in the chain) — implementations may mutate and return the same list, or
     *                return a new one
     * @param context the roll's context
     * @return the resulting stacks, passed to the next function in the chain
     */
    List<ItemStack> apply(List<ItemStack> items, LootContext context);
}
