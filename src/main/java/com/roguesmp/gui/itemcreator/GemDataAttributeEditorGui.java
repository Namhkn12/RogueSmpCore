package com.roguesmp.gui.itemcreator;

import com.roguesmp.attribute.Attributes;
import com.roguesmp.constant.EquipSlot;
import com.roguesmp.utils.Utils;
import com.roguesmp.utils.dialog.DialogBuilder;
import com.roguesmp.utils.dialog.DialogTypeBuilder;
import io.papermc.paper.dialog.Dialog;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.EnumMap;
import java.util.Map;

/**
 * Dialog-based editor for one {@link EquipSlot}'s attribute map inside a
 * {@link com.roguesmp.item.component.impl.GemDataComponent}. Same shape as
 * {@link AttributeEditorGui} (including the hover tooltip from
 * {@link com.roguesmp.attribute.SmpAttribute#getSimpleDescription()}), but scoped to a single
 * slot within the parent {@link GemDataEditorGui}'s (slot -> attribute -> value) map instead of
 * committing a whole standalone {@code EquipAttributeComponent}.
 */
public class GemDataAttributeEditorGui {

    private final GemDataEditorGui parent;
    private final EquipSlot slot;
    private final Map<Attributes, Double> values;

    public GemDataAttributeEditorGui(GemDataEditorGui parent, EquipSlot slot, Map<Attributes, Double> initialValues) {
        this.parent = parent;
        this.slot = slot;
        this.values = new EnumMap<>(initialValues);
    }

    public Dialog buildListDialog(Player player) {
        DialogBuilder builder = DialogBuilder.create(Component.text("Ngọc - " + slot.getSimpleName()))
                .canCloseWithEscape(false)
                .externalTitle(Component.text(slot.getId(), values.isEmpty() ? NamedTextColor.GRAY : NamedTextColor.GREEN));

        DialogTypeBuilder.MultiAction multi = builder.multiAction();

        for (Attributes attribute : Attributes.values()) {
            double current = values.getOrDefault(attribute, 0d);
            boolean valuePresent = !Utils.isEffectiveZero(current);
            multi.addButton(Component.text(attribute.name(), valuePresent ? NamedTextColor.GREEN : NamedTextColor.GRAY),
                    Utils.text(attribute.getAttribute().getSimpleDescription(), NamedTextColor.GRAY),
                    (response, audience) -> Utils.runLater(() -> player.showDialog(buildValueDialog(player, attribute))));
        }

        multi.columns(3);
        multi.exitButton(Component.text("« Quay lại"), (response, audience) -> Utils.runLater(() -> parent.reopen(player)));

        return multi.build();
    }

    private Dialog buildValueDialog(Player player, Attributes attribute) {
        double current = values.getOrDefault(attribute, 0d);
        boolean present = !Utils.isEffectiveZero(current);

        DialogBuilder builder = DialogBuilder.create(Component.text("Chỉ số: " + attribute.name()))
                .canCloseWithEscape(false)
                .externalTitle(Component.text(attribute.name(), present ? NamedTextColor.GREEN : NamedTextColor.GRAY))
                .addTextInput("value", Component.text("Giá trị (-1000 đến 1000)"), b -> b.initial(Utils.formatDecimal(current)).maxLength(16));

        DialogTypeBuilder.MultiAction multi = builder.multiAction();
        multi.addButton(Component.text("Lưu"), null, (response, audience) -> {
            Float value = DialogInputUtils.parseFloat(response.getText("value"), -1000f, 1000f);
            if (value != null) {
                if (Utils.isEffectiveZero(value)) values.remove(attribute);
                else values.put(attribute, (double) (float) value);
            }
            parent.commitSlotAttributes(slot, values);
            Utils.runLater(() -> player.showDialog(buildListDialog(player)));
        });
        if (present) {
            multi.addButton(Component.text("Xoá"), null, (response, audience) -> {
                values.remove(attribute);
                parent.commitSlotAttributes(slot, values);
                Utils.runLater(() -> player.showDialog(buildListDialog(player)));
            });
        }
        multi.columns(present ? 3 : 2);
        multi.exitButton(Component.text("Huỷ"), (response, audience) -> Utils.runLater(() -> player.showDialog(buildListDialog(player))));

        return multi.build();
    }
}
