package com.roguesmp.block.transfer;

import com.roguesmp.block.StoredItem;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface ItemHandler {

    int slots();

    @Nullable StoredItem get(int slot);

    @Nullable StoredItem extract(int slot, int amount, boolean simulate);

    /** Returns how many of {@code item} were (or, when simulating, would be) accepted. */
    int insert(StoredItem item, boolean simulate);

    default List<StoredItem> demand() {
        return List.of();
    }
}
