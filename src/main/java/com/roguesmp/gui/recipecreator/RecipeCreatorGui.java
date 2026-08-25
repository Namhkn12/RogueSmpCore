package com.roguesmp.gui.recipecreator;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.crafting.CraftingIngredient;
import com.roguesmp.crafting.recipe.CraftingRecipe;
import com.roguesmp.crafting.recipe.FusionRecipe;
import com.roguesmp.crafting.recipe.ShapedCraftingRecipe;
import com.roguesmp.crafting.recipe.ShapelessCraftingRecipe;
import com.roguesmp.crafting.CraftingManager;
import com.roguesmp.registry.Registries;
import com.roguesmp.utils.Utils;
import com.roguesmp.utils.dialog.DialogBuilder;
import com.roguesmp.utils.dialog.DialogTypeBuilder;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.StringArgument;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.TextDialogInput;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * In-game editor for {@link CraftingRecipe}s, built on the same Paper Dialog API pattern as
 * {@code gui.loottablecreator.LootTableGuiCreator} - a main {@code multiAction()} menu (id + kind
 * + result + remainders + kind-specific fields + save), where list-shaped fields (a shaped
 * recipe's {@code key} map, a shapeless/fusion recipe's {@code ingredients}, {@code remainders})
 * each hand off to their own hub dialog (one button per entry + add-entry), mirroring
 * {@code LootTableGuiCreator}'s pool/entry hub/list/add/entry shape.
 * <p>
 * Unlike a loot table, a recipe's overall kind (shaped/shapeless/fusion) is fixed for its whole
 * lifetime rather than varying per sub-entry, so creating a new recipe asks for that kind first
 * (see {@link #openKindPickerDialog}) before the main dialog - which only then knows which
 * kind-specific buttons to show - can even be built.
 */
public class RecipeCreatorGui {

    private @Nullable String id;
    private RecipeKind kind = RecipeKind.SHAPED;

    private String resultItemId = "";
    private int resultCount = 1;
    private final List<RemainderDraft> remainders = new ArrayList<>();

    // shaped-only
    private List<String> pattern = new ArrayList<>();
    private final List<KeyEntryDraft> keyEntries = new ArrayList<>();
    private boolean mirrored = true;

    // shapeless + fusion
    private final List<IngredientDraft> ingredients = new ArrayList<>();

    // fusion-only
    private String inputItemId = "";
    private int inputCount = 1;

    public static RecipeCreatorGui editExisting(String id, CraftingRecipe recipe) {
        RecipeCreatorGui gui = new RecipeCreatorGui();
        gui.id = id;
        CraftingIngredient result = recipe.getResult();
        if (result != null) {
            gui.resultItemId = result.key();
            gui.resultCount = result.count();
        }
        if (recipe instanceof ShapedCraftingRecipe shaped) {
            gui.kind = RecipeKind.SHAPED;
            gui.pattern = new ArrayList<>(shaped.getPattern());
            gui.mirrored = shaped.isMirrored();
            shaped.getKeyMap().forEach((symbol, ingredient) -> gui.keyEntries.add(new KeyEntryDraft(symbol, ingredient.key(), ingredient.count())));
            shaped.getRemainders().forEach((ingredientId, remainderId) -> gui.remainders.add(new RemainderDraft(ingredientId, remainderId)));
        } else if (recipe instanceof ShapelessCraftingRecipe shapeless) {
            gui.kind = RecipeKind.SHAPELESS;
            for (CraftingIngredient ingredient : shapeless.getIngredients()) gui.ingredients.add(new IngredientDraft(ingredient.key(), ingredient.count()));
        } else if (recipe instanceof FusionRecipe fusion) {
            gui.kind = RecipeKind.FUSION;
            gui.inputItemId = fusion.getInput().key();
            gui.inputCount = fusion.getInput().count();
            for (CraftingIngredient ingredient : fusion.getIngredients()) gui.ingredients.add(new IngredientDraft(ingredient.key(), ingredient.count()));
        }

        return gui;
    }

    // ==========================================
    // KIND PICKER (new recipe only)
    // ==========================================

    public void openKindPickerDialog(Player player) {
        DialogBuilder builder = DialogBuilder.create(Component.text("Chọn loại công thức")).canCloseWithEscape(false);

        DialogTypeBuilder.MultiAction multi = builder.multiAction();
        for (RecipeKind candidate : RecipeKind.values()) {
            multi.addButton(Component.text(candidate.label), Component.text(candidate.hint, NamedTextColor.GRAY), (response, audience) -> {
                this.kind = candidate;
                Utils.runLater(() -> openMainDialog(player));
            });
        }
        multi.columns(3);
        multi.exitButton(Component.text("Huỷ"), (s, v) -> v.closeDialog());

        player.showDialog(multi.build());
    }

    // ==========================================
    // MAIN DIALOG
    // ==========================================

    public void openMainDialog(Player player) {
        DialogBuilder builder = DialogBuilder.create(Component.text("Trình Tạo Công Thức"))
                .canCloseWithEscape(false)
                .addTextBody(Component.text("ID: " + (id == null ? "(chưa đặt)" : id)))
                .addTextBody(Component.text("Loại: " + kind.label))
                .addTextBody(Component.text(kind.hint, NamedTextColor.GRAY))
                .addItemBody(previewStack(resultItemId, resultCount))
                .addTextBody(Component.text("Kết quả: ").append(ingredientLabel(resultItemId, resultCount)));

        if (kind == RecipeKind.FUSION) {
            builder = addIngredientPreview(builder, inputItemId, inputCount, "Đầu vào (slot 0)");
        }
        switch (kind) {
            case SHAPED -> {
                for (KeyEntryDraft entry : keyEntries) builder = addIngredientPreview(builder, entry.itemId, entry.count, "Key '" + entry.symbol + "'");
            }
            case SHAPELESS, FUSION -> {
                for (IngredientDraft ingredient : ingredients) builder = addIngredientPreview(builder, ingredient.itemId, ingredient.count, "Nguyên liệu");
            }
        }

        DialogTypeBuilder.MultiAction multi = builder.multiAction();

        multi.addButton(Component.text("id", id == null ? NamedTextColor.RED : NamedTextColor.GREEN), Component.text("Đặt/đổi ID định danh của công thức (dùng làm tên khi lưu)."),
                (response, audience) -> Utils.runLater(() -> player.showDialog(buildIdDialog(player))));

        multi.addButton(Component.text("result", NamedTextColor.AQUA), ingredientLabel(resultItemId, resultCount),
                (response, audience) -> Utils.runLater(() -> player.showDialog(buildResultDialog(player))));

        switch (kind) {
            case SHAPED -> {
                multi.addButton(Component.text("pattern", NamedTextColor.YELLOW),
                        Component.text(describePattern() + " - mỗi ký tự = 1 ô trong lưới 3x3 (khoảng trắng = ô trống, xem 'keys' bên dưới)."),
                        (response, audience) -> Utils.runLater(() -> player.showDialog(buildPatternDialog(player))));
                multi.addButton(Component.text("keys (" + keyEntries.size() + ")", NamedTextColor.YELLOW),
                        Component.text("Ánh xạ ký tự trong pattern -> nguyên liệu + số lượng cần ở ô đó."),
                        (response, audience) -> Utils.runLater(() -> player.showDialog(buildKeyHubDialog(player))));
                multi.addButton(Component.text("mirrored: " + mirrored, NamedTextColor.YELLOW),
                        Component.text("Bật thì công thức cũng khớp khi lưới bị lật ngang (ảnh gương), giống crafting vanilla."),
                        (response, audience) -> {
                            mirrored = !mirrored;
                            Utils.runLater(() -> openMainDialog(player));
                        });
                multi.addButton(Component.text("remainders (" + remainders.size() + ")", NamedTextColor.AQUA),
                        Component.text("Vật phẩm còn sót lại trong ô sau khi nguyên liệu bị dùng hết (VD: bucket nước -> bucket rỗng)."),
                        (response, audience) -> Utils.runLater(() -> player.showDialog(buildRemainderHubDialog(player))));
            }
            case SHAPELESS -> multi.addButton(Component.text("ingredients (" + ingredients.size() + ")", NamedTextColor.YELLOW),
                    Component.text("Nguyên liệu cần có trong lưới - vị trí không quan trọng, nhưng 1 item lặp lại cần đúng số slot riêng."),
                    (response, audience) -> Utils.runLater(() -> player.showDialog(buildIngredientHubDialog(player))));
            case FUSION -> {
                multi.addButton(Component.text("input", NamedTextColor.YELLOW),
                        ingredientLabel(inputItemId, inputCount).append(Component.newline()).append(Component.text("Slot 0 - vật phẩm ở ô trung tâm.", NamedTextColor.GRAY)),
                        (response, audience) -> Utils.runLater(() -> player.showDialog(buildInputDialog(player))));
                multi.addButton(Component.text("ingredients (" + ingredients.size() + ")", NamedTextColor.YELLOW),
                        Component.text("Nguyên liệu ở các ô xung quanh (slot 1+) - vị trí/thứ tự không quan trọng."),
                        (response, audience) -> Utils.runLater(() -> player.showDialog(buildIngredientHubDialog(player))));
            }
        }

        multi.addButton(Component.text("save", NamedTextColor.GOLD), Component.text("Lưu công thức này vào registry (ghi đè nếu ID đã tồn tại)."),
                (response, audience) -> Utils.runLater(() -> player.showDialog(buildSaveDialog(player))));

        multi.columns(3);
        multi.exitButton(Component.text("Đóng"), (v, a) -> a.closeDialog());

        player.showDialog(multi.build());
    }

    /** {@code itemId} (or "chưa đặt"/red for a blank/unresolvable one) with its count - used as both button tooltips and body text wherever an ingredient reference needs a human-readable, validity-colored label. */
    private static Component ingredientLabel(String itemId, int count) {
        if (itemId == null || itemId.isBlank()) return Component.text("(chưa đặt)", NamedTextColor.GRAY);
        boolean resolvable = CraftingIngredient.resolveItemStack(itemId, Math.max(1, count)) != null;
        return Component.text(itemId + " x" + count, resolvable ? NamedTextColor.WHITE : NamedTextColor.RED);
    }

    /** Resolves {@code itemId} to a real preview {@link ItemStack} for a dialog's {@code addItemBody} - a labeled placeholder (paper for "unset", barrier for "doesn't resolve") when there's nothing real to show, so the preview icon itself doubles as validity feedback. */
    private static @NotNull ItemStack previewStack(@Nullable String itemId, int count) {
        if (itemId == null || itemId.isBlank()) {
            ItemStack placeholder = ItemStack.of(Material.PAPER);
            placeholder.setData(DataComponentTypes.ITEM_NAME, Component.text("(chưa đặt item)", NamedTextColor.GRAY));
            return placeholder;
        }

        ItemStack resolved = CraftingIngredient.resolveItemStack(itemId, Math.max(1, count));
        if (resolved != null) return resolved;

        ItemStack placeholder = ItemStack.of(Material.BARRIER);
        placeholder.setData(DataComponentTypes.ITEM_NAME, Component.text("Không tìm thấy: " + itemId, NamedTextColor.RED));
        return placeholder;
    }

    /**
     * Appends one labeled item-preview body (icon + inline description, e.g. "Key 'A': iron_ingot x2")
     * to {@code builder} for the main dialog's ingredient-list preview - skips entries with no item id
     * set yet (a freshly added, still-blank draft) so the list only grows as it's actually filled in.
     */
    private static DialogBuilder addIngredientPreview(DialogBuilder builder, @Nullable String itemId, int count, String prefix) {
        if (itemId == null || itemId.isBlank()) return builder;
        Component label = Component.text(prefix + ": ").append(ingredientLabel(itemId, count));
        return builder.addItemBody(previewStack(itemId, count), b -> b.description(DialogBody.plainMessage(label)));
    }

    private String describePattern() {
        return pattern.isEmpty() ? "(trống)" : String.join("|", pattern);
    }

    // ==========================================
    // ID
    // ==========================================

    private Dialog buildIdDialog(Player player) {
        return DialogBuilder.create(Component.text("Đặt ID công thức"))
                .canCloseWithEscape(false)
                .externalTitle(Component.text("id", id == null ? NamedTextColor.RED : NamedTextColor.GREEN))
                .addTextInput("value", Component.text("ID, vd fire_sword_upgrade"), b -> b.initial(id == null ? "" : id).maxLength(128))
                .confirmation()
                .yesButton(Component.text("Xác nhận"), null, (response, audience) -> {
                    String sanitized = sanitizeId(response.getText("value"));
                    if (sanitized.isEmpty()) player.sendMessage(Utils.text("ID không hợp lệ.", NamedTextColor.RED));
                    else this.id = sanitized;
                    Utils.runLater(() -> openMainDialog(player));
                })
                .noButton(Component.text("Huỷ"), null, (response, audience) -> Utils.runLater(() -> openMainDialog(player)))
                .build();
    }

    private static String sanitizeId(@Nullable String raw) {
        if (raw == null) return "";
        return raw.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", "_");
    }

    // ==========================================
    // RESULT / FUSION INPUT
    // ==========================================

    private Dialog buildResultDialog(Player player) {
        return DialogBuilder.create(Component.text("Kết quả công thức"))
                .canCloseWithEscape(false)
                .addItemBody(previewStack(resultItemId, resultCount))
                .addTextInput("item_id", Component.text("item_id (\"minecraft:...\" = vanilla, hoặc BaseItem id)"), b -> b.initial(resultItemId).maxLength(128))
                .addTextInput("count", Component.text("count"), b -> b.initial(String.valueOf(resultCount)).maxLength(8))
                .confirmation()
                .yesButton(Component.text("Xác nhận"), null, (response, audience) -> {
                    String itemId = response.getText("item_id");
                    if (itemId != null && !itemId.isBlank()) resultItemId = itemId.trim();
                    Integer count = parseInt(response.getText("count"), 1, 6400);
                    if (count != null) resultCount = count;
                    Utils.runLater(() -> openMainDialog(player));
                })
                .noButton(Component.text("Huỷ"), null, (response, audience) -> Utils.runLater(() -> openMainDialog(player)))
                .build();
    }

    private Dialog buildInputDialog(Player player) {
        return DialogBuilder.create(Component.text("Vật phẩm đầu vào (fusion)"))
                .canCloseWithEscape(false)
                .addItemBody(previewStack(inputItemId, inputCount))
                .addTextInput("item_id", Component.text("item_id"), b -> b.initial(inputItemId).maxLength(128))
                .addTextInput("count", Component.text("count"), b -> b.initial(String.valueOf(inputCount)).maxLength(8))
                .confirmation()
                .yesButton(Component.text("Xác nhận"), null, (response, audience) -> {
                    String itemId = response.getText("item_id");
                    if (itemId != null && !itemId.isBlank()) inputItemId = itemId.trim();
                    Integer count = parseInt(response.getText("count"), 1, 6400);
                    if (count != null) inputCount = count;
                    Utils.runLater(() -> openMainDialog(player));
                })
                .noButton(Component.text("Huỷ"), null, (response, audience) -> Utils.runLater(() -> openMainDialog(player)))
                .build();
    }

    // ==========================================
    // PATTERN (shaped only)
    // ==========================================

    private Dialog buildPatternDialog(Player player) {
        String initial = String.join("\n", pattern);
        return DialogBuilder.create(Component.text("Pattern (tối đa 3 dòng, mỗi dòng tối đa 3 ký tự)"))
                .canCloseWithEscape(false)
                .addTextBody(Component.text("Khoảng trắng = ô trống. VD: \"A \" / \" B\""))
                .addTextInput("value", Component.text("pattern"), b -> b.initial(initial).maxLength(30).multiline(TextDialogInput.MultilineOptions.create(3, 100)))
                .confirmation()
                .yesButton(Component.text("Xác nhận"), null, (response, audience) -> {
                    String text = response.getText("value");
                    pattern = text == null || text.isEmpty() ? new ArrayList<>() : new ArrayList<>(Arrays.asList(text.split("\n", -1)));
                    Utils.runLater(() -> openMainDialog(player));
                })
                .noButton(Component.text("Huỷ"), null, (response, audience) -> Utils.runLater(() -> openMainDialog(player)))
                .build();
    }

    // ==========================================
    // KEYS (shaped only)
    // ==========================================

    private Dialog buildKeyHubDialog(Player player) {
        DialogBuilder builder = DialogBuilder.create(Component.text("Danh sách key"))
                .canCloseWithEscape(false)
                .externalTitle(Component.text("keys (" + keyEntries.size() + ")", NamedTextColor.YELLOW));

        DialogTypeBuilder.MultiAction multi = builder.multiAction();
        for (int i = 0; i < keyEntries.size(); i++) {
            int index = i;
            KeyEntryDraft entry = keyEntries.get(i);
            Component label = Component.text("'" + entry.symbol + "' -> ").append(ingredientLabel(entry.itemId, entry.count));
            multi.addButton(label, label, (response, audience) -> Utils.runLater(() -> player.showDialog(buildKeyEntryDialog(player, index))));
        }
        multi.addButton(Component.text("+ Thêm key", NamedTextColor.GREEN), Component.text("Thêm 1 ánh xạ ký tự -> nguyên liệu mới."), (response, audience) -> {
            keyEntries.add(new KeyEntryDraft("", "", 1));
            int newIndex = keyEntries.size() - 1;
            Utils.runLater(() -> player.showDialog(buildKeyEntryDialog(player, newIndex)));
        });

        multi.columns(3);
        multi.exitButton(Component.text("« Quay lại"), (response, audience) -> Utils.runLater(() -> openMainDialog(player)));
        return multi.build();
    }

    private Dialog buildKeyEntryDialog(Player player, int index) {
        KeyEntryDraft entry = keyEntries.get(index);

        DialogBuilder builder = DialogBuilder.create(Component.text("Key entry"))
                .canCloseWithEscape(false)
                .addItemBody(previewStack(entry.itemId, entry.count))
                .addTextInput("symbol", Component.text("ký tự trong pattern (1 ký tự)"), b -> b.initial(entry.symbol).maxLength(1))
                .addTextInput("item_id", Component.text("item_id"), b -> b.initial(entry.itemId).maxLength(128))
                .addTextInput("count", Component.text("count"), b -> b.initial(String.valueOf(entry.count)).maxLength(8));

        DialogTypeBuilder.MultiAction multi = builder.multiAction();
        multi.addButton(Component.text("Lưu"), Component.text("Lưu thay đổi cho key entry này."), (response, audience) -> {
            String symbol = response.getText("symbol");
            if (symbol != null && !symbol.isBlank()) entry.symbol = symbol.trim().substring(0, 1);
            String itemId = response.getText("item_id");
            if (itemId != null && !itemId.isBlank()) entry.itemId = itemId.trim();
            Integer count = parseInt(response.getText("count"), 1, 6400);
            if (count != null) entry.count = count;
            Utils.runLater(() -> player.showDialog(buildKeyHubDialog(player)));
        });
        multi.addButton(Component.text("Xoá", NamedTextColor.RED), Component.text("Xoá key entry này khỏi danh sách."), (response, audience) -> {
            keyEntries.remove(index);
            Utils.runLater(() -> player.showDialog(buildKeyHubDialog(player)));
        });
        multi.columns(3);
        multi.exitButton(Component.text("« Quay lại"), (response, audience) -> Utils.runLater(() -> player.showDialog(buildKeyHubDialog(player))));

        return multi.build();
    }

    // ==========================================
    // INGREDIENTS (shapeless + fusion)
    // ==========================================

    private Dialog buildIngredientHubDialog(Player player) {
        DialogBuilder builder = DialogBuilder.create(Component.text("Danh sách nguyên liệu"))
                .canCloseWithEscape(false)
                .externalTitle(Component.text("ingredients (" + ingredients.size() + ")", NamedTextColor.YELLOW));

        DialogTypeBuilder.MultiAction multi = builder.multiAction();
        for (int i = 0; i < ingredients.size(); i++) {
            int index = i;
            IngredientDraft ingredient = ingredients.get(i);
            Component label = ingredientLabel(ingredient.itemId, ingredient.count);
            multi.addButton(label, label, (response, audience) -> Utils.runLater(() -> player.showDialog(buildIngredientEntryDialog(player, index))));
        }
        multi.addButton(Component.text("+ Thêm nguyên liệu", NamedTextColor.GREEN), Component.text("Thêm 1 nguyên liệu mới vào danh sách."), (response, audience) -> {
            ingredients.add(new IngredientDraft("", 1));
            int newIndex = ingredients.size() - 1;
            Utils.runLater(() -> player.showDialog(buildIngredientEntryDialog(player, newIndex)));
        });

        multi.columns(3);
        multi.exitButton(Component.text("« Quay lại"), (response, audience) -> Utils.runLater(() -> openMainDialog(player)));
        return multi.build();
    }

    private Dialog buildIngredientEntryDialog(Player player, int index) {
        IngredientDraft ingredient = ingredients.get(index);

        DialogBuilder builder = DialogBuilder.create(Component.text("Nguyên liệu"))
                .canCloseWithEscape(false)
                .addItemBody(previewStack(ingredient.itemId, ingredient.count))
                .addTextInput("item_id", Component.text("item_id"), b -> b.initial(ingredient.itemId).maxLength(128))
                .addTextInput("count", Component.text("count"), b -> b.initial(String.valueOf(ingredient.count)).maxLength(8));

        DialogTypeBuilder.MultiAction multi = builder.multiAction();
        multi.addButton(Component.text("Lưu"), Component.text("Lưu thay đổi cho nguyên liệu này."), (response, audience) -> {
            String itemId = response.getText("item_id");
            if (itemId != null && !itemId.isBlank()) ingredient.itemId = itemId.trim();
            Integer count = parseInt(response.getText("count"), 1, 6400);
            if (count != null) ingredient.count = count;
            Utils.runLater(() -> player.showDialog(buildIngredientHubDialog(player)));
        });
        multi.addButton(Component.text("Xoá", NamedTextColor.RED), Component.text("Xoá nguyên liệu này khỏi danh sách."), (response, audience) -> {
            ingredients.remove(index);
            Utils.runLater(() -> player.showDialog(buildIngredientHubDialog(player)));
        });
        multi.columns(3);
        multi.exitButton(Component.text("« Quay lại"), (response, audience) -> Utils.runLater(() -> player.showDialog(buildIngredientHubDialog(player))));

        return multi.build();
    }

    // ==========================================
    // REMAINDERS
    // ==========================================

    private Dialog buildRemainderHubDialog(Player player) {
        DialogBuilder builder = DialogBuilder.create(Component.text("Danh sách remainder"))
                .canCloseWithEscape(false)
                .externalTitle(Component.text("remainders (" + remainders.size() + ")", NamedTextColor.AQUA))
                .addTextBody(Component.text("Vật phẩm còn lại trong ô sau khi dùng hết 1 nguyên liệu (vd bucket -> empty bucket)."));

        DialogTypeBuilder.MultiAction multi = builder.multiAction();
        for (int i = 0; i < remainders.size(); i++) {
            int index = i;
            RemainderDraft entry = remainders.get(i);
            Component label = ingredientLabel(entry.ingredientId, 1).append(Component.text(" -> ")).append(ingredientLabel(entry.remainderId, 1));
            multi.addButton(label, label, (response, audience) -> Utils.runLater(() -> player.showDialog(buildRemainderEntryDialog(player, index))));
        }
        multi.addButton(Component.text("+ Thêm remainder", NamedTextColor.GREEN), Component.text("Thêm 1 ánh xạ nguyên liệu -> remainder mới."), (response, audience) -> {
            remainders.add(new RemainderDraft("", ""));
            int newIndex = remainders.size() - 1;
            Utils.runLater(() -> player.showDialog(buildRemainderEntryDialog(player, newIndex)));
        });

        multi.columns(3);
        multi.exitButton(Component.text("« Quay lại"), (response, audience) -> Utils.runLater(() -> openMainDialog(player)));
        return multi.build();
    }

    private Dialog buildRemainderEntryDialog(Player player, int index) {
        RemainderDraft entry = remainders.get(index);

        DialogBuilder builder = DialogBuilder.create(Component.text("Remainder"))
                .canCloseWithEscape(false)
                .addItemBody(previewStack(entry.ingredientId, 1))
                .addTextBody(Component.text("↓ trở thành ↓", NamedTextColor.GRAY))
                .addItemBody(previewStack(entry.remainderId, 1))
                .addTextInput("ingredient_id", Component.text("item_id của nguyên liệu"), b -> b.initial(entry.ingredientId).maxLength(128))
                .addTextInput("remainder_id", Component.text("item_id của remainder"), b -> b.initial(entry.remainderId).maxLength(128));

        DialogTypeBuilder.MultiAction multi = builder.multiAction();
        multi.addButton(Component.text("Lưu"), Component.text("Lưu thay đổi cho remainder này."), (response, audience) -> {
            String ingredientId = response.getText("ingredient_id");
            if (ingredientId != null && !ingredientId.isBlank()) entry.ingredientId = ingredientId.trim();
            String remainderId = response.getText("remainder_id");
            if (remainderId != null && !remainderId.isBlank()) entry.remainderId = remainderId.trim();
            Utils.runLater(() -> player.showDialog(buildRemainderHubDialog(player)));
        });
        multi.addButton(Component.text("Xoá", NamedTextColor.RED), Component.text("Xoá remainder này khỏi danh sách."), (response, audience) -> {
            remainders.remove(index);
            Utils.runLater(() -> player.showDialog(buildRemainderHubDialog(player)));
        });
        multi.columns(3);
        multi.exitButton(Component.text("« Quay lại"), (response, audience) -> Utils.runLater(() -> player.showDialog(buildRemainderHubDialog(player))));

        return multi.build();
    }

    // ==========================================
    // SAVE
    // ==========================================

    private Dialog buildSaveDialog(Player player) {
        return DialogBuilder.create(Component.text("Lưu công thức"))
                .canCloseWithEscape(false)
                .addTextBody(Component.text("ID: " + (id == null ? "(chưa đặt)" : id)))
                .addTextBody(Component.text("Loại: " + kind.label))
                .addItemBody(previewStack(resultItemId, resultCount))
                .addTextBody(Component.text("Kết quả: ").append(ingredientLabel(resultItemId, resultCount)))
                .confirmation()
                .yesButton(Component.text("Lưu"), null, (response, audience) -> Utils.runLater(() -> attemptSave(player)))
                .noButton(Component.text("Huỷ"), null, (response, audience) -> Utils.runLater(() -> openMainDialog(player)))
                .build();
    }

    private void attemptSave(Player player) {
        if (id == null || id.isBlank()) {
            player.sendMessage(Utils.text("Bạn cần đặt ID trước khi lưu.", NamedTextColor.RED));
            openMainDialog(player);
            return;
        }
        if (resultItemId.isBlank()) {
            player.sendMessage(Utils.text("Bạn cần đặt kết quả (result) trước khi lưu.", NamedTextColor.RED));
            openMainDialog(player);
            return;
        }

        if (Registries.CRAFTING_RECIPE.get(id) != null) {
            Dialog dialog = DialogBuilder.create(Component.text("Ghi đè công thức?"))
                    .canCloseWithEscape(false)
                    .addTextBody(Component.text("Đã tồn tại một công thức với ID '" + id + "'. Ghi đè?"))
                    .confirmation()
                    .yesButton(Component.text("Ghi đè"), null, (response, audience) -> doSave(player))
                    .noButton(Component.text("Huỷ"), null, (response, audience) -> Utils.runLater(() -> openMainDialog(player)))
                    .build();

            player.showDialog(dialog);
            return;
        }

        doSave(player);
    }

    private void doSave(Player player) {
        CraftingRecipe recipe = toRealRecipe();
        Registries.CRAFTING_RECIPE.registerAndSave(RogueSmpCore.getInstance(), id, recipe);
        CraftingManager.getInstance().rebuild(); // the trie must re-index or the new/edited recipe won't be matchable until restart

        player.sendMessage(Utils.text("Đã lưu công thức '" + id + "'.", NamedTextColor.GREEN));
        Utils.runLater(() -> openMainDialog(player));
    }

    private CraftingRecipe toRealRecipe() {
        CraftingIngredient result = new CraftingIngredient(resultItemId, resultCount);

        Map<String, String> remainderMap = new LinkedHashMap<>();
        for (RemainderDraft draft : remainders) {
            if (!draft.ingredientId.isBlank() && !draft.remainderId.isBlank()) remainderMap.put(draft.ingredientId, draft.remainderId);
        }

        CraftingRecipe.BaseProperties base = new CraftingRecipe.BaseProperties(id, result);

        return switch (kind) {
            case SHAPED -> {
                Map<String, CraftingIngredient> keyMap = new LinkedHashMap<>();
                for (KeyEntryDraft entry : keyEntries) {
                    if (!entry.symbol.isBlank() && !entry.itemId.isBlank()) keyMap.put(entry.symbol, new CraftingIngredient(entry.itemId, entry.count));
                }
                yield new ShapedCraftingRecipe(base, pattern, keyMap, mirrored, remainderMap);
            }
            case SHAPELESS -> new ShapelessCraftingRecipe(base, toRealIngredients());
            case FUSION -> new FusionRecipe(base, new CraftingIngredient(inputItemId, inputCount), toRealIngredients());
        };
    }

    private List<CraftingIngredient> toRealIngredients() {
        List<CraftingIngredient> real = new ArrayList<>();
        for (IngredientDraft draft : ingredients) {
            if (!draft.itemId.isBlank()) real.add(new CraftingIngredient(draft.itemId, draft.count));
        }
        return real;
    }

    private static @Nullable Integer parseInt(@Nullable String text, int min, int max) {
        if (text == null || text.isBlank()) return null;
        try {
            int value = Integer.parseInt(text.trim());
            return Math.max(min, Math.min(max, value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ==========================================
    // DRAFT STATE
    // ==========================================

    private static class KeyEntryDraft {
        String symbol;
        String itemId;
        int count;

        KeyEntryDraft(String symbol, String itemId, int count) {
            this.symbol = symbol;
            this.itemId = itemId;
            this.count = count;
        }
    }

    private static class IngredientDraft {
        String itemId;
        int count;

        IngredientDraft(String itemId, int count) {
            this.itemId = itemId;
            this.count = count;
        }
    }

    private static class RemainderDraft {
        String ingredientId;
        String remainderId;

        RemainderDraft(String ingredientId, String remainderId) {
            this.ingredientId = ingredientId;
            this.remainderId = remainderId;
        }
    }

    private enum RecipeKind {
        SHAPED("shaped", "Vị trí quan trọng: khớp theo đúng pattern trong lưới 3x3 (như crafting vanilla)."),
        SHAPELESS("shapeless", "Vị trí không quan trọng - nhưng 1 nguyên liệu lặp lại nhiều lần cần đúng số slot riêng, không được gộp vào 1 stack."),
        FUSION("fusion", "Slot 0 luôn là 'input' (ô trung tâm) - các nguyên liệu còn lại (slot 1+) là các ô xung quanh, vị trí không quan trọng.");

        final String label;
        final String hint;

        RecipeKind(String label, String hint) {
            this.label = label;
            this.hint = hint;
        }
    }

    // ==========================================
    // COMMAND
    // ==========================================

    public static void registerCommand() {
        new CommandAPICommand("smprecipecreator")
                .withSubcommand(new CommandAPICommand("new")
                        .executesPlayer((player, args) -> {
                            new RecipeCreatorGui().openKindPickerDialog(player);
                        }))
                .withSubcommand(new CommandAPICommand("edit")
                        .withArguments(new StringArgument("recipe_id"))
                        .executesPlayer((player, args) -> {
                            String recipeId = (String) args.get("recipe_id");
                            CraftingRecipe existing = Registries.CRAFTING_RECIPE.get(recipeId);
                            if (existing == null) {
                                player.sendMessage(Utils.text("Không tìm thấy công thức với ID '" + recipeId + "'.", NamedTextColor.RED));
                                return;
                            }
                            RecipeCreatorGui.editExisting(recipeId, existing).openMainDialog(player);
                        }))
                .register();
    }
}
