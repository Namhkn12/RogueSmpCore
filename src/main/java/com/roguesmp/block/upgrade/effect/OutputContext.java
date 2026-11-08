package com.roguesmp.block.upgrade.effect;

import com.roguesmp.block.BlockDrop;
import com.roguesmp.block.StoredItem;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Function;

public record OutputContext(@Nullable StoredItem input, List<StoredItem> outputs, Function<BlockDrop, @Nullable StoredItem> roll) {
}
