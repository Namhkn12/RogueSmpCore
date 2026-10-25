package com.roguesmp.block;

import com.roguesmp.block.data.*;
import com.roguesmp.block.impl.brewing.BrewingBlock;
import com.roguesmp.block.impl.energy.WirelessNodeBlock;
import com.roguesmp.block.impl.TallyBlock;
import com.roguesmp.block.impl.cokeoven.CokeOvenBlock;
import com.roguesmp.block.impl.energy.EnergyGeneratorBlock;
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

    public static final BlockType<NoData, SmpBlock> BASIC = plain(DEFAULT_TYPE, "Khối", SmpBlock::new);
    public static final BlockType<NoData, TallyBlock> TALLY = plain("tally", "Bộ đếm", TallyBlock::new);
    public static final BlockType<NoData, AltarMainBlock> ALTAR_MAIN = plain("altar_main", "Bàn thờ chính", AltarMainBlock::new);
    public static final BlockType<NoData, AltarSideBlock> ALTAR_SIDE = plain("altar_side", "Bàn thờ phụ", AltarSideBlock::new);
    public static final BlockType<CokeOvenData, CokeOvenBlock> COKE_OVEN =
            register("coke_oven", "Lò cốc", CokeOvenData.CODEC, CokeOvenData.DEFAULT, CokeOvenBlock::new);
    public static final BlockType<GeneratorData, ResourceGeneratorBlock> GENERATOR =
            register("generator", "Máy tạo tài nguyên", GeneratorData.CODEC, GeneratorData.DEFAULT, ResourceGeneratorBlock::new);
    public static final BlockType<EnergyGeneratorData, EnergyGeneratorBlock> ENERGY_GENERATOR =
            register("energy_generator", "Máy phát điện", EnergyGeneratorData.CODEC, EnergyGeneratorData.DEFAULT, EnergyGeneratorBlock::new);
    public static final BlockType<WirelessNodeData, WirelessNodeBlock> WIRELESS_NODE =
            register("wireless_node", "Nút không dây", WirelessNodeData.CODEC, WirelessNodeData.DEFAULT, WirelessNodeBlock::new);
    public static final BlockType<BrewingData, BrewingBlock> BREWING =
            register("brewing", "Bàn pha chế", BrewingData.CODEC, BrewingData.DEFAULT, BrewingBlock::new);

    /** Create a new SmpBlock instance of {@code id} from its corresponding registered data, or null if there's no such block. */
    public static @Nullable SmpBlock create(String id) {
        BlockData data = Registries.BLOCK.get(id);
        if (data == null) return null;

        SmpBlock block = data.type().create(data);
        block.setId(id);
        block.setType(data.type());
        return block;
    }

    private static <B extends SmpBlock> BlockType<NoData, B> plain(String key, String displayName, Function<BlockProperties, B> factory) {
        return register(key, displayName, NoData.CODEC, NoData.INSTANCE, (properties, none) -> factory.apply(properties));
    }

    private static <D, B extends SmpBlock> BlockType<D, B> register(String key, String displayName, Codec<D> dataCodec, D defaults, BiFunction<BlockProperties, D, B> factory) {
        BlockType<D, B> type = new BlockType<>(key, displayName, dataCodec, defaults, factory);
        if (Registries.BLOCK_TYPE.get(key) != null) throw new IllegalStateException("Duplicate block type '" + key + "'");
        Registries.BLOCK_TYPE.register(key, type);
        return type;
    }

    public static void loadClass() {}
}
