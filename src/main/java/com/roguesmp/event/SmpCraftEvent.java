package com.roguesmp.event;

import com.roguesmp.crafting.recipe.CraftingRecipe;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Fired once per craft action after ingredients are consumed. {@code result} is a single craft's output and
 * {@code times} how many crafts the action performed; {@code recipe} is null for vanilla fallback recipes.
 */
public class SmpCraftEvent extends PlayerEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    private final @Nullable CraftingRecipe recipe;
    private final ItemStack result;
    private final int times;

    public SmpCraftEvent(Player player, @Nullable CraftingRecipe recipe, ItemStack result, int times) {
        super(player);
        this.recipe = recipe;
        this.result = result;
        this.times = times;
    }

    public @Nullable CraftingRecipe getRecipe() {
        return recipe;
    }

    public ItemStack getResult() {
        return result;
    }

    public int getTimes() {
        return times;
    }

    public int getTotalAmount() {
        return result.getAmount() * times;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
