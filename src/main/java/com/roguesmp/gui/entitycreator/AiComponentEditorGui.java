package com.roguesmp.gui.entitycreator;

import com.roguesmp.entity.ai.AiGoal;
import com.roguesmp.entity.ai.AiPreset;
import com.roguesmp.entity.ai.RemovableGoal;
import com.roguesmp.entity.component.EntityComponentKeys;
import com.roguesmp.entity.component.impl.AiComponent;
import com.roguesmp.registry.Registries;
import com.roguesmp.utils.Utils;
import com.roguesmp.utils.dialog.DialogBuilder;
import com.roguesmp.utils.dialog.DialogTypeBuilder;
import com.roguesmp.utils.dialog.JsonEntryDialog;
import com.roguesmp.utils.dialog.LenientJson;
import io.papermc.paper.dialog.Dialog;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class AiComponentEditorGui {


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
                (response, audience) -> Utils.runLater(() -> player.showDialog(entryDialog(player, -1).buildWithText(player, "type: "))));
        for (int i = 0; i < goals.size(); i++) {
            int index = i;
            AiGoal goal = goals.get(i);
            multi.addButton(Component.text((index + 1) + ". " + goal.getTypeId(), goal.isTarget() ? NamedTextColor.GOLD : NamedTextColor.AQUA),
                    Component.text("Priority: " + goal.priority() + (goal.isTarget() ? " (target)" : ""), NamedTextColor.GRAY),
                    (response, audience) -> Utils.runLater(() -> player.showDialog(entryDialog(player, index).build(player, goals.get(index)))));
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

    private JsonEntryDialog<AiGoal> entryDialog(Player player, int index) {
        boolean editing = index >= 0;
        return JsonEntryDialog.of(editing ? "Goal: " + goals.get(index).getTypeId() : "Goal mới", AiGoal.CODEC,
                goal -> {
                    if (editing) goals.set(index, goal);
                    else goals.add(goal);
                    commit();
                    player.showDialog(buildHubDialog(player));
                },
                editing ? () -> {
                    goals.remove(index);
                    commit();
                    player.showDialog(buildHubDialog(player));
                } : null,
                () -> player.showDialog(buildHubDialog(player)))
                .withHelp(List.of(LenientJson.typesHelp(Registries.AI_GOAL_CODEC.getAll().keySet())));
    }

    private void commit() {
        present = true;
        parent.applyAi(new AiComponent(List.copyOf(presets), List.copyOf(remove), List.copyOf(goals)));
    }
}
