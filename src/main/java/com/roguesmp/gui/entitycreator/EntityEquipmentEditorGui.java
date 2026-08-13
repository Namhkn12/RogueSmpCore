package com.roguesmp.gui.entitycreator;

import com.roguesmp.entity.EntityEquipment;
import com.roguesmp.entity.component.EntityComponentKeys;
import com.roguesmp.entity.component.impl.EquipmentComponent;
import com.roguesmp.registry.SkinRegistry;
import com.roguesmp.utils.Utils;
import com.roguesmp.utils.dialog.DialogBuilder;
import com.roguesmp.utils.dialog.DialogTypeBuilder;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.input.TextDialogInput;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Dialog-based hub for an {@link EquipmentComponent}: one button per {@link EquipmentSlot}, each
 * leading to a per-slot field dialog for {@link EntityEquipment}'s 8 fields. Mirrors
 * {@code gui.itemcreator.ConsumableEditorGui}'s hub-with-leaf-dialogs shape, one level deeper
 * (slot list -> fields) since equipment is keyed per-slot rather than being a single record.
 */
public class EntityEquipmentEditorGui {

    private final EntityCreatorGui parent;
    private final Map<EquipmentSlot, EntityEquipment> slots;
    private boolean present;

    public EntityEquipmentEditorGui(EntityCreatorGui parent, Map<EquipmentSlot, EntityEquipment> initialSlots, boolean present) {
        this.parent = parent;
        this.slots = new EnumMap<>(EquipmentSlot.class);
        this.slots.putAll(initialSlots);
        this.present = present;
    }

    public Dialog buildListDialog(Player player) {
        DialogBuilder builder = DialogBuilder.create(Component.text("Trang bị entity"))
                .canCloseWithEscape(false)
                .externalTitle(Component.text(EntityComponentKeys.EQUIPMENT.id(), present ? NamedTextColor.GREEN : NamedTextColor.GRAY))
                .addTextBody(Component.text(slots.size() + " ô trang bị đã đặt"));

        DialogTypeBuilder.MultiAction multi = builder.multiAction();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            EntityEquipment current = slots.get(slot);
            multi.addButton(Component.text(slot.name(), current != null ? NamedTextColor.GREEN : NamedTextColor.GRAY),
                    current != null ? Component.text(current.getMaterial().name(), NamedTextColor.GRAY) : null,
                    (response, audience) -> Utils.runLater(() -> player.showDialog(buildSlotDialog(player, slot))));
        }
        if (present) {
            multi.addButton(Component.text("Xoá toàn bộ", NamedTextColor.RED), null, (response, audience) -> {
                present = false;
                slots.clear();
                parent.removeEquipment();
                Utils.runLater(() -> parent.reopen(player));
            });
        }
        multi.columns(3);
        multi.exitButton(Component.text("« Quay lại"), (response, audience) -> Utils.runLater(() -> parent.reopen(player)));

        return multi.build();
    }

    private Dialog buildSlotDialog(Player player, EquipmentSlot slot) {
        EntityEquipment current = slots.get(slot);
        boolean slotPresent = current != null;

        DialogBuilder builder = DialogBuilder.create(Component.text("Trang bị: " + slot.name()))
                .canCloseWithEscape(false)
                .externalTitle(Component.text(slot.name(), slotPresent ? NamedTextColor.GREEN : NamedTextColor.GRAY))
                .addTextInput("material", Component.text("Material (bắt buộc)"), b -> b.initial(current == null ? "" : current.getMaterial().name()).maxLength(64))
                .addTextInput("display_name", Component.text("Tên hiển thị (MiniMessage, để trống nếu không có)"),
                        b -> b.initial(current == null || current.getDisplayName() == null ? "" : current.getDisplayName()).maxLength(256))
                .addTextInput("lore", Component.text("Lore (mỗi dòng 1 dòng lore, để trống nếu không có)"),
                        b -> b.initial(current == null || current.getLore() == null ? "" : String.join("\n", current.getLore()))
                                .maxLength(1000).multiline(TextDialogInput.MultilineOptions.create(5, 100)))
                .addCheckboxInput("enchant_glint", Component.text("Hiệu ứng phù phép (glint)"), current != null && current.isEnchantGlint(), "true", "false")
                .addTextInput("trim_material", Component.text("Trim material (vd: redstone, để trống nếu không có)"),
                        b -> b.initial(current == null || current.getTrimMaterial() == null ? "" : current.getTrimMaterial()).maxLength(64))
                .addTextInput("trim_pattern", Component.text("Trim pattern (vd: sentry, để trống nếu không có)"),
                        b -> b.initial(current == null || current.getTrimPattern() == null ? "" : current.getTrimPattern()).maxLength(64))
                .addTextInput("dye_color", Component.text("Dye color (a,r,g,b - để trống nếu không có)"),
                        b -> b.initial(current == null || current.getDyeColor() == null ? "" : current.getDyeColor()).maxLength(32))
                .addTextInput("head_skin", Component.text("Head skin ID (trong SkinRegistry, để trống nếu không có)"),
                        b -> b.initial(current == null || current.getHeadSkin() == null ? "" : current.getHeadSkin()).maxLength(64));

        DialogTypeBuilder.MultiAction multi = builder.multiAction();
        multi.addButton(Component.text("Lưu"), null, (response, audience) -> {
            String materialRaw = response.getText("material");
            Material material = materialRaw == null ? null : Material.matchMaterial(materialRaw.trim());
            if (material == null) {
                player.sendMessage(Utils.text("Material không hợp lệ.", NamedTextColor.RED));
                Utils.runLater(() -> player.showDialog(buildSlotDialog(player, slot)));
                return;
            }

            String displayName = blankToNull(response.getText("display_name"));
            String loreRaw = response.getText("lore");
            List<String> lore = (loreRaw == null || loreRaw.isBlank()) ? null : new ArrayList<>(Arrays.asList(loreRaw.split("\n", -1)));
            Boolean enchantGlint = response.getBoolean("enchant_glint");
            String trimMaterial = blankToNull(response.getText("trim_material"));
            String trimPattern = blankToNull(response.getText("trim_pattern"));
            String dyeColor = blankToNull(response.getText("dye_color"));
            String headSkin = blankToNull(response.getText("head_skin"));

            if (headSkin != null && SkinRegistry.getInstance().getSkin(headSkin) == null) {
                player.sendMessage(Utils.text("Cảnh báo: Không tìm thấy skin '" + headSkin + "' (vẫn được lưu).", NamedTextColor.YELLOW));
            }

            slots.put(slot, new EntityEquipment(material, displayName, lore, enchantGlint != null && enchantGlint, trimMaterial, trimPattern, dyeColor, headSkin));
            commit();
            Utils.runLater(() -> player.showDialog(buildListDialog(player)));
        });
        if (slotPresent) {
            multi.addButton(Component.text("Xoá"), null, (response, audience) -> {
                slots.remove(slot);
                commit();
                Utils.runLater(() -> player.showDialog(buildListDialog(player)));
            });
        }
        multi.columns(slotPresent ? 3 : 2);
        multi.exitButton(Component.text("Huỷ"), (response, audience) -> Utils.runLater(() -> player.showDialog(buildListDialog(player))));

        return multi.build();
    }

    private void commit() {
        present = true;
        parent.applyEquipment(new EquipmentComponent(new EnumMap<>(slots)));
    }

    private static @Nullable String blankToNull(@Nullable String raw) {
        return (raw == null || raw.isBlank()) ? null : raw.trim();
    }
}
