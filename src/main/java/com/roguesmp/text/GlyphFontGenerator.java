package com.roguesmp.text;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.roguesmp.registry.Registry;
import net.kyori.adventure.key.Key;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class GlyphFontGenerator {

    public record Result(List<File> files, List<String> warnings) { }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final Registry<Glyph> glyphs;

    public GlyphFontGenerator(Registry<Glyph> glyphs) {
        this.glyphs = glyphs;
    }

    /**
     * Writes one {@code assets/<namespace>/font/<value>.json} per font used by the registered glyphs.
     *
     * @throws IllegalStateException if two glyphs of one font share a code point, or a texture is malformed
     */
    public Result generate(File outputRoot) throws IOException {
        Map<Key, Map<String, Glyph>> byFont = groupByFont();
        List<File> files = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        for (Map.Entry<Key, Map<String, Glyph>> font : byFont.entrySet()) {
            validate(font.getKey(), font.getValue(), warnings);

            File file = new File(outputRoot, "assets/" + font.getKey().namespace() + "/font/" + font.getKey().value() + ".json");
            Files.createDirectories(file.getParentFile().toPath());
            Files.writeString(file.toPath(), escapeNonAscii(GSON.toJson(fontJson(font.getValue()))), StandardCharsets.UTF_8);
            files.add(file);
        }

        return new Result(files, warnings);
    }

    private Map<Key, Map<String, Glyph>> groupByFont() {
        Map<Key, Map<String, Glyph>> byFont = new TreeMap<>(Comparator.comparing(Key::asString));

        glyphs.getAll().entrySet().stream()
                .sorted(Comparator.comparingInt((Map.Entry<String, Glyph> entry) -> entry.getValue().codePoint()))
                .forEach(entry -> byFont
                        .computeIfAbsent(entry.getValue().font(), key -> new LinkedHashMap<>())
                        .put(entry.getKey(), entry.getValue()));

        return byFont;
    }

    private static void validate(Key font, Map<String, Glyph> glyphs, List<String> warnings) {
        Map<Integer, String> usedCodePoints = new TreeMap<>();

        glyphs.forEach((id, glyph) -> {
            String previous = usedCodePoints.put(glyph.codePoint(), id);
            if (previous != null) {
                throw new IllegalStateException("Glyphs '" + previous + "' and '" + id + "' both use U+"
                        + Integer.toHexString(glyph.codePoint()).toUpperCase() + " in font " + font.asString());
            }

            if (!glyph.texture().contains(":") || !glyph.texture().endsWith(".png")) {
                throw new IllegalStateException("Glyph '" + id + "' texture '" + glyph.texture()
                        + "' must look like namespace:path/image.png");
            }

            if (glyph.ascent() > glyph.height()) {
                warnings.add("Glyph '" + id + "' has ascent " + glyph.ascent() + " higher than height " + glyph.height()
                        + ", Minecraft rejects that for bitmap providers");
            }
        });
    }

    private static JsonObject fontJson(Map<String, Glyph> glyphs) {
        JsonArray providers = new JsonArray();

        for (Glyph glyph : glyphs.values()) {
            JsonArray chars = new JsonArray();
            chars.add(new String(Character.toChars(glyph.codePoint())));

            JsonObject provider = new JsonObject();
            provider.addProperty("type", "bitmap");
            provider.addProperty("file", glyph.texture());
            provider.addProperty("ascent", glyph.ascent());
            provider.addProperty("height", glyph.height());
            provider.add("chars", chars);
            providers.add(provider);
        }

        JsonObject root = new JsonObject();
        root.add("providers", providers);
        return root;
    }

    private static String escapeNonAscii(String json) {
        StringBuilder escaped = new StringBuilder(json.length());

        for (char c : json.toCharArray()) {
            if (c > 0x7E) {
                escaped.append(String.format("\\u%04X", (int) c));
            } else {
                escaped.append(c);
            }
        }

        return escaped.toString();
    }
}
