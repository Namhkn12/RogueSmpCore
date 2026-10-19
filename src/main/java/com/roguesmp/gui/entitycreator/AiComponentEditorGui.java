package com.roguesmp.gui.entitycreator;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.roguesmp.codec.Codec;
import com.roguesmp.codec.DataResult;
import com.roguesmp.codec.JsonOps;
import com.roguesmp.entity.ai.AiGoal;
import com.roguesmp.entity.ai.AiGoals;
import com.roguesmp.entity.ai.AiPreset;
import com.roguesmp.entity.ai.RemovableGoal;
import com.roguesmp.entity.component.EntityComponentKeys;
import com.roguesmp.entity.component.impl.AiComponent;
import com.roguesmp.registry.Registries;
import com.roguesmp.utils.Utils;
import com.roguesmp.utils.dialog.DialogBuilder;
import com.roguesmp.utils.dialog.DialogTypeBuilder;
import com.roguesmp.utils.dialog.KeyValueParams;
import io.papermc.paper.dialog.Dialog;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class AiComponentEditorGui {

    private static final String PARAMS_INPUT = "ai_goal_params";

    private final EntityCreatorGui parent;
    private boolean present;
    private final Set<AiPreset> presets = EnumSet.noneOf(AiPreset.class);
    private final Set<RemovableGoal> remove = EnumSet.noneOf(RemovableGoal.class);
    private final List<AiGoal> goals;

    public AiComponentEditorGui(EntityCreatorGui parent, @Nullable AiComponent current) {
        this.parent = parent;
        this.present = current != null;
        if (current != null) {
            presets.addAll(current.presets());
            remove.addAll(current.remove());
        }
        this.goals = new ArrayList<>(current == null ? List.of() : current.goals());
    }

    public Dialog buildHubDialog(Player player) {
        DialogBuilder builder = DialogBuilder.create(Component.text("AI entity"))
                .canCloseWithEscape(false)
                .externalTitle(Component.text(EntityComponentKeys.AI.id(), present ? NamedTextColor.GREEN : NamedTextColor.GRAY))
                .addTextBody(Component.text("Preset: " + (presets.isEmpty() ? "(không)" : String.join(", ", presets.stream().map(AiPreset::id).toList()))))
                .addTextBody(Component.text("Xoá: " + (remove.isEmpty() ? "(không)" : String.join(", ", remove.stream().map(RemovableGoal::id).toList()))))
                .addTextBody(Component.text("Goal: " + goals.size()))
                .addTextBody(Component.text("Target: minecraft:<type>, #<tag entity> hoặc id entity custom.", NamedTextColor.DARK_GRAY))
                .addTextBody(Component.text("Vàng = goal chọn mục tiêu, xanh = goal hành động.", NamedTextColor.DARK_GRAY));

        DialogTypeBuilder.MultiAction multi = builder.multiAction();
        multi.addButton(Component.text("presets", presets.isEmpty() ? NamedTextColor.GRAY : NamedTextColor.GREEN), null,
                (response, audience) -> Utils.runLater(() -> player.showDialog(buildPresetDialog(player))));
        multi.addButton(Component.text("remove", remove.isEmpty() ? NamedTextColor.GRAY : NamedTextColor.GREEN), null,
                (response, audience) -> Utils.runLater(() -> player.showDialog(buildRemoveDialog(player))));
        multi.addButton(Component.text("+ Thêm goal", NamedTextColor.GREEN), null,
                (response, audience) -> Utils.runLater(() -> player.showDialog(buildAddDialog(player))));
        for (int i = 0; i < goals.size(); i++) {
            int index = i;
            AiGoal goal = goals.get(i);
            multi.addButton(Component.text((index + 1) + ". " + goal.getTypeId(), goal.isTarget() ? NamedTextColor.GOLD : NamedTextColor.AQUA),
                    Component.text("Priority: " + goal.priority() + (goal.isTarget() ? " (target)" : ""), NamedTextColor.GRAY),
                    (response, audience) -> Utils.runLater(() -> player.showDialog(buildEntryDialog(player, index, null))));
        }
        if (present) {
            multi.addButton(Component.text("Xoá toàn bộ", NamedTextColor.RED), null, (response, audience) -> {
                present = false;
                presets.clear();
                remove.clear();
                goals.clear();
                parent.removeAi();
                Utils.runLater(() -> parent.reopen(player));
            });
        }
        multi.columns(2);
        multi.exitButton(Component.text("« Quay lại"), (response, audience) -> Utils.runLater(() -> parent.reopen(player)));

        return multi.build();
    }

    private Dialog buildPresetDialog(Player player) {
        DialogBuilder builder = DialogBuilder.create(Component.text("Preset AI"))
                .canCloseWithEscape(false)
                .addTextBody(Component.text("Preset chạy trước remove/goal, nên goal tự thêm vẫn ghi đè được preset.", NamedTextColor.GRAY));
        for (AiPreset preset : AiPreset.values()) {
            builder.addTextBody(Component.text(preset.id(), NamedTextColor.YELLOW).append(Component.text(": " + preset.description(), NamedTextColor.GRAY)));
            builder.addCheckboxInput("ai_preset_" + preset.id(), Component.text(preset.id()), presets.contains(preset), "true", "false");
        }

        DialogTypeBuilder.MultiAction multi = builder.multiAction();
        multi.addButton(Component.text("Lưu"), null, (response, audience) -> {
            presets.clear();
            for (AiPreset preset : AiPreset.values()) {
                if (Boolean.TRUE.equals(response.getBoolean("ai_preset_" + preset.id()))) presets.add(preset);
            }
            commit();
            Utils.runLater(() -> player.showDialog(buildHubDialog(player)));
        });
        multi.columns(2);
        multi.exitButton(Component.text("Huỷ"), (response, audience) -> Utils.runLater(() -> player.showDialog(buildHubDialog(player))));

        return multi.build();
    }

    private Dialog buildRemoveDialog(Player player) {
        DialogBuilder builder = DialogBuilder.create(Component.text("Xoá goal vanilla"))
                .canCloseWithEscape(false)
                .addTextBody(Component.text("Các goal vanilla được chọn sẽ bị xoá trước khi thêm goal mới.", NamedTextColor.GRAY));
        for (RemovableGoal goal : RemovableGoal.values()) {
            builder.addCheckboxInput("ai_remove_" + goal.id(), Component.text(goal.id()), remove.contains(goal), "true", "false");
        }

        DialogTypeBuilder.MultiAction multi = builder.multiAction();
        multi.addButton(Component.text("Lưu"), null, (response, audience) -> {
            remove.clear();
            for (RemovableGoal goal : RemovableGoal.values()) {
                if (Boolean.TRUE.equals(response.getBoolean("ai_remove_" + goal.id()))) remove.add(goal);
            }
            commit();
            Utils.runLater(() -> player.showDialog(buildHubDialog(player)));
        });
        multi.columns(2);
        multi.exitButton(Component.text("Huỷ"), (response, audience) -> Utils.runLater(() -> player.showDialog(buildHubDialog(player))));

        return multi.build();
    }

    private Dialog buildAddDialog(Player player) {
        DialogBuilder builder = DialogBuilder.create(Component.text("Chọn loại goal"))
                .canCloseWithEscape(false);

        DialogTypeBuilder.MultiAction multi = builder.multiAction();
        for (String type : new TreeSet<>(Registries.AI_GOAL_CODEC.getAll().keySet())) {
            multi.addButton(Component.text(type), null, (response, audience) -> {
                AiGoal goal = createDefault(type);
                if (goal == null) {
                    player.sendMessage(Utils.text("Không thể tạo goal mặc định '" + type + "'.", NamedTextColor.RED));
                    Utils.runLater(() -> player.showDialog(buildHubDialog(player)));
                    return;
                }
                goals.add(goal);
                commit();
                Utils.runLater(() -> player.showDialog(buildEntryDialog(player, goals.size() - 1, null)));
            });
        }
        multi.columns(3);
        multi.exitButton(Component.text("Huỷ"), (response, audience) -> Utils.runLater(() -> player.showDialog(buildHubDialog(player))));

        return multi.build();
    }

    private Dialog buildEntryDialog(Player player, int index, @Nullable String error) {
        AiGoal goal = goals.get(index);
        DialogBuilder builder = DialogBuilder.create(Component.text("Goal: " + goal.getTypeId()))
                .canCloseWithEscape(false);
        if (error != null) builder.addTextBody(Component.text(error, NamedTextColor.RED));
        for (AiGoals.Param param : AiGoals.params(goal.getTypeId())) {
            builder.addTextBody(Component.text(param.name(), NamedTextColor.YELLOW)
                    .append(Component.text(": " + param.description(), NamedTextColor.GRAY)));
        }
        builder.addTextInput(PARAMS_INPUT, Component.text("Tham số (key=value key=value ...)"), 300, true, formatParams(goal), 512, null);

        DialogTypeBuilder.MultiAction multi = builder.multiAction();
        multi.addButton(Component.text("Lưu"), null, (response, audience) -> {
            String parseError = updateGoal(index, response.getText(PARAMS_INPUT));
            Utils.runLater(() -> {
                if (parseError != null) player.showDialog(buildEntryDialog(player, index, parseError));
                else player.showDialog(buildHubDialog(player));
            });
        });
        multi.addButton(Component.text("Xoá", NamedTextColor.RED), null, (response, audience) -> {
            goals.remove(index);
            commit();
            Utils.runLater(() -> player.showDialog(buildHubDialog(player)));
        });
        multi.columns(2);
        multi.exitButton(Component.text("« Quay lại"), (response, audience) -> Utils.runLater(() -> player.showDialog(buildHubDialog(player))));

        return multi.build();
    }

    private @Nullable String updateGoal(int index, @Nullable String text) {
        JsonObject params;
        try {
            params = KeyValueParams.parse(text);
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }

        params.addProperty("type", goals.get(index).getTypeId());
        DataResult<AiGoal> result = AiGoal.CODEC.decode(params, JsonOps.INSTANCE);
        if (!result.isSuccess()) return "Tham số không hợp lệ: " + result.error();

        goals.set(index, result.result());
        commit();
        return null;
    }

    private static String formatParams(AiGoal goal) {
        DataResult<JsonElement> result = AiGoal.CODEC.encode(goal, JsonOps.INSTANCE);
        return result.isSuccess() ? KeyValueParams.format(result.result(), Set.of("type")) : "";
    }

    private static @Nullable AiGoal createDefault(String type) {
        Codec<? extends AiGoal> codec = Registries.AI_GOAL_CODEC.get(type);
        if (codec == null) return null;
        DataResult<? extends AiGoal> result = codec.decode(new JsonObject(), JsonOps.INSTANCE);
        return result.isSuccess() ? result.result() : null;
    }

    private void commit() {
        present = true;
        parent.applyAi(new AiComponent(List.copyOf(presets), List.copyOf(remove), List.copyOf(goals)));
    }
}
