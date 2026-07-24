package com.roguesmp.codec;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DynamicOps implementation for Gson's JsonElement hierarchy.
 */
public enum JsonOps implements DynamicOps<JsonElement> {
    INSTANCE;

    @Override
    public JsonElement emptyMap() { return new JsonObject(); }

    @Override
    public JsonElement createString(String value) { return new JsonPrimitive(value); }

    @Override
    public JsonElement createBoolean(boolean value) { return new JsonPrimitive(value); }

    @Override
    public JsonElement createByte(byte value) { return new JsonPrimitive(value); }

    @Override
    public JsonElement createShort(short value) { return new JsonPrimitive(value); }

    @Override
    public JsonElement createInt(int value) { return new JsonPrimitive(value); }

    @Override
    public JsonElement createLong(long value) { return new JsonPrimitive(value); }

    @Override
    public JsonElement createFloat(float value) { return new JsonPrimitive(value); }

    @Override
    public JsonElement createDouble(double value) { return new JsonPrimitive(value); }

    @Override
    public JsonElement createList(List<JsonElement> elements) {
        JsonArray array = new JsonArray();
        elements.forEach(array::add);
        return array;
    }

    @Override
    public DataResult<String> getString(JsonElement input) {
        return (input != null && input.isJsonPrimitive() && input.getAsJsonPrimitive().isString())
                ? DataResult.success(input.getAsString())
                : DataResult.error("Not a string: " + input);
    }

    @Override
    public DataResult<Boolean> getBoolean(JsonElement input) {
        return (input != null && input.isJsonPrimitive() && input.getAsJsonPrimitive().isBoolean())
                ? DataResult.success(input.getAsBoolean())
                : DataResult.error("Not a boolean: " + input);
    }

    @Override
    public DataResult<Byte> getByte(JsonElement input) {
        return (input != null && input.isJsonPrimitive() && input.getAsJsonPrimitive().isNumber())
                ? DataResult.success(input.getAsByte())
                : DataResult.error("Not a byte: " + input);
    }

    @Override
    public DataResult<Short> getShort(JsonElement input) {
        return (input != null && input.isJsonPrimitive() && input.getAsJsonPrimitive().isNumber())
                ? DataResult.success(input.getAsShort())
                : DataResult.error("Not a short: " + input);
    }

    @Override
    public DataResult<Integer> getInt(JsonElement input) {
        return (input != null && input.isJsonPrimitive() && input.getAsJsonPrimitive().isNumber())
                ? DataResult.success(input.getAsInt())
                : DataResult.error("Not an integer: " + input);
    }

    @Override
    public DataResult<Long> getLong(JsonElement input) {
        return (input != null && input.isJsonPrimitive() && input.getAsJsonPrimitive().isNumber())
                ? DataResult.success(input.getAsLong())
                : DataResult.error("Not a long: " + input);
    }

    @Override
    public DataResult<Float> getFloat(JsonElement input) {
        return (input != null && input.isJsonPrimitive() && input.getAsJsonPrimitive().isNumber())
                ? DataResult.success(input.getAsFloat())
                : DataResult.error("Not a float: " + input);
    }

    @Override
    public DataResult<Double> getDouble(JsonElement input) {
        return (input != null && input.isJsonPrimitive() && input.getAsJsonPrimitive().isNumber())
                ? DataResult.success(input.getAsDouble())
                : DataResult.error("Not a double: " + input);
    }

    @Override
    public DataResult<List<JsonElement>> getList(JsonElement input) {
        if (input != null && input.isJsonArray()) {
            List<JsonElement> list = new ArrayList<>();
            input.getAsJsonArray().forEach(list::add);
            return DataResult.success(list);
        }
        return DataResult.error("Not a list: " + input);
    }

    @Override
    public DataResult<Map<String, JsonElement>> getMap(JsonElement input) {
        if (input != null && input.isJsonObject()) {
            Map<String, JsonElement> map = new HashMap<>();
            for (Map.Entry<String, JsonElement> entry : input.getAsJsonObject().entrySet()) {
                map.put(entry.getKey(), entry.getValue());
            }
            return DataResult.success(map);
        }
        return DataResult.error("Not a map object: " + input);
    }

    @Override
    public DataResult<JsonElement> getMapField(JsonElement mapInput, String key) {
        if (mapInput != null && mapInput.isJsonObject()) {
            JsonElement elem = mapInput.getAsJsonObject().get(key);
            return elem != null ? DataResult.success(elem) : DataResult.error("Field missing: " + key);
        }
        return DataResult.error("Not a map object: " + mapInput);
    }

    @Override
    public JsonElement setMapEntry(JsonElement mapInput, String key, JsonElement value) {
        JsonObject obj = mapInput != null && mapInput.isJsonObject() ? mapInput.getAsJsonObject() : new JsonObject();
        obj.add(key, value);
        return obj;
    }
}
