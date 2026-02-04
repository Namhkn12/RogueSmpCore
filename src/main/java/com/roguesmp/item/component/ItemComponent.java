package com.roguesmp.item.component;

import com.roguesmp.context.ItemDataContext;
import com.roguesmp.context.ItemLoreContext;
import com.roguesmp.item.lore.LoreBuilder;
import com.roguesmp.player.SmpPlayer;
import io.papermc.paper.persistence.PersistentDataContainerView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * All implementing classes should be immutable!
 */
public interface ItemComponent {

    @NotNull ItemComponent copy();

    default void contributeLore(ItemLoreContext context) {

    }

    /**
     * For modifying vanilla ItemStack stuff
     */
    default void modifyStack(ItemDataContext context) {

    }
}
