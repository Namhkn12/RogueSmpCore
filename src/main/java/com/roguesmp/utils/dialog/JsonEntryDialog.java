package com.roguesmp.utils.dialog;

import com.roguesmp.codec.Codec;
import com.roguesmp.codec.DataResult;
import com.roguesmp.utils.Utils;
import io.papermc.paper.dialog.Dialog;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public final class JsonEntryDialog<T> {

    private static final String INPUT = "json";

    private final String title;
    private final Codec<T> codec;
    private final Consumer<T> onSave;
    private final @Nullable Runnable onDelete;
    private final Runnable onBack;
    private List<Component> help = List.of();
    private Function<T, @Nullable String> validator = value -> null;

    private JsonEntryDialog(String title, Codec<T> codec, Consumer<T> onSave, @Nullable Runnable onDelete, Runnable onBack) {
        this.title = title;
        this.codec = codec;
        this.onSave = onSave;
        this.onDelete = onDelete;
        this.onBack = onBack;
    }

    public static <T> JsonEntryDialog<T> of(String title, Codec<T> codec, Consumer<T> onSave, @Nullable Runnable onDelete, Runnable onBack) {
        return new JsonEntryDialog<>(title, codec, onSave, onDelete, onBack);
    }

    public JsonEntryDialog<T> withHelp(List<Component> help) {
        this.help = List.copyOf(help);
        return this;
    }

    public JsonEntryDialog<T> withValidator(Function<T, @Nullable String> validator) {
        this.validator = validator;
        return this;
    }

    public Dialog build(Player player, @Nullable T current) {
        return build(player, current == null ? "" : LenientJson.format(codec, current), null);
    }

    public Dialog buildWithText(Player player, String text) {
        return build(player, text, null);
    }

    private Dialog build(Player player, String text, @Nullable String error) {
        DialogBuilder builder = DialogBuilder.create(Component.text(title))
                .canCloseWithEscape(false)
                .addTextBody(Component.text(LenientJson.HELP, NamedTextColor.GRAY));
        help.forEach(builder::addTextBody);
        if (error != null) builder.addTextBody(Component.text(error, NamedTextColor.RED));
        LenientJson.addInput(builder, INPUT, Component.text("JSON"), text);

        DialogTypeBuilder.MultiAction multi = builder.multiAction();
        multi.addButton(Component.text("Lưu"), null, (response, audience) -> {
            String input = response.getText(INPUT);
            DataResult<T> result = LenientJson.decode(codec, input);
            String saveError = result.isSuccess() ? validator.apply(result.result()) : result.error();
            if (saveError != null) {
                Utils.runLater(() -> player.showDialog(build(player, input == null ? "" : input, saveError)));
                return;
            }
            Utils.runLater(() -> onSave.accept(result.result()));
        });
        if (onDelete != null) {
            multi.addButton(Component.text("Xoá", NamedTextColor.RED), null, (response, audience) -> Utils.runLater(onDelete));
        }
        multi.columns(2);
        multi.exitButton(Component.text("« Quay lại"), (response, audience) -> Utils.runLater(onBack));
        return multi.build();
    }
}
