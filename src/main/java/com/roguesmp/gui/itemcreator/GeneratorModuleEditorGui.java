package com.roguesmp.gui.itemcreator;

import com.roguesmp.block.impl.generator.catalyst.CatalystSynergy;
import com.roguesmp.block.impl.generator.catalyst.CatalystType;
import com.roguesmp.item.component.ItemComponentKeys;
import com.roguesmp.item.component.impl.GeneratorModuleComponent;
import com.roguesmp.registry.Registries;
import com.roguesmp.utils.Utils;
import com.roguesmp.utils.dialog.DialogBuilder;
import com.roguesmp.utils.dialog.DialogTypeBuilder;
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
    private final List<SynergyEntry> synergies = new ArrayList<>();

    public GeneratorModuleEditorGui(ItemCreatorGui parent, @Nullable GeneratorModuleComponent current) {
        this.parent = parent;
        this.present = current != null;
        this.incompatible = current == null ? List.of() : current.incompatible();
        this.maxPerMachine = current == null ? 0 : current.maxPerMachine();
        this.effect = new GeneratorEffectEditorGui(current == null ? GeneratorEffectEditorGui.empty() : current.effect(), value -> commit());
        if (current != null) current.synergies().forEach(synergy -> synergies.add(new SynergyEntry(synergy.type().getId(), synergy)));
    }

    public Dialog buildHubDialog(Player player) {
        DialogTypeBuilder.MultiAction multi = DialogBuilder.create(Component.text("Module máy tạo tài nguyên"))
                .canCloseWithEscape(false)
                .externalTitle(Component.text(ItemComponentKeys.GENERATOR_MODULE.id(), present ? NamedTextColor.GREEN : NamedTextColor.GRAY))
                .addTextBody(Component.text("Tối đa mỗi máy: " + (maxPerMachine <= 0 ? "(không giới hạn)" : maxPerMachine)))
                .addTextBody(Component.text("Xung khắc: " + (incompatible.isEmpty() ? "(không)" : String.join(", ", incompatible))))
                .addTextBody(Component.text("Hiệu ứng: " + effect.size() + "  |  Synergy: " + synergies.size()))
                .addTextBody(Component.text("Vàng = modifier chỉ số, xanh = behavior, tím = synergy.", NamedTextColor.DARK_GRAY))
                .multiAction();

        multi.addButton(Component.text("settings", NamedTextColor.GREEN), null,
                (response, audience) -> Utils.runLater(() -> player.showDialog(buildSettingsDialog(player))));
        multi.addButton(Component.text("+ Thêm synergy", NamedTextColor.GREEN), null,
                (response, audience) -> Utils.runLater(() -> player.showDialog(buildAddSynergyDialog(player))));
        effect.addButtons(multi, player, this::buildHubDialog);
        for (int i = 0; i < synergies.size(); i++) {
            int index = i;
            SynergyEntry synergy = synergies.get(i);
            multi.addButton(Component.text("synergy: " + synergy.typeId, NamedTextColor.LIGHT_PURPLE),
                    Component.text("Hiệu ứng: " + synergy.effect.size(), NamedTextColor.GRAY),
                    (response, audience) -> Utils.runLater(() -> player.showDialog(buildSynergyDialog(player, index))));
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

    private Dialog buildAddSynergyDialog(Player player) {
        DialogTypeBuilder.MultiAction multi = DialogBuilder.create(Component.text("Chọn loại chất xúc tác"))
                .canCloseWithEscape(false)
                .addTextBody(Component.text("Hiệu ứng synergy chỉ áp dụng khi máy đang chạy chất xúc tác thuộc loại này.", NamedTextColor.GRAY))
                .multiAction();

        for (String typeId : new TreeSet<>(Registries.CATALYST_TYPE.getAll().keySet())) {
            CatalystType type = Registries.CATALYST_TYPE.get(typeId);
            multi.addButton(Component.text(typeId), Utils.fromString(type.displayText()), (response, audience) -> {
                synergies.add(new SynergyEntry(typeId, null));
                commit();
                Utils.runLater(() -> player.showDialog(buildSynergyDialog(player, synergies.size() - 1)));
            });
        }
        multi.columns(3);
        multi.exitButton(Component.text("Huỷ"), (response, audience) -> Utils.runLater(() -> player.showDialog(buildHubDialog(player))));
        return multi.build();
    }

    private Dialog buildSynergyDialog(Player player, int index) {
        SynergyEntry synergy = synergies.get(index);
        DialogTypeBuilder.MultiAction multi = DialogBuilder.create(Component.text("Synergy: " + synergy.typeId))
                .canCloseWithEscape(false)
                .addTextBody(Component.text("Hiệu ứng: " + synergy.effect.size()))
                .multiAction();

        synergy.effect.addButtons(multi, player, p -> buildSynergyDialog(p, index));
        multi.addButton(Component.text("Xoá synergy", NamedTextColor.RED), null, (response, audience) -> {
            synergies.remove(index);
            commit();
            Utils.runLater(() -> player.showDialog(buildHubDialog(player)));
        });
        multi.columns(2);
        multi.exitButton(Component.text("« Quay lại"), (response, audience) -> Utils.runLater(() -> player.showDialog(buildHubDialog(player))));
        return multi.build();
    }

    private void commit() {
        present = true;
        List<CatalystSynergy> built = synergies.stream()
                .map(entry -> new CatalystSynergy(Registries.CATALYST_TYPE.getHolder(entry.typeId), entry.effect.effect()))
                .toList();
        parent.applyComponent(ItemComponentKeys.GENERATOR_MODULE, new GeneratorModuleComponent(effect.effect(), incompatible, maxPerMachine, built));
    }

    private final class SynergyEntry {
        private final String typeId;
        private final GeneratorEffectEditorGui effect;

        private SynergyEntry(String typeId, @Nullable CatalystSynergy initial) {
            this.typeId = typeId;
            this.effect = new GeneratorEffectEditorGui(initial == null ? GeneratorEffectEditorGui.empty() : initial.effect(), value -> commit());
        }
    }
}
