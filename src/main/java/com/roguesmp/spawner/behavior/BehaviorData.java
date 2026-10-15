package com.roguesmp.spawner.behavior;

import com.roguesmp.codec.Codec;
import com.roguesmp.codec.DataResult;
import com.roguesmp.codec.DynamicOps;

import java.util.Collections;
import java.util.Map;

public class BehaviorData {

    private static final Codec<Object> PARAM_VALUE = new Codec<>() {
        @Override
        public <O> DataResult<O> encode(Object input, DynamicOps<O> ops) {
            if (input instanceof Boolean bool) return DataResult.success(ops.createBoolean(bool));
            if (input instanceof Number number) return DataResult.success(ops.createDouble(number.doubleValue()));
            return DataResult.success(ops.createString(String.valueOf(input)));
        }

        @Override
        public <O> DataResult<Object> decode(O input, DynamicOps<O> ops) {
            DataResult<Boolean> bool = ops.getBoolean(input);
            if (bool.isSuccess()) return DataResult.success(bool.result());
            DataResult<Double> number = ops.getDouble(input);
            if (number.isSuccess()) return DataResult.success(number.result());
            return ops.getString(input).map(string -> string);
        }
    };

    public static final Codec<BehaviorData> CODEC = Codec.composite(
            Codec.STRING.fieldOf("type").forGetter(BehaviorData::getType),
            Codec.unboundedMap(PARAM_VALUE).optionalFieldOf("params", Map.of()).forGetter(BehaviorData::getParams),
            BehaviorData::new
    );

    private String type;
    private Map<String, Object> params;

    public BehaviorData() {}

    public BehaviorData(String type, Map<String, Object> params) {
        this.type   = type;
        this.params = params;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Map<String, Object> getParams() { return params != null ? params : Collections.emptyMap(); }
    public void setParams(Map<String, Object> params) { this.params = params; }

    // ── Type-safe helpers ────────────────────────────────────────────────────

    public int getInt(String key, int def) {
        Object v = getParams().get(key);
        return v instanceof Number n ? n.intValue() : def;
    }

    public double getDouble(String key, double def) {
        Object v = getParams().get(key);
        return v instanceof Number n ? n.doubleValue() : def;
    }

    public String getString(String key, String def) {
        Object v = getParams().get(key);
        return v != null ? v.toString() : def;
    }

    public boolean getBoolean(String key, boolean def) {
        Object v = getParams().get(key);
        return v instanceof Boolean b ? b : def;
    }
}