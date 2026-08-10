package com.roguesmp.gui.itemcreator;

import com.roguesmp.attribute.Attributes;
import com.roguesmp.constant.EquipSlot;
import com.roguesmp.item.component.ItemComponentKeys;
import com.roguesmp.item.component.impl.GemDataComponent;
import com.roguesmp.utils.Utils;
import com.roguesmp.utils.dialog.DialogBuilder;
import com.roguesmp.utils.dialog.DialogTypeBuilder;
import io.papermc.paper.dialog.Dialog;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Dialog-based hub for a {@link GemDataComponent}: a success-chance field plus one
 * {@link GemDataAttributeEditorGui} entry per {@link EquipSlot} (unlike
 * {@link com.roguesmp.item.component.impl.EquipAttributeComponent}, a gem can carry bonuses for
 * more than one slot at once). Every leaf edit re-commits the whole component into the parent
 * {@link ItemCreatorGui} draft.
 */
public class GemDataEditorGui {

    private final ItemCreatorGui parent;
    private boolean present;
    private double successChance;
    private final Map<EquipSlot, Map<Attributes, Double>> attributes;

    public GemDataEditorGui(ItemCreatorGui parent, @Nullable GemDataComponent current) {
        this.parent = parent;
        this.present = current != null;
        this.successChance = current == null ? 1d : current.getSuccessChance();
        this.attributes = new EnumMap<>(EquipSlot.class);
        if (current != null) {
            current.getAttributes().forEach((slot, map) -> {
                Map<Attributes, Double> copy = new EnumMap<>(Attributes.class);
                copy.putAll(map);
                attributes.put(slot, copy);
            });
        }
    }

    public Dialog buildHubDialog(Player player) {
        DialogTypeBuilder.DialogList listBuilder = DialogBuilder.create(Component.text("Dữ liệu ngọc"))
                .canCloseWithEscape(false)
                .externalTitle(Component.text(ItemComponentKeys.GEM_DATA.id(), present ? NamedTextColor.GREEN : NamedTextColor.GRAY))
                .addTextBody(Component.text("Tỉ lệ khảm thành công: " + Utils.formatDecimal(successChance * 100) + "%"))
                .dialogList()
                .columns(3)
                .buttonWidth(120)
                .exitButton(Component.text("« Quay lại"), (response, audience) -> Utils.runLater(() -> parent.reopen(player)));

        listBuilder.addDialog(buildSuccessChanceDialog(player));
        for (EquipSlot slot : EquipSlot.values()) {
            Map<Attributes, Double> slotValues = attributes.getOrDefault(slot, new EnumMap<>(Attributes.class));
            listBuilder.addDialog(new GemDataAttributeEditorGui(this, slot, slotValues).buildListDialog(player));
        }
        if (present) listBuilder.addDialog(buildRemoveDialog(player));

        return listBuilder.build();
    }

    private Dialog buildSuccessChanceDialog(Player player) {
        return DialogBuilder.create(Component.text("Tỉ lệ khảm thành công"))
                .canCloseWithEscape(false)
                .externalTitle(Component.text("success_chance"))
                .addTextInput("value", Component.text("Tỉ lệ (%, 0-100)"), b -> b.initial(Utils.formatDecimal(successChance * 100)).maxLength(16))
                .confirmation()
                .yesButton(Component.text("Xác nhận"), null, (response, audience) -> {
                    Float value = DialogInputUtils.parseFloat(response.getText("value"), 0f, 100f);
                    if (value != null) {
                        successChance = value / 100d;
                        commit();
                    }
                    Utils.runLater(() -> player.showDialog(buildHubDialog(player)));
                })
                .noButton(Component.text("Huỷ"), null, (response, audience) -> Utils.runLater(() -> player.showDialog(buildHubDialog(player))))
                .build();
    }

    private Dialog buildRemoveDialog(Player player) {
        return DialogBuilder.create(Component.text("Xoá dữ liệu ngọc"))
                .canCloseWithEscape(false)
                .externalTitle(Component.text("remove", NamedTextColor.RED))
                .addTextBody(Component.text("Xoá toàn bộ dữ liệu ngọc của vật phẩm này?"))
                .confirmation()
                .yesButton(Component.text("Xoá"), null, (response, audience) -> {
                    present = false;
                    attributes.clear();
                    successChance = 1d;
                    parent.removeGemData();
                    Utils.runLater(() -> parent.reopen(player));
                })
                .noButton(Component.text("Huỷ"), null, (response, audience) -> Utils.runLater(() -> player.showDialog(buildHubDialog(player))))
                .build();
    }

    void commitSlotAttributes(EquipSlot slot, Map<Attributes, Double> newValues) {
        if (newValues.isEmpty()) attributes.remove(slot);
        else attributes.put(slot, newValues);
        commit();
    }

    void reopen(Player player) {
        player.showDialog(buildHubDialog(player));
    }

    private void commit() {
        present = true;
        Map<EquipSlot, Map<Attributes, Double>> nonEmpty = new EnumMap<>(EquipSlot.class);
        attributes.forEach((slot, map) -> {
            if (!map.isEmpty()) nonEmpty.put(slot, map);
        });
        parent.applyGemData(new GemDataComponent(nonEmpty, successChance));
    }
}
