package com.roguesmp.block;

import com.roguesmp.block.data.GeneratorData;
import com.roguesmp.block.data.NoData;
import com.roguesmp.block.impl.TallyBlock;
import com.roguesmp.block.impl.altar.AltarMainBlock;
import com.roguesmp.block.impl.altar.AltarSideBlock;
import com.roguesmp.codec.Codec;
import com.roguesmp.registry.Registries;
import com.roguesmp.block.impl.generator.ResourceGeneratorBlock;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;
import java.util.function.Function;

public class BlockTypes {

    public static final String DEFAULT_TYPE = "block";

    public static final BlockType<NoData, SmpBlock> BASIC = plain(DEFAULT_TYPE, SmpBlock::new);
    public static final BlockType<NoData, TallyBlock> TALLY = plain("tally", TallyBlock::new);
    public static final BlockType<NoData, AltarMainBlock> ALTAR_MAIN = plain("altar_main", AltarMainBlock::new);
    public static final BlockType<NoData, AltarSideBlock> ALTAR_SIDE = plain("altar_side", AltarSideBlock::new);
    public static final BlockType<GeneratorData, ResourceGeneratorBlock> GENERATOR =
            register("generator", GeneratorData.CODEC, GeneratorData.DEFAULT, ResourceGeneratorBlock::new);

    /** Create a new SmpBlock instance of {@code id} from its corresponding registered data, or null if there's no such block. */
    public static @Nullable SmpBlock create(String id) {
        BlockData data = Registries.BLOCK.get(id);
        if (data == null) return null;

        SmpBlock block = data.type().create(data);
        block.setId(id);
        return block;
    }

    private static <B extends SmpBlock> BlockType<NoData, B> plain(String key, Function<BlockProperties, B> factory) {
        return register(key, NoData.CODEC, NoData.INSTANCE, (properties, none) -> factory.apply(properties));
    }

    private static <D, B extends SmpBlock> BlockType<D, B> register(String key, Codec<D> dataCodec, D defaults, BiFunction<BlockProperties, D, B> factory) {
        BlockType<D, B> type = new BlockType<>(key, dataCodec, defaults, factory);
        if (Registries.BLOCK_TYPE.get(key) != null) throw new IllegalStateException("Duplicate block type '" + key + "'");
        Registries.BLOCK_TYPE.register(key, type);
        return type;
    }

    public static void loadClass() {}
}
