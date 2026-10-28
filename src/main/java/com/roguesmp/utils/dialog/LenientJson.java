package com.roguesmp.utils.dialog;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.roguesmp.codec.Codec;
import com.roguesmp.codec.DataResult;
import com.roguesmp.codec.JsonOps;
import io.papermc.paper.registry.data.dialog.input.TextDialogInput;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeSet;
import java.util.regex.Pattern;

public final class LenientJson {

    public static final String HELP = "Gson lenient format: có thể không cần {} ngoài cùng, key và text đơn giản không cần ngoặc kép. "
            + "Value có dấu cách hoặc ':' cần đặt trong \"...\" hoặc '...'. Vd: count: 2, item: 'minecraft:stone', item: stone";

    private static final Pattern BARE_WORD = Pattern.compile("[A-Za-z_][A-Za-z0-9_.\\-]*");

    private LenientJson() {
    }

    public static JsonElement parse(@Nullable String text) {
        String trimmed = text == null ? "" : text.trim();
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) trimmed = "{" + trimmed + "}";

        try (JsonReader reader = new JsonReader(new StringReader(trimmed))) {
            reader.setStrictness(Strictness.LENIENT);
            JsonElement element = JsonParser.parseReader(reader);
            if (reader.peek() != JsonToken.END_DOCUMENT) throw new IllegalArgumentException("Có ký tự thừa sau JSON.");
            return element;
        } catch (JsonParseException | IOException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new IllegalArgumentException("JSON không hợp lệ: " + cause.getMessage(), e);
        }
    }

    public static <T> DataResult<T> decode(Codec<T> codec, @Nullable String text) {
        JsonElement json;
        try {
            json = parse(text);
        } catch (IllegalArgumentException e) {
            return DataResult.error(e.getMessage());
        }
        return decode(codec, json);
    }

    public static <T> DataResult<T> decode(Codec<T> codec, JsonElement json) {
        try {
            return codec.decode(json, JsonOps.INSTANCE);
        } catch (RuntimeException e) {
            return DataResult.error(String.valueOf(e.getMessage()));
        }
    }

    public static <T> String format(Codec<T> codec, T value) {
        DataResult<JsonElement> encoded = codec.encode(value, JsonOps.INSTANCE);
        return encoded.isSuccess() ? format(encoded.result()) : "";
    }

    public static String format(JsonElement element) {
        if (element.isJsonObject()) return formatMembers(element.getAsJsonObject());
        return formatValue(element);
    }

    public static Component typesHelp(Collection<String> types) {
        return Component.text("type: ", NamedTextColor.YELLOW)
                .append(Component.text(String.join(", ", new TreeSet<>(types)), NamedTextColor.GRAY));
    }

    public static TextDialogInput.MultilineOptions multiline() {
        return TextDialogInput.MultilineOptions.create(null, 140);
    }

    public static void addInput(DialogBuilder builder, String id, Component label, String initial) {
        builder.addTextInput(id, label, 300, true, initial, Integer.MAX_VALUE, multiline());
    }

    private static String formatMembers(JsonObject object) {
        StringJoiner joiner = new StringJoiner(", ");
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            joiner.add(formatKey(entry.getKey()) + ": " + formatValue(entry.getValue()));
        }
        return joiner.toString();
    }

    private static String formatValue(JsonElement value) {
        if (value.isJsonObject()) return "{" + formatMembers(value.getAsJsonObject()) + "}";
        if (value.isJsonArray()) {
            StringJoiner joiner = new StringJoiner(", ", "[", "]");
            for (JsonElement element : (JsonArray) value) joiner.add(formatValue(element));
            return joiner.toString();
        }
        if (value.isJsonPrimitive() && ((JsonPrimitive) value).isString()) {
            String text = value.getAsString();
            return isBareWord(text) ? text : value.toString();
        }
        return value.toString();
    }

    private static String formatKey(String key) {
        return BARE_WORD.matcher(key).matches() ? key : new JsonPrimitive(key).toString();
    }

    private static boolean isBareWord(String text) {
        return BARE_WORD.matcher(text).matches() && !text.equals("true") && !text.equals("false") && !text.equals("null");
    }
}
