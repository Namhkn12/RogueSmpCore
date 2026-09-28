package com.roguesmp.block.persistence;

import com.roguesmp.block.BlockPos;
import com.roguesmp.codec.Codec;
import com.roguesmp.utils.InventoryBase64;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

public final class BlockCodecs {

    public static final Codec<Map<Integer, ItemStack>> ITEM_MAP =
            Codec.STRING.xmap(InventoryBase64::itemMapFromBase64, InventoryBase64::itemMapToBase64);

    public static final Codec<List<BlockPos>> POSITIONS = Codec.listOf(BlockPos.CODEC);

    private BlockCodecs() {}
}
