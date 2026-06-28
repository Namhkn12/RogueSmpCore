package com.roguesmp.item.component;

import com.roguesmp.context.ItemDataContext;
import com.roguesmp.context.ItemLoreContext;
import io.papermc.paper.persistence.PersistentDataContainerView;
import org.bukkit.persistence.PersistentDataContainer;
import org.jetbrains.annotations.NotNull;

public interface ItemComponent {

    /**
     * Copy BaseItem component into active SmpItem instance
     * @return A fresh/new copy
     */
    @NotNull ItemComponent copy();

    default void contributeLore(ItemLoreContext context) {

    }

    /**
     * For modifying vanilla ItemStack stuff
     */
    default void modifyStack(ItemDataContext context) {

    }

    /**
     * Load data from pdc
     * @param pdc ItemStack's pdc
     */
    default void load(PersistentDataContainerView pdc) {

    }

    /**
     * Save data to pdc
     * @param pdc ItemStack's pdc
     */
    default void save(PersistentDataContainer pdc) {

    }
}
