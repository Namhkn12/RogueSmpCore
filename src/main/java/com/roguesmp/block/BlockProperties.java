package com.roguesmp.block;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.roguesmp.codec.Codec;
import com.roguesmp.codec.DataResult;
import com.roguesmp.codec.DynamicOps;
import com.roguesmp.codec.JsonOps;
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
        String displayName,
        List<BlockDrop> drops,
        BlockExperience experience,
        Key placeSound,
        Key breakSound,
        JsonElement data
) {

    public static final BlockProperties DEFAULT = new BlockProperties(
            2,
            Set.of(),
            0,
            Key.key("stone"),
            "",
            List.of(),
            BlockExperience.NONE,
            Key.key("block.stone.place"),
            Key.key("block.stone.break"),
            new JsonObject()
    );

    private static final Codec<Set<Holder<ItemType>>> TOOLS_CODEC = Codec.listOf(Registry.referenceCodec(() -> Registries.ITEM_TYPE))
            .xmap(HashSet::new, ArrayList::new);

    /**
     * Passes the {@code "data"} field through untouched, for a {@link SmpBlock} subclass to decode
     * with its own codec via {@link BlockType#data}. Only works against {@link JsonOps}, the only
     * format any registry in this plugin ever uses.
     */
    private static final Codec<JsonElement> RAW_JSON_CODEC = new Codec<>() {
        @Override
        @SuppressWarnings("unchecked")
        public <O> DataResult<O> encode(JsonElement input, DynamicOps<O> ops) {
            if (ops != JsonOps.INSTANCE) return DataResult.error("Raw block data only supports JsonOps");
            return DataResult.success((O) input);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <O> DataResult<JsonElement> decode(O input, DynamicOps<O> ops) {
            if (ops != JsonOps.INSTANCE) return DataResult.error("Raw block data only supports JsonOps");
            return DataResult.success((JsonElement) input);
        }
    };

    public static final Codec<BlockProperties> CODEC = Codec.composite(
            Codec.INT.optionalFieldOf("hardness", DEFAULT.hardness()).forGetter(BlockProperties::hardness),
            TOOLS_CODEC.optionalFieldOf("tools", DEFAULT.tools()).forGetter(BlockProperties::tools),
            Codec.INT.optionalFieldOf("break_strength", DEFAULT.breakStrength()).forGetter(BlockProperties::breakStrength),
            Codec.KEY.optionalFieldOf("model", DEFAULT.model()).forGetter(BlockProperties::model),
            Codec.STRING.optionalFieldOf("display_name", DEFAULT.displayName()).forGetter(BlockProperties::displayName),
            Codec.listOf(BlockDrop.CODEC).optionalFieldOf("drops", DEFAULT.drops()).forGetter(BlockProperties::drops),
            BlockExperience.CODEC.optionalFieldOf("experience", DEFAULT.experience()).forGetter(BlockProperties::experience),
            Codec.KEY.optionalFieldOf("place_sound", DEFAULT.placeSound()).forGetter(BlockProperties::placeSound),
            Codec.KEY.optionalFieldOf("break_sound", DEFAULT.breakSound()).forGetter(BlockProperties::breakSound),
            RAW_JSON_CODEC.optionalFieldOf("data", DEFAULT.data()).forGetter(BlockProperties::data),
            BlockProperties::new
    );
}
