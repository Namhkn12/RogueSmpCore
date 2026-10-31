package com.roguesmp.block;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.block.manager.BlockManager;
import com.roguesmp.codec.Codec;
import com.roguesmp.codec.DataResult;
import com.roguesmp.registry.Holder;
import com.roguesmp.registry.Registries;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;

public sealed interface BlockRef {
    String VANILLA_PREFIX = "minecraft:";

    Codec<BlockRef> CODEC = Codec.STRING.comapFlatMap(BlockRef::parse, BlockRef::id);

    String id();

    void place(Block target);

    static DataResult<BlockRef> parse(String id) {
        if (!id.startsWith(VANILLA_PREFIX)) return DataResult.success(new Custom(Registries.BLOCK.getHolder(id)));
        try {
            return DataResult.success(new Vanilla(id, Bukkit.createBlockData(id)));
        } catch (IllegalArgumentException e) {
            return DataResult.error("Unknown vanilla block data: " + id);
        }
    }

    record Vanilla(String id, org.bukkit.block.data.BlockData data) implements BlockRef {
        @Override
        public void place(Block target) {
            target.setBlockData(data, false);
        }
    }

    record Custom(Holder<BlockData> holder) implements BlockRef {
        @Override
        public String id() {
            return holder.getId();
        }

        @Override
        public void place(Block target) {
            if (!holder.isBound()) {
                RogueSmpCore.LOGGER.warn("Unknown block '{}' — skipping.", id());
                return;
            }
            BlockManager.getInstance().place(target, id(), null, null, null);
        }
    }
}
