package com.roguesmp.block;

import com.roguesmp.codec.Codec;
import com.roguesmp.item.ItemType;
import com.roguesmp.registry.Holder;
import com.roguesmp.registry.Registries;
import com.roguesmp.registry.Registry;
import net.kyori.adventure.key.Key;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Tunable data of a custom block, loaded from {@code blocks/<id>.json} into
 * {@code Registries.BLOCK_PROPERTIES}. Read it through {@link BlockType}, which falls back to
 * {@link #DEFAULT} for an id with no file.
 * <p>
 *     Sound is also played client-side, so it's best to use a vanilla item that is not a placeable block to prevent double sound.
 * </p>
 */
public record BlockProperties(
        int hardness,
        Set<Holder<ItemType>> tools,
        int breakStrength,
        Key model,
        List<BlockDrop> drops,
        BlockExperience experience,
        Key placeSound,
        Key breakSound
) {

    public static final BlockProperties DEFAULT = new BlockProperties(
            2,
            Set.of(),
            0,
            Key.key("stone"),
            List.of(),
            BlockExperience.NONE,
            Key.key("block.stone.place"),
            Key.key("block.stone.break")
    );

    private static final Codec<Set<Holder<ItemType>>> TOOLS_CODEC = Codec.listOf(Registry.referenceCodec(() -> Registries.ITEM_TYPE))
            .xmap(HashSet::new, ArrayList::new);

    public static final Codec<BlockProperties> CODEC = Codec.composite(
            Codec.INT.optionalFieldOf("hardness", DEFAULT.hardness()).forGetter(BlockProperties::hardness),
            TOOLS_CODEC.optionalFieldOf("tools", DEFAULT.tools()).forGetter(BlockProperties::tools),
            Codec.INT.optionalFieldOf("break_strength", DEFAULT.breakStrength()).forGetter(BlockProperties::breakStrength),
            Codec.KEY.optionalFieldOf("model", DEFAULT.model()).forGetter(BlockProperties::model),
            Codec.listOf(BlockDrop.CODEC).optionalFieldOf("drops", DEFAULT.drops()).forGetter(BlockProperties::drops),
            BlockExperience.CODEC.optionalFieldOf("experience", DEFAULT.experience()).forGetter(BlockProperties::experience),
            Codec.KEY.optionalFieldOf("place_sound", DEFAULT.placeSound()).forGetter(BlockProperties::placeSound),
            Codec.KEY.optionalFieldOf("break_sound", DEFAULT.breakSound()).forGetter(BlockProperties::breakSound),
            BlockProperties::new
    );
}
