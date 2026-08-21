package com.roguesmp.gui.itemcreator;

import com.roguesmp.attribute.Attributes;
import com.roguesmp.constant.EquipSlot;
import com.roguesmp.item.component.ItemComponentKeys;
import com.roguesmp.item.component.impl.EquipAttributeComponent;
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
 * Dialog-based sub-editor of {@link ItemCreatorGui} for a single {@link EquipAttributeComponent}'s
 * (slot, attribute -> value) map. A {@code multiAction()} with one button per {@link Attributes}
 * constant (its raw enum name as the label, {@link com.roguesmp.attribute.SmpAttribute#getSimpleDescription()}
 * as the hover tooltip); each entry commits straight into the parent's draft on "Lưu"/"Xoá".
 * <p>
 * An <em>empty</em> {@code EquipAttributeComponent} is meaningful on its own - per the item docs,
 * it marks the item as attribute-modifiable (e.g. by gems) even with zero base attributes. So an
 * empty map does not imply "not present": presence is tracked separately via {@link #present},
 * and clearing every individual attribute back to 0 leaves the (now empty) component in place
 * rather than deleting it. Deleting it entirely is its own explicit action ("remove").
 */
public class AttributeEditorGui {

    private final ItemCreatorGui parent;
    private final EquipSlot slot;
    private final Map<Attributes, Double> values;
    private boolean present;

    public AttributeEditorGui(ItemCreatorGui parent, EquipSlot slot, Map<Attributes, Double> initialValues, boolean present) {
        this.parent = parent;
        this.slot = slot;
        this.values = new EnumMap<>(Attributes.class);
        this.values.putAll(initialValues);
        this.present = present;
    }

    public Dialog buildListDialog(Player player) {
        DialogBuilder builder = DialogBuilder.create(Component.text("Chỉ số trang bị - " + slot.getSimpleName()))
                .canCloseWithEscape(false)
                .externalTitle(Component.text(ItemComponentKeys.ATTRIBUTE.id(), present ? NamedTextColor.GREEN : NamedTextColor.GRAY))
                .addTextBody(Component.text(present
                        ? "Vật phẩm có thể thay đổi attribute (" + values.size() + " chỉ số cơ bản)"
                        : "Chưa đánh dấu có thể thay đổi attribute"))
                .addTextBody(Component.text("Lưu ý: với hầu hết chỉ số dạng %, 1 = 100%", NamedTextColor.GRAY));

        DialogTypeBuilder.MultiAction multi = builder.multiAction();

        boolean hasComponent = present;
        multi.addButton(hasComponent ? Component.text("remove", NamedTextColor.RED) : Component.text("mark_modifiable"), null,
                (response, audience) -> Utils.runLater(() -> player.showDialog(hasComponent ? buildRemoveDialog(player) : buildMarkModifiableDialog(player))));

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

    private Dialog buildMarkModifiableDialog(Player player) {
        return DialogBuilder.create(Component.text("Đánh dấu có thể thay đổi attribute"))
                .canCloseWithEscape(false)
                .externalTitle(Component.text("mark_modifiable"))
                .addTextBody(Component.text("Đánh dấu vật phẩm này có thể thay đổi attribute (từ gem, etc...), kể cả khi chưa có chỉ số cơ bản nào?"))
                .confirmation()
                .yesButton(Component.text("Xác nhận"), null, (response, audience) -> {
                    present = true;
                    commit();
                    Utils.runLater(() -> player.showDialog(buildListDialog(player)));
                })
                .noButton(Component.text("Huỷ"), null, (response, audience) -> Utils.runLater(() -> player.showDialog(buildListDialog(player))))
                .build();
    }

    private Dialog buildRemoveDialog(Player player) {
        return DialogBuilder.create(Component.text("Bỏ đánh dấu thay đổi attribute"))
                .canCloseWithEscape(false)
                .externalTitle(Component.text("remove", NamedTextColor.RED))
                .addTextBody(Component.text("Xoá toàn bộ dữ liệu attribute của vật phẩm này?"))
                .confirmation()
                .yesButton(Component.text("Xoá"), null, (response, audience) -> {
                    present = false;
                    values.clear();
                    parent.removeAttribute();
                    Utils.runLater(() -> parent.reopen(player));
                })
                .noButton(Component.text("Huỷ"), null, (response, audience) -> Utils.runLater(() -> player.showDialog(buildListDialog(player))))
                .build();
    }

    private Dialog buildValueDialog(Player player, Attributes attribute) {
        double current = values.getOrDefault(attribute, 0d);
        boolean valuePresent = !Utils.isEffectiveZero(current);

        DialogBuilder builder = DialogBuilder.create(Component.text("Chỉ số: " + attribute.name()))
                .canCloseWithEscape(false)
                .externalTitle(Component.text(attribute.name(), valuePresent ? NamedTextColor.GREEN : NamedTextColor.GRAY))
                .addTextBody(Component.text("Lưu ý: với hầu hết chỉ số dạng %, 1 = 100%", NamedTextColor.GRAY))
                .addTextInput("value", Component.text("Giá trị (-1000 đến 1000)"), b -> b.initial(Utils.formatDecimal(current)).maxLength(16));

        DialogTypeBuilder.MultiAction multi = builder.multiAction();
        multi.addButton(Component.text("Lưu"), null, (response, audience) -> {
            Float value = DialogInputUtils.parseFloat(response.getText("value"), -1000f, 1000f);
            if (value != null) {
                if (Utils.isEffectiveZero(value)) values.remove(attribute);
                else values.put(attribute, (double) (float) value);
            }
            commit();
            Utils.runLater(() -> player.showDialog(buildListDialog(player)));
        });
        if (valuePresent) {
            multi.addButton(Component.text("Xoá"), null, (response, audience) -> {
                values.remove(attribute);
                commit();
                Utils.runLater(() -> player.showDialog(buildListDialog(player)));
            });
        }
        multi.columns(valuePresent ? 3 : 2);
        multi.exitButton(Component.text("Huỷ"), (response, audience) -> Utils.runLater(() -> player.showDialog(buildListDialog(player))));

        return multi.build();
    }

    private void commit() {
        present = true;
        parent.applyAttribute(new EquipAttributeComponent(new EnumMap<>(values), slot));
    }
}
