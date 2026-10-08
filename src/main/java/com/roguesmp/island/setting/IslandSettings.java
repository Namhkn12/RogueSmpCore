package com.roguesmp.island.setting;

import com.roguesmp.codec.Codec;
import com.roguesmp.codec.DataResult;
import com.roguesmp.codec.DynamicOps;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class IslandSettings {
    public static final Setting<Boolean> ALLOW_GUEST = new Setting<>("allow_guest", true, Codec.BOOLEAN);

    private static final List<Setting<?>> ALL = List.of(ALLOW_GUEST);
    private static final Map<String, Setting<?>> BY_ID = new LinkedHashMap<>();

    static {
        for (Setting<?> setting : ALL) BY_ID.put(setting.id(), setting);
    }

    public static final Codec<Map<String, Object>> VALUES_CODEC = new Codec<>() {
        @Override
        public <O> DataResult<O> encode(Map<String, Object> input, DynamicOps<O> ops) {
            O encoded = ops.emptyMap();
            for (Map.Entry<String, Object> entry : input.entrySet()) {
                DataResult<O> value = encodeValue(BY_ID.get(entry.getKey()), entry.getValue(), ops);
                if (!value.isSuccess()) return DataResult.error("['" + entry.getKey() + "']: " + value.error());
                encoded = ops.setMapEntry(encoded, entry.getKey(), value.result());
            }
            return DataResult.success(encoded);
        }

        @Override
        public <O> DataResult<Map<String, Object>> decode(O input, DynamicOps<O> ops) {
            DataResult<Map<String, O>> raw = ops.getMap(input);
            if (!raw.isSuccess()) return DataResult.error(raw.error());

            Map<String, Object> values = new LinkedHashMap<>();
            for (Map.Entry<String, O> entry : raw.result().entrySet()) {
                Setting<?> setting = BY_ID.get(entry.getKey());
                if (setting == null) continue;

                DataResult<?> decoded = setting.codec().decode(entry.getValue(), ops);
                if (decoded.isSuccess()) values.put(entry.getKey(), decoded.result());
            }
            return DataResult.success(values);
        }

        @SuppressWarnings("unchecked")
        private <O, T> DataResult<O> encodeValue(Setting<T> setting, Object value, DynamicOps<O> ops) {
            if (setting == null) return DataResult.error("unknown setting");
            return setting.codec().encode((T) value, ops);
        }
    };
}
