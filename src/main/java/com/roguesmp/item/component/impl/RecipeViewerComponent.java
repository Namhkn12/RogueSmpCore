package com.roguesmp.item.component.impl;

import com.roguesmp.utils.ItemRef;
import com.roguesmp.codec.Codec;
import com.roguesmp.context.ItemLoreContext;
import com.roguesmp.crafting.gui.RecipeViewerGui;
import com.roguesmp.crafting.recipe.CraftingRecipe;
import com.roguesmp.item.component.InteractableComponent;
import com.roguesmp.item.component.ItemComponent;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.registry.Registries;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record RecipeViewerComponent(String preferredRecipe) implements InteractableComponent {

    public static final Codec<RecipeViewerComponent> CODEC = Codec.composite(
            Codec.STRING.optionalFieldOf("preferred_recipe", "").forGetter(RecipeViewerComponent::preferredRecipe),
            RecipeViewerComponent::new
    );

    private static final int LORE_PRIORITY = 950;

    @Override
    public @NotNull ItemComponent copy() {
        return this;
    }

    @Override
    public void contributeLore(ItemLoreContext context) {
        context.builder().putLines(LORE_PRIORITY, List.of(Utils.text("Chuột phải để xem công thức", NamedTextColor.YELLOW)));
    }

    @Override
    public void onItemInteract(SmpPlayer smpPlayer, PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR) return;

        String itemKey = ItemRef.idOf(event.getItem());
        if (itemKey == null) return;

        Player player = event.getPlayer();
        CraftingRecipe preferred = preferredRecipe.isBlank() ? null : Registries.CRAFTING_RECIPE.get(preferredRecipe);
        RecipeViewerGui.forUses(player, itemKey, preferred).showInventory(player);
    }
}
