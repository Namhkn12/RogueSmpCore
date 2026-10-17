package com.roguesmp.block.transfer;

import com.roguesmp.block.persistence.StateSection;
import com.roguesmp.codec.Codec;
import com.roguesmp.codec.MapCodec;
import org.bukkit.block.BlockFace;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class SideConfig {

    public static final List<BlockFace> FACES = List.of(BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN);

    private static final MapCodec<Map<BlockFace, SideMode>> CODEC =
            Codec.unboundedMap(Codec.enumOf(BlockFace.class), Codec.enumOf(SideMode.class)).optionalFieldOf("sides", Map.of());

    private final Map<BlockFace, SideMode> modes = new EnumMap<>(BlockFace.class);

    public SideMode get(BlockFace face) {
        return modes.getOrDefault(face, SideMode.NONE);
    }

    public void set(BlockFace face, SideMode mode) {
        if (mode == SideMode.NONE) modes.remove(face);
        else modes.put(face, mode);
    }

    public boolean isEmpty() {
        return modes.isEmpty();
    }

    public StateSection<Map<BlockFace, SideMode>> section() {
        return StateSection.of(CODEC, () -> modes, value -> {
            modes.clear();
            modes.putAll(value);
        });
    }
}
