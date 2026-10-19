package com.roguesmp.gui.itemcreator;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.roguesmp.block.impl.generator.behavior.AddDropsBehavior;
import com.roguesmp.block.impl.generator.behavior.ConvertDropsBehavior;
import com.roguesmp.block.impl.generator.behavior.GeneratorBehavior;
import com.roguesmp.block.impl.generator.stat.GeneratorEffect;
import com.roguesmp.block.impl.generator.stat.GeneratorStat;
import com.roguesmp.block.impl.generator.stat.ModifierOperation;
import com.roguesmp.block.impl.generator.stat.StatModifier;
import com.roguesmp.codec.DataResult;
import com.roguesmp.codec.JsonOps;
import com.roguesmp.utils.Utils;
import com.roguesmp.utils.dialog.DialogBuilder;
import com.roguesmp.utils.dialog.DialogTypeBuilder;
import com.roguesmp.utils.dialog.KeyValueParams;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.input.TextDialogInput;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Consumer;
import java.util.function.Function;

public class GeneratorEffectEditorGui {

    private static final String MODIFIER = "modifier";
    private static final String PARAMS_INPUT = "generator_effect_params";

    private final List<StatModifier> modifiers;
    private final List<GeneratorBehavior> behaviors;
    private final Consumer<GeneratorEffect> onChange;

    public GeneratorEffectEditorGui(GeneratorEffect initial, Consumer<GeneratorEffect> onChange) {
        this.modifiers = new ArrayList<>(initial.modifiers());
        this.behaviors = new ArrayList<>(initial.behaviors());
        this.onChange = onChange;
    }

    public static GeneratorEffect empty() {
        return new GeneratorEffect(List.of(), List.of());
    }

    public GeneratorEffect effect() {
        return new GeneratorEffect(List.copyOf(modifiers), List.copyOf(behaviors));
    }

    public int size() {
        return modifiers.size() + behaviors.size();
    }

    public void addButtons(DialogTypeBuilder.MultiAction multi, Player player, Function<Player, Dialog> hub) {
        multi.addButton(Component.text("+ Thêm hiệu ứng", NamedTextColor.GREEN), null,
                (response, audience) -> Utils.runLater(() -> player.showDialog(buildAddDialog(player, hub))));
        for (int i = 0; i < size(); i++) {
            int index = i;
            boolean isModifier = index < modifiers.size();
            Component label = isModifier
                    ? Component.text((index + 1) + ". ", NamedTextColor.GOLD).append(modifiers.get(index).describe())
                    : Component.text((index + 1) + ". " + behaviors.get(index - modifiers.size()).getTypeId(), NamedTextColor.AQUA);
            Component tooltip = isModifier ? null : Component.join(JoinConfiguration.newlines(), behaviors.get(index - modifiers.size()).getDisplay());
            multi.addButton(label, tooltip, (response, audience) -> Utils.runLater(() -> player.showDialog(buildEntryDialog(player, hub, index, null))));
        }
    }

    private Dialog buildAddDialog(Player player, Function<Player, Dialog> hub) {
        DialogTypeBuilder.MultiAction multi = DialogBuilder.create(Component.text("Chọn loại hiệu ứng"))
                .canCloseWithEscape(false)
                .multiAction();

        multi.addButton(Component.text(MODIFIER, NamedTextColor.GOLD), Component.text("Thay đổi chỉ số máy", NamedTextColor.GRAY), (response, audience) -> {
            modifiers.add(new StatModifier(GeneratorStat.BREAK_DELAY, ModifierOperation.MULTIPLY_BASE, 0));
            commit();
            Utils.runLater(() -> player.showDialog(buildEntryDialog(player, hub, modifiers.size() - 1, null)));
        });
        multi.addButton(Component.text(AddDropsBehavior.TYPE_KEY, NamedTextColor.AQUA), Component.text("Thêm vật phẩm vào mỗi lần khai thác", NamedTextColor.GRAY),
                (response, audience) -> addBehavior(player, hub, new AddDropsBehavior(List.of())));
        multi.addButton(Component.text(ConvertDropsBehavior.TYPE_KEY, NamedTextColor.AQUA), Component.text("Đổi vật phẩm khai thác thành vật phẩm khác", NamedTextColor.GRAY),
                (response, audience) -> addBehavior(player, hub, new ConvertDropsBehavior(Map.of())));
        multi.columns(3);
        multi.exitButton(Component.text("Huỷ"), (response, audience) -> Utils.runLater(() -> player.showDialog(hub.apply(player))));
        return multi.build();
    }

    private void addBehavior(Player player, Function<Player, Dialog> hub, GeneratorBehavior behavior) {
        behaviors.add(behavior);
        commit();
        Utils.runLater(() -> player.showDialog(buildEntryDialog(player, hub, size() - 1, null)));
    }

    private Dialog buildEntryDialog(Player player, Function<Player, Dialog> hub, int index, @Nullable String error) {
        boolean isModifier = index < modifiers.size();
        String type = isModifier ? MODIFIER : behaviors.get(index - modifiers.size()).getTypeId();

        DialogBuilder builder = DialogBuilder.create(Component.text("Hiệu ứng: " + type))
                .canCloseWithEscape(false);
        if (error != null) builder.addTextBody(Component.text(error, NamedTextColor.RED));
        help(type).forEach(line -> builder.addTextBody(line));

        String initial = isModifier ? formatModifier(modifiers.get(index)) : formatBehavior(behaviors.get(index - modifiers.size()));
        TextDialogInput.MultilineOptions multiline = isModifier ? null : TextDialogInput.MultilineOptions.create(10, 120);
        builder.addTextInput(PARAMS_INPUT, Component.text(isModifier ? "Tham số (key=value ...)" : "Mỗi dòng một mục"), 300, true, initial, 2000, multiline);

        DialogTypeBuilder.MultiAction multi = builder.multiAction();
        multi.addButton(Component.text("Lưu"), null, (response, audience) -> {
            String parseError = update(index, response.getText(PARAMS_INPUT));
            Utils.runLater(() -> player.showDialog(parseError != null ? buildEntryDialog(player, hub, index, parseError) : hub.apply(player)));
        });
        multi.addButton(Component.text("Xoá", NamedTextColor.RED), null, (response, audience) -> {
            if (isModifier) modifiers.remove(index);
            else behaviors.remove(index - modifiers.size());
            commit();
            Utils.runLater(() -> player.showDialog(hub.apply(player)));
        });
        multi.columns(2);
        multi.exitButton(Component.text("« Quay lại"), (response, audience) -> Utils.runLater(() -> player.showDialog(hub.apply(player))));
        return multi.build();
    }

    private static List<Component> help(String type) {
        return switch (type) {
            case MODIFIER -> List.of(
                    param("stat", names(GeneratorStat.values())),
                    param("operation", names(ModifierOperation.values()) + " (mặc định multiply_base)"),
                    param("amount", "multiply_base 0.2 = +20%, multiply_total 0.2 = ×1.2, add cộng thẳng"));
            case AddDropsBehavior.TYPE_KEY -> List.of(
                    param("item", "id vật phẩm"),
                    param("min_amount / max_amount", "mặc định 1"),
                    param("chance", "0-1, mặc định 1"));
            default -> List.of(param("<từ>=<thành>", "vd: cobblestone=stone"));
        };
    }

    private static Component param(String name, String description) {
        return Component.text(name, NamedTextColor.YELLOW).append(Component.text(": " + description, NamedTextColor.GRAY));
    }

    private static String names(Enum<?>[] values) {
        return String.join(", ", Arrays.stream(values).map(value -> value.name().toLowerCase(Locale.ROOT)).toList());
    }

    private @Nullable String update(int index, @Nullable String text) {
        boolean isModifier = index < modifiers.size();
        try {
            if (isModifier) {
                DataResult<StatModifier> result = StatModifier.CODEC.decode(KeyValueParams.parse(text), JsonOps.INSTANCE);
                if (!result.isSuccess()) return "Tham số không hợp lệ: " + result.error();
                modifiers.set(index, result.result());
            } else {
                int behaviorIndex = index - modifiers.size();
                DataResult<GeneratorBehavior> result = GeneratorBehavior.CODEC.decode(parseBehavior(behaviors.get(behaviorIndex).getTypeId(), text), JsonOps.INSTANCE);
                if (!result.isSuccess()) return "Tham số không hợp lệ: " + result.error();
                behaviors.set(behaviorIndex, result.result());
            }
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
        commit();
        return null;
    }

    private static JsonObject parseBehavior(String type, @Nullable String text) {
        JsonObject json = new JsonObject();
        json.addProperty("type", type);
        if (type.equals(AddDropsBehavior.TYPE_KEY)) {
            JsonArray drops = new JsonArray();
            for (String line : (text == null ? "" : text).split("\n")) {
                if (!line.isBlank()) drops.add(KeyValueParams.parse(line));
            }
            json.add("drops", drops);
        } else {
            JsonObject conversions = new JsonObject();
            KeyValueParams.parse(text).entrySet().forEach(entry -> conversions.addProperty(entry.getKey(), entry.getValue().getAsString()));
            json.add("conversions", conversions);
        }
        return json;
    }

    private static String formatModifier(StatModifier modifier) {
        DataResult<JsonElement> result = StatModifier.CODEC.encode(modifier, JsonOps.INSTANCE);
        return result.isSuccess() ? KeyValueParams.format(result.result(), Set.of()).toLowerCase(Locale.ROOT) : "";
    }

    private static String formatBehavior(GeneratorBehavior behavior) {
        StringJoiner lines = new StringJoiner("\n");
        if (behavior instanceof AddDropsBehavior addDrops) {
            addDrops.drops().forEach(drop -> lines.add("item=" + drop.item() + " min_amount=" + drop.minAmount()
                    + " max_amount=" + drop.maxAmount() + " chance=" + Utils.formatDecimal(drop.chance())));
        } else if (behavior instanceof ConvertDropsBehavior convert) {
            convert.conversions().forEach((from, to) -> lines.add(from + "=" + to));
        }
        return lines.toString();
    }

    private void commit() {
        onChange.accept(effect());
    }
}
