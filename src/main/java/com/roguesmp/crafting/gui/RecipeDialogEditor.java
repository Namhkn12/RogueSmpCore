package com.roguesmp.crafting.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.roguesmp.RogueSmpCore;
import com.roguesmp.codec.DataResult;
import com.roguesmp.codec.JsonOps;
import com.roguesmp.crafting.CraftingManager;
import com.roguesmp.crafting.recipe.CraftingRecipe;
import com.roguesmp.crafting.recipe.CraftingRecipes;
import com.roguesmp.crafting.recipe.RecipeKey;
import com.roguesmp.crafting.recipe.CokeOvenRecipe;
import com.roguesmp.crafting.recipe.FusionRecipe;
import com.roguesmp.crafting.recipe.ScrappingRecipe;
import com.roguesmp.crafting.recipe.ShapedCraftingRecipe;
import com.roguesmp.crafting.recipe.ShapelessCraftingRecipe;
import com.roguesmp.gui.crafting.editor.ShapedShapelessRecipeEditorGui;
import com.roguesmp.registry.Registries;
import com.roguesmp.utils.Utils;
import com.roguesmp.utils.dialog.DialogBuilder;
import com.roguesmp.utils.dialog.DialogTypeBuilder;
import io.papermc.paper.dialog.Dialog;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RecipeDialogEditor {

    private record Field(String name, String description) {}

    private static final String ID_INPUT = "recipe_id";
    private static final String FIELD_INPUT_PREFIX = "recipe_field_";
    private static final String RESULTS = "results";
    private static final Pattern FIELD_ERROR = Pattern.compile("Field '(\\w+)'");

    private static final Field RESULTS_FIELD = new Field(RESULTS, "Thành phẩm, vd: minecraft:coal*2, smp:slag");
    private static final Map<String, List<Field>> FIELDS = Map.of(
            FusionRecipe.TYPE_KEY, List.of(
                    new Field("input", "Vật phẩm chính đặt ở giữa, vd: minecraft:diamond*2"),
                    new Field("ingredients", "Nguyên liệu xung quanh, vd: minecraft:gold_ingot*4, minecraft:emerald")),
            CokeOvenRecipe.TYPE_KEY, List.of(
                    new Field("ingredients", "Nguyên liệu, vd: minecraft:oak_log*4"),
                    new Field("duration", "Thời gian luyện (tick, 20 tick = 1 giây)")),
            ScrappingRecipe.TYPE_KEY, List.of(
                    new Field("input", "Vật phẩm bị phân rã, vd: smp:old_sword"))
    );

    private final Player player;
    private final String typeId;
    private final @Nullable String originalId;
    private final Runnable onExit;
    private String id;
    private final Map<String, String> values = new LinkedHashMap<>();

    private RecipeDialogEditor(Player player, String typeId, @Nullable String originalId, Runnable onExit) {
        this.player = player;
        this.typeId = typeId;
        this.originalId = originalId;
        this.onExit = onExit;
        this.id = originalId == null ? "" : originalId;
        for (Field field : fieldsOf(typeId)) values.put(field.name(), "");
    }

    public static boolean usesGridEditor(CraftingRecipe recipe) {
        return recipe instanceof ShapedCraftingRecipe || recipe instanceof ShapelessCraftingRecipe;
    }

    public static void openExisting(Player player, CraftingRecipe recipe, Runnable onExit) {
        RecipeDialogEditor editor = new RecipeDialogEditor(player, recipe.getTypeId(), recipe.getId(), onExit);
        editor.prefill(recipe);
        editor.show(null);
    }

    public static void openTypePicker(Player player, Runnable onExit) {
        DialogTypeBuilder.MultiAction multi = DialogBuilder.create(Component.text("Chọn loại công thức"))
                .canCloseWithEscape(false)
                .multiAction();

        multi.addButton(Component.text(ShapedCraftingRecipe.TYPE_NAME + " / " + ShapelessCraftingRecipe.TYPE_NAME, NamedTextColor.GOLD), Component.text("Trình chỉnh sửa lưới 3x3", NamedTextColor.GRAY),
                (response, audience) -> Utils.runLater(() -> new ShapedShapelessRecipeEditorGui(player).showInventory(player)));
        for (RecipeKey<?> key : CraftingRecipes.all()) {
            if (key == CraftingRecipes.SHAPED || key == CraftingRecipes.SHAPELESS) continue;
            multi.addButton(Component.text(key.displayName(), NamedTextColor.AQUA), null,
                    (response, audience) -> Utils.runLater(() -> new RecipeDialogEditor(player, key.id(), null, onExit).show(null)));
        }
        multi.columns(2);
        multi.exitButton(Component.text("Huỷ"), (response, audience) -> Utils.runLater(onExit));

        player.closeInventory();
        player.showDialog(multi.build());
    }

    private void show(@Nullable String error) {
        player.closeInventory();
        player.showDialog(buildDialog(error));
    }

    private Dialog buildDialog(@Nullable String error) {
        DialogBuilder builder = DialogBuilder.create(Component.text("Công thức: " + typeName()))
                .canCloseWithEscape(false);
        if (error != null) builder.addTextBody(Component.text(error, NamedTextColor.RED));
        builder.addTextBody(Component.text("Vật phẩm: <id>*<số lượng>, nhiều vật phẩm cách nhau bởi dấu phẩy.", NamedTextColor.DARK_GRAY));
        for (Field field : fieldsOf(typeId)) {
            builder.addTextBody(Component.text(field.name(), NamedTextColor.YELLOW)
                    .append(Component.text(": " + field.description(), NamedTextColor.GRAY)));
        }

        builder.addTextInput(ID_INPUT, Component.text("id"), 300, true, id, 128, null);
        for (Map.Entry<String, String> entry : values.entrySet()) {
            builder.addTextInput(FIELD_INPUT_PREFIX + entry.getKey(), Component.text(entry.getKey()), 300, true, entry.getValue(), 1024, null);
        }

        DialogTypeBuilder.MultiAction multi = builder.multiAction();
        multi.addButton(Component.text("Lưu", NamedTextColor.GREEN), null, (response, audience) -> {
            id = sanitizeId(response.getText(ID_INPUT));
            for (String name : values.keySet()) {
                String text = response.getText(FIELD_INPUT_PREFIX + name);
                values.put(name, text == null ? "" : text);
            }
            String saveError = save();
            Utils.runLater(() -> {
                if (saveError != null) show(saveError);
                else onExit.run();
            });
        });
        if (originalId != null) {
            multi.addButton(Component.text("Xoá", NamedTextColor.RED), null, (response, audience) -> {
                Registries.CRAFTING_RECIPE.removeAndDeleteFiles(RogueSmpCore.getInstance(), originalId);
                CraftingManager.getInstance().rebuild();
                player.sendMessage(Utils.text("Đã xoá công thức '" + originalId + "'.", NamedTextColor.RED));
                Utils.runLater(onExit);
            });
        }
        multi.columns(2);
        multi.exitButton(Component.text("« Quay lại"), (response, audience) -> Utils.runLater(onExit));

        return multi.build();
    }

    private @Nullable String save() {
        if (id.isBlank()) return "Cần đặt ID công thức.";
        if (!id.equals(originalId) && Registries.CRAFTING_RECIPE.get(id) != null) return "Đã tồn tại công thức với ID '" + id + "'.";

        JsonObject json = new JsonObject();
        json.addProperty("type", typeId);
        json.addProperty("id", id);
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (entry.getValue().isBlank()) continue;
            JsonElement value = parseValue(entry.getValue());
            String key = entry.getKey().equals(RESULTS) && !value.isJsonArray() ? "result" : entry.getKey();
            json.add(key, value);
        }

        DataResult<CraftingRecipe> result = decodeWrappingLists(json);
        if (!result.isSuccess()) return "Công thức không hợp lệ: " + result.error();

        Registries.CRAFTING_RECIPE.registerAndSave(RogueSmpCore.getInstance(), id, result.result());
        if (originalId != null && !originalId.equals(id)) Registries.CRAFTING_RECIPE.removeAndDeleteFiles(RogueSmpCore.getInstance(), originalId);
        CraftingManager.getInstance().rebuild();
        player.sendMessage(Utils.text("Đã lưu công thức '" + id + "'.", NamedTextColor.GREEN));
        return null;
    }

    private static DataResult<CraftingRecipe> decodeWrappingLists(JsonObject json) {
        DataResult<CraftingRecipe> result = decode(json);
        for (int attempt = 0; attempt < json.size() && !result.isSuccess(); attempt++) {
            Matcher matcher = FIELD_ERROR.matcher(result.error());
            if (!matcher.find()) break;

            JsonElement value = json.get(matcher.group(1));
            if (value == null || value.isJsonArray()) break;

            JsonArray wrapped = new JsonArray();
            wrapped.add(value);
            json.add(matcher.group(1), wrapped);
            result = decode(json);
        }
        return result;
    }

    private static DataResult<CraftingRecipe> decode(JsonObject json) {
        try {
            return CraftingRecipe.CODEC.decode(json, JsonOps.INSTANCE);
        } catch (IllegalArgumentException e) {
            return DataResult.error(e.getMessage());
        }
    }

    private void prefill(CraftingRecipe recipe) {
        DataResult<JsonElement> encoded = CraftingRecipe.CODEC.encode(recipe, JsonOps.INSTANCE);
        if (!encoded.isSuccess() || !encoded.result().isJsonObject()) return;

        for (Map.Entry<String, JsonElement> entry : encoded.result().getAsJsonObject().entrySet()) {
            String key = entry.getKey().equals("result") ? RESULTS : entry.getKey();
            if (key.equals("type") || key.equals("id")) continue;
            values.put(key, formatValue(entry.getValue()));
        }
    }

    private String typeName() {
        RecipeKey<?> key = CraftingRecipes.byId(typeId);
        return key == null ? typeId : key.displayName();
    }

    private static List<Field> fieldsOf(String typeId) {
        List<Field> fields = new ArrayList<>();
        fields.add(RESULTS_FIELD);
        fields.addAll(FIELDS.getOrDefault(typeId, List.of()));
        return fields;
    }

    private static JsonElement parseValue(String raw) {
        String text = raw.trim();
        if (text.startsWith("[") || text.startsWith("{")) {
            try {
                return JsonParser.parseString(text);
            } catch (RuntimeException ignored) {
            }
        }

        List<String> parts = new ArrayList<>();
        for (String part : text.split(",")) {
            if (!part.isBlank()) parts.add(part.trim());
        }
        if (parts.size() == 1) return parseToken(parts.getFirst());

        JsonArray array = new JsonArray();
        for (String part : parts) array.add(parseToken(part));
        return array;
    }

    private static JsonElement parseToken(String token) {
        int star = token.lastIndexOf('*');
        if (star > 0) {
            try {
                JsonObject ingredient = new JsonObject();
                ingredient.addProperty("count", Integer.parseInt(token.substring(star + 1).trim()));
                ingredient.addProperty("item", token.substring(0, star).trim());
                return ingredient;
            } catch (NumberFormatException ignored) {
            }
        }
        if (token.equalsIgnoreCase("true") || token.equalsIgnoreCase("false")) return new JsonPrimitive(Boolean.parseBoolean(token));
        try {
            return new JsonPrimitive(new BigDecimal(token));
        } catch (NumberFormatException e) {
            return new JsonPrimitive(token);
        }
    }

    private static String formatValue(JsonElement value) {
        if (!value.isJsonArray()) return formatToken(value);

        StringJoiner joiner = new StringJoiner(", ");
        for (JsonElement element : value.getAsJsonArray()) joiner.add(formatToken(element));
        return joiner.toString();
    }

    private static String formatToken(JsonElement value) {
        if (value.isJsonPrimitive()) return value.getAsString();
        if (value.isJsonObject() && value.getAsJsonObject().has("item")) {
            JsonObject ingredient = value.getAsJsonObject();
            int count = ingredient.has("count") ? ingredient.get("count").getAsInt() : 1;
            return ingredient.get("item").getAsString() + (count == 1 ? "" : "*" + count);
        }
        return value.toString();
    }

    private static String sanitizeId(@Nullable String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", "_");
    }
}
