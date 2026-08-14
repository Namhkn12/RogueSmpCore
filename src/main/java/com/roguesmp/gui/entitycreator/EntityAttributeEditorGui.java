package com.roguesmp.gui.entitycreator;

import com.roguesmp.entity.EntityAttribute;
import com.roguesmp.entity.component.EntityComponentKeys;
import com.roguesmp.entity.component.impl.AttributeComponent;
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
 * Dialog-based sub-editor of {@link EntityCreatorGui} for a single {@link AttributeComponent}'s
 * (attribute -> base value) map. Mirrors {@code gui.itemcreator.EnchantEditorGui}'s shape - one
 * button per {@link EntityAttribute} constant, immediate-commit-per-leaf-edit.
 */
public class EntityAttributeEditorGui {

    private final EntityCreatorGui parent;
    private final Map<EntityAttribute, Double> values;
    private boolean present;

    public EntityAttributeEditorGui(EntityCreatorGui parent, Map<EntityAttribute, Double> initialValues, boolean present) {
        this.parent = parent;
        this.values = new EnumMap<>(EntityAttribute.class);
        this.values.putAll(initialValues);
        this.present = present;
    }

    public Dialog buildListDialog(Player player) {
        DialogBuilder builder = DialogBuilder.create(Component.text("Attribute entity"))
                .canCloseWithEscape(false)
                .externalTitle(Component.text(EntityComponentKeys.ATTRIBUTES.id(), present ? NamedTextColor.GREEN : NamedTextColor.GRAY))
                .addTextBody(Component.text(values.size() + " attribute đã đặt"));

        DialogTypeBuilder.MultiAction multi = builder.multiAction();
        for (EntityAttribute attribute : EntityAttribute.values()) {
            Double current = values.get(attribute);
            multi.addButton(Component.text(attribute.name(), current != null ? NamedTextColor.GREEN : NamedTextColor.GRAY), null,
                    (response, audience) -> Utils.runLater(() -> player.showDialog(buildValueDialog(player, attribute))));
        }
        if (present) {
            multi.addButton(Component.text("Xoá toàn bộ", NamedTextColor.RED), null, (response, audience) -> {
                present = false;
                values.clear();
                parent.removeAttributes();
                Utils.runLater(() -> parent.reopen(player));
            });
        }
        multi.columns(3);
        multi.exitButton(Component.text("« Quay lại"), (response, audience) -> Utils.runLater(() -> parent.reopen(player)));

        return multi.build();
    }

    private Dialog buildValueDialog(Player player, EntityAttribute attribute) {
        Double current = values.get(attribute);
        boolean valuePresent = current != null;

        DialogBuilder builder = DialogBuilder.create(Component.text("Attribute: " + attribute.name()))
                .canCloseWithEscape(false)
                .externalTitle(Component.text(attribute.name(), valuePresent ? NamedTextColor.GREEN : NamedTextColor.GRAY))
                .addTextInput("value", Component.text("Giá trị"), b -> b.initial(current == null ? "" : Utils.formatDecimal(current)).maxLength(32));

        DialogTypeBuilder.MultiAction multi = builder.multiAction();
        multi.addButton(Component.text("Lưu"), null, (response, audience) -> {
            Float value = DialogInputUtils.parseFloat(response.getText("value"), -100000f, 100000f);
            if (value != null) {
                values.put(attribute, (double) value);
                commit();
            }
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
        parent.applyAttributes(new AttributeComponent(new EnumMap<>(values)));
    }
}
