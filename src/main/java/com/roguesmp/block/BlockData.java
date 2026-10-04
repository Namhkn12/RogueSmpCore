package com.roguesmp.block;

import com.roguesmp.codec.Codec;
import com.roguesmp.codec.DataResult;
import com.roguesmp.codec.DynamicOps;
import com.roguesmp.registry.Registries;

/**
 * Hold info about a block: its type, properties, and any extra "data"
 */
public record BlockData(BlockType<?, ?> type, BlockProperties properties, Object data) {

    public static final Codec<BlockData> CODEC = new Codec<>() {
        @Override
        public <O> DataResult<O> encode(BlockData input, DynamicOps<O> ops) {
            DataResult<O> encoded = input.type().dataCodec().encode(input, ops);
            if (!encoded.isSuccess()) return encoded;
            return DataResult.success(ops.setMapEntry(encoded.result(), "type", ops.createString(input.type().key())));
        }

        @Override
        public <O> DataResult<BlockData> decode(O input, DynamicOps<O> ops) {
            String key = BlockTypes.DEFAULT_TYPE;
            DataResult<O> typeField = ops.getMapField(input, "type");
            if (typeField.isSuccess()) {
                DataResult<String> typeName = ops.getString(typeField.result());
                if (!typeName.isSuccess()) return DataResult.error("Field 'type': " + typeName.error());
                key = typeName.result();
            }

            BlockType<?, ?> type = Registries.BLOCK_TYPE.get(key);
            if (type == null) {
                return DataResult.error("Unknown block type '" + key + "' (available: " + Registries.BLOCK_TYPE.getAll().keySet() + ")");
            }
            return type.dataCodec().decode(input, ops);
        }
    };
}
