package com.roguesmp.gui.itemcreator;

import com.roguesmp.block.impl.generator.catalyst.CatalystSynergy;
import com.roguesmp.block.impl.generator.stat.GeneratorEffect;
import com.roguesmp.item.component.ItemComponentKeys;
import com.roguesmp.item.component.impl.GeneratorModuleComponent;
import com.roguesmp.registry.Registries;
import com.roguesmp.utils.Utils;
import com.roguesmp.utils.dialog.DialogBuilder;
import com.roguesmp.utils.dialog.DialogTypeBuilder;
import com.roguesmp.utils.dialog.JsonEntryDialog;
import io.papermc.paper.dialog.Dialog;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

public class GeneratorModuleEditorGui {

    private final ItemCreatorGui parent;
    private boolean present;
    private List<String> incompatible;
    private int maxPerMachine;
    private final GeneratorEffectEditorGui effect;
    private final List<CatalystSynergy> synergies = new ArrayList<>();

    public GeneratorModuleEditorGui(ItemCreatorGui parent, @Nullable GeneratorModuleComponent current) {
        this.parent = parent;
        this.present = current != null;
        this.incompatible = current == null ? List.of() : current.incompatible();
        this.maxPerMachine = current == null ? 0 : current.maxPerMachine();
        this.effect = new GeneratorEffectEditorGui(current == null ? GeneratorEffectEditorGui.empty() : current.effect(), value -> commit());
        if (current != null) synergies.addAll(current.synergies());
    }

    public Dialog buildHubDialog(Player player) {
        DialogTypeBuilder.MultiAction multi = DialogBuilder.create(Component.text("Module máy tạo tài nguyên"))
                .canCloseWithEscape(false)
                .externalTitle(Component.text(ItemComponentKeys.GENERATOR_MODULE.id(), present ? NamedTextColor.GREEN : NamedTextColor.GRAY))
                .addTextBody(Component.text("Tối đa mỗi máy: " + (maxPerMachine <= 0 ? "(không giới hạn)" : maxPerMachine)))
                .addTextBody(Component.text("Xung khắc: " + (incompatible.isEmpty() ? "(không)" : String.join(", ", incompatible))))
                .addTextBody(Component.text("Hiệu ứng: " + effect.size() + "  |  Synergy: " + synergies.size()))
                .addTextBody(Component.text("Vàng = hiệu ứng (modifier + behavior), tím = synergy.", NamedTextColor.DARK_GRAY))
                .multiAction();

        multi.addButton(Component.text("settings", NamedTextColor.GREEN), null,
                (response, audience) -> Utils.runLater(() -> player.showDialog(buildSettingsDialog(player))));
        multi.addButton(Component.text("+ Thêm synergy", NamedTextColor.GREEN), null,
                (response, audience) -> Utils.runLater(() -> player.showDialog(synergyDialog(player, -1).buildWithText(player, "type: "))));
        effect.addButtons(multi, player, this::buildHubDialog);
        for (int i = 0; i < synergies.size(); i++) {
            int index = i;
            CatalystSynergy synergy = synergies.get(i);
            GeneratorEffect synergyEffect = synergy.effect();
            multi.addButton(Component.text("synergy: " + synergy.type().getId(), NamedTextColor.LIGHT_PURPLE),
                    Component.text("Hiệu ứng: " + (synergyEffect.modifiers().size() + synergyEffect.behaviors().size()), NamedTextColor.GRAY),
                    (response, audience) -> Utils.runLater(() -> player.showDialog(synergyDialog(player, index).build(player, synergy))));
        }
        if (present) {
            multi.addButton(Component.text("Xoá toàn bộ", NamedTextColor.RED), null, (response, audience) -> {
                present = false;
                parent.removeComponent(ItemComponentKeys.GENERATOR_MODULE);
                Utils.runLater(() -> parent.reopen(player));
            });
        }
        multi.columns(2);
        multi.exitButton(Component.text("« Quay lại"), (response, audience) -> Utils.runLater(() -> parent.reopen(player)));
        return multi.build();
    }

    private Dialog buildSettingsDialog(Player player) {
        return DialogBuilder.create(Component.text("Cài đặt module"))
                .canCloseWithEscape(false)
                .addTextInput("max", Component.text("Tối đa mỗi máy (0 = không giới hạn)"), b -> b.initial(String.valueOf(maxPerMachine)).maxLength(16))
                .addTextInput("incompatible", Component.text("ID module xung khắc (cách nhau bởi dấu cách)"), b -> b.initial(String.join(" ", incompatible)).maxLength(1000))
                .confirmation()
                .yesButton(Component.text("Lưu"), null, (response, audience) -> {
                    Integer max = DialogInputUtils.parseInt(response.getText("max"), 0, Integer.MAX_VALUE);
                    List<String> ids = Arrays.stream(Objects.requireNonNullElse(response.getText("incompatible"), "").trim().split("\\s+"))
                            .filter(id -> !id.isEmpty())
                            .toList();
                    List<String> unknown = ids.stream().filter(id -> Registries.ITEM.get(id) == null).toList();
                    if (!unknown.isEmpty()) {
                        player.sendMessage(Utils.text("Không tìm thấy vật phẩm: " + String.join(", ", unknown), NamedTextColor.RED));
                    } else if (max != null) {
                        maxPerMachine = max;
                        incompatible = ids;
                        commit();
                    }
                    Utils.runLater(() -> player.showDialog(buildHubDialog(player)));
                })
                .noButton(Component.text("Huỷ"), null, (response, audience) -> Utils.runLater(() -> player.showDialog(buildHubDialog(player))))
                .build();
    }

    private JsonEntryDialog<CatalystSynergy> synergyDialog(Player player, int index) {
        boolean editing = index >= 0;
        return JsonEntryDialog.of(editing ? "Synergy: " + synergies.get(index).type().getId() : "Synergy mới", CatalystSynergy.CODEC,
                synergy -> {
                    if (editing) synergies.set(index, synergy);
                    else synergies.add(synergy);
                    commit();
                    player.showDialog(buildHubDialog(player));
                },
                editing ? () -> {
                    synergies.remove(index);
                    commit();
                    player.showDialog(buildHubDialog(player));
                } : null,
                () -> player.showDialog(buildHubDialog(player)))
                .withHelp(List.of(
                        Component.text("type (chất xúc tác): ", NamedTextColor.YELLOW)
                                .append(Component.text(String.join(", ", new TreeSet<>(Registries.CATALYST_TYPE.getAll().keySet())), NamedTextColor.GRAY))));
    }

    private void commit() {
        present = true;
        parent.applyComponent(ItemComponentKeys.GENERATOR_MODULE, new GeneratorModuleComponent(effect.effect(), incompatible, maxPerMachine, List.copyOf(synergies)));
    }
}
