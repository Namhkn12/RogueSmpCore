package com.roguesmp.block.persistence;

import com.google.gson.JsonElement;
import com.roguesmp.codec.DataResult;
import com.roguesmp.codec.JsonOps;
import com.roguesmp.codec.MapCodec;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * One field (or group of fields) of a block's persisted state. Every section of a block writes into
 * the same flat JSON object, so each class in a block's hierarchy adds only the fields it owns:
 * <pre>
 * sections.add(StateSection.of(Codec.INT.optionalFieldOf("energy", 0), () -> energy, value -> energy = value));
 * </pre>
 * Use {@code optionalFieldOf} for anything added after rows already exist, so old rows still load.
 */
public record StateSection<S>(MapCodec<S> codec, Supplier<S> capture, Consumer<S> apply) {

    public static <S> StateSection<S> of(MapCodec<S> codec, Supplier<S> capture, Consumer<S> apply) {
        return new StateSection<>(codec, capture, apply);
    }

    public static DataResult<JsonElement> encodeAll(List<StateSection<?>> sections) {
        JsonElement state = JsonOps.INSTANCE.emptyMap();
        for (StateSection<?> section : sections) {
            DataResult<JsonElement> encoded = section.encodeInto(state);
            if (!encoded.isSuccess()) return encoded;
            state = encoded.result();
        }
        return DataResult.success(state);
    }

    /**
     * Decodes every section before applying any, so a corrupt field can never leave the block
     * half-restored.
     */
    public static DataResult<Runnable> decodeAll(List<StateSection<?>> sections, JsonElement json) {
        if (!json.isJsonObject()) return DataResult.error("Block state is not an object");

        List<Runnable> applications = new ArrayList<>();
        for (StateSection<?> section : sections) {
            DataResult<Runnable> decoded = section.decode(json);
            if (!decoded.isSuccess()) return decoded;
            applications.add(decoded.result());
        }
        return DataResult.success(() -> applications.forEach(Runnable::run));
    }

    private DataResult<JsonElement> encodeInto(JsonElement state) {
        return codec.encodeFields(capture.get(), state, JsonOps.INSTANCE);
    }

    private DataResult<Runnable> decode(JsonElement json) {
        return codec.decodeFields(json, JsonOps.INSTANCE).map(value -> () -> apply.accept(value));
    }
}
