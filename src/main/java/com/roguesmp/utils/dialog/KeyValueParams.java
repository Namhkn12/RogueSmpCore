package com.roguesmp.utils.dialog;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.Set;
import java.util.StringJoiner;

public final class KeyValueParams {

    private KeyValueParams() {
    }

    public static JsonObject parse(@Nullable String line) {
        JsonObject params = new JsonObject();
        for (String token : (line == null ? "" : line.trim()).split("\\s+")) {
            if (token.isEmpty()) continue;
            int separator = token.indexOf('=');
            if (separator <= 0) throw new IllegalArgumentException("Tham số '" + token + "' phải có dạng key=value.");
            params.add(token.substring(0, separator), parseValue(token.substring(separator + 1)));
        }
        return params;
    }

    public static String format(JsonElement element, Set<String> skippedKeys) {
        if (!element.isJsonObject()) return "";

        StringJoiner line = new StringJoiner(" ");
        element.getAsJsonObject().entrySet().stream()
                .filter(entry -> !skippedKeys.contains(entry.getKey()) && entry.getValue().isJsonPrimitive())
                .forEach(entry -> line.add(entry.getKey() + "=" + entry.getValue().getAsString()));
        return line.toString();
    }

    public static JsonPrimitive parseValue(String raw) {
        String value = unquote(raw);
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) return new JsonPrimitive(Boolean.parseBoolean(value));
        try {
            return new JsonPrimitive(new BigDecimal(value));
        } catch (NumberFormatException e) {
            return new JsonPrimitive(value);
        }
    }

    private static String unquote(String value) {
        boolean quoted = value.length() >= 2 && (value.startsWith("\"") && value.endsWith("\"") || value.startsWith("'") && value.endsWith("'"));
        return quoted ? value.substring(1, value.length() - 1) : value;
    }
}
