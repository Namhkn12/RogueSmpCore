package com.roguesmp.gui.itemcreator;

import com.roguesmp.enchant.Enchants;
import com.roguesmp.item.component.ItemComponentKeys;
import com.roguesmp.item.component.impl.EnchantComponent;
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
 * Dialog-based sub-editor of {@link ItemCreatorGui} for a single {@link EnchantComponent}'s
 * (enchant -> level) map. A {@code multiAction()} with one button per {@link Enchants} constant
 * ({@link com.roguesmp.enchant.SmpEnchant#getSimpleDescription()} as the hover tooltip). Same
 * immediate-commit shape as {@link AttributeEditorGui}.
 * <p>
 * An <em>empty</em> {@code EnchantComponent} is meaningful on its own - per the item docs, it
 * marks the item as "can be enchanted" even with zero base enchants. So an empty map does not
 * imply "not present": presence is tracked separately via {@link #present}, and clearing every
 * individual enchant back to level 0 leaves the (now empty) component in place rather than
 * deleting it. Deleting it entirely is its own explicit action ("remove").
 */
public class EnchantEditorGui {

    private final ItemCreatorGui parent;
    private final Map<Enchants, Integer> values;
    private boolean present;

    public EnchantEditorGui(ItemCreatorGui parent, Map<Enchants, Integer> initialValues, boolean present) {
        this.parent = parent;
        this.values = new EnumMap<>(Enchants.class);
        this.values.putAll(initialValues);
        this.present = present;
    }

    public Dialog buildListDialog(Player player) {
        DialogBuilder builder = DialogBuilder.create(Component.text("Phù phép"))
                .canCloseWithEscape(false)
                .externalTitle(Component.text(ItemComponentKeys.ENCHANT.id(), present ? NamedTextColor.GREEN : NamedTextColor.GRAY))
                .addTextBody(Component.text(present
                        ? "Vật phẩm có thể enchant (" + values.size() + " phù phép cơ bản)"
                        : "Chưa đánh dấu có thể enchant"));

        DialogTypeBuilder.MultiAction multi = builder.multiAction();

        boolean hasComponent = present;
        multi.addButton(hasComponent ? Component.text("remove", NamedTextColor.RED) : Component.text("mark_enchantable"), null,
                (response, audience) -> Utils.runLater(() -> player.showDialog(hasComponent ? buildRemoveDialog(player) : buildMarkEnchantableDialog(player))));

        for (Enchants enchant : Enchants.values()) {
            int current = values.getOrDefault(enchant, 0);
            boolean valuePresent = current > 0;
            multi.addButton(Component.text(enchant.name(), valuePresent ? NamedTextColor.GREEN : NamedTextColor.GRAY),
                    Utils.text(enchant.getEnchant().getSimpleDescription(), NamedTextColor.GRAY),
                    (response, audience) -> Utils.runLater(() -> player.showDialog(buildValueDialog(player, enchant))));
        }

        multi.columns(3);
        multi.exitButton(Component.text("« Quay lại"), (response, audience) -> Utils.runLater(() -> parent.reopen(player)));

        return multi.build();
    }

    private Dialog buildMarkEnchantableDialog(Player player) {
        return DialogBuilder.create(Component.text("Đánh dấu có thể enchant"))
                .canCloseWithEscape(false)
                .externalTitle(Component.text("mark_enchantable"))
                .addTextBody(Component.text("Đánh dấu vật phẩm này là có thể enchant, kể cả khi chưa có phù phép cơ bản nào?"))
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
        return DialogBuilder.create(Component.text("Bỏ đánh dấu có thể enchant"))
                .canCloseWithEscape(false)
                .externalTitle(Component.text("remove", NamedTextColor.RED))
                .addTextBody(Component.text("Xoá toàn bộ dữ liệu enchant (bỏ đánh dấu có thể enchant) của vật phẩm này?"))
                .confirmation()
                .yesButton(Component.text("Xoá"), null, (response, audience) -> {
                    present = false;
                    values.clear();
                    parent.removeEnchant();
                    Utils.runLater(() -> parent.reopen(player));
                })
                .noButton(Component.text("Huỷ"), null, (response, audience) -> Utils.runLater(() -> player.showDialog(buildListDialog(player))))
                .build();
    }

    private Dialog buildValueDialog(Player player, Enchants enchant) {
        int current = values.getOrDefault(enchant, 0);
        boolean valuePresent = current > 0;

        DialogBuilder builder = DialogBuilder.create(Component.text("Phù phép: " + enchant.name()))
                .canCloseWithEscape(false)
                .externalTitle(Component.text(enchant.name(), valuePresent ? NamedTextColor.GREEN : NamedTextColor.GRAY))
                .addTextInput("value", Component.text("Cấp độ (>= 0)"), b -> b.initial(String.valueOf(current)).maxLength(16));

        DialogTypeBuilder.MultiAction multi = builder.multiAction();
        multi.addButton(Component.text("Lưu"), null, (response, audience) -> {
            Float value = DialogInputUtils.parseFloat(response.getText("value"), 0f, Float.MAX_VALUE);
            if (value != null) {
                int level = Math.round(value);
                if (level <= 0) values.remove(enchant);
                else values.put(enchant, level);
            }
            commit();
            Utils.runLater(() -> player.showDialog(buildListDialog(player)));
        });
        if (valuePresent) {
            multi.addButton(Component.text("Xoá"), null, (response, audience) -> {
                values.remove(enchant);
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
        parent.applyEnchant(new EnchantComponent(new EnumMap<>(values)));
    }
}
