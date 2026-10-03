package com.roguesmp.block;

import com.roguesmp.block.impl.ResourceGeneratorBlock;
import com.roguesmp.block.impl.TallyBlock;
import com.roguesmp.registry.Registries;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Every custom block's {@link BlockType}. Each constant registers itself into
 * {@link Registries#BLOCK_TYPE} as it's initialized - call {@link #loadClass()} to force that to
 * happen. Use a constant directly for the concrete type, or look an id up in the registry and cast.
 * The block's tunable data lives in {@code blocks/<id>.json}, see {@link BlockProperties}.
 * <p>
 * Declare a block as {@code register("steel_block", SmpBlock::new)}.
 */
public class BlockTypes {

    public static final BlockType<SmpBlock> STEEL_BLOCK = register("steel_block", SmpBlock::new);
    public static final BlockType<TallyBlock> TALLY_BLOCK = register("tally_block", TallyBlock::new);
    public static final BlockType<ResourceGeneratorBlock> GENERATOR_BLOCK = register("generator_block", ResourceGeneratorBlock::new);
    public static final BlockType<ResourceGeneratorBlock> STEEL_GENERATOR = register("steel_generator", ResourceGeneratorBlock::new);

    public static void loadClass() {

    }

    public static void registerDefaults() {
        for (String s : Registries.BLOCK_PROPERTIES.getAll().keySet()) {
            if (Registries.BLOCK_TYPE.get(s) != null) continue;
            Registries.BLOCK_TYPE.register(s, new BlockType<>(s, SmpBlock::new));
        }
    }

    public static @Nullable SmpBlock create(String id) {
        BlockType<? extends SmpBlock> type = Registries.BLOCK_TYPE.get(id);
        return type == null ? null : type.create();
    }

    private static <T extends SmpBlock> BlockType<T> register(String id, Supplier<T> factory) {
        return Registries.BLOCK_TYPE.register(id, new BlockType<>(id, factory));
    }
}
