package com.roguesmp.gui.equipment;

import com.roguesmp.codec.Codec;
import com.roguesmp.codec.DataResult;
import com.roguesmp.codec.JsonOps;
import com.roguesmp.entity.EntityEquipment;
import com.roguesmp.registry.SkinRegistry;
import com.roguesmp.utils.Utils;
import com.roguesmp.utils.dialog.DialogBuilder;
import com.roguesmp.utils.dialog.DialogTypeBuilder;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.data.dialog.input.TextDialogInput;
import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class EquipmentEditorGui {

    private final String title;
    private final Map<EquipmentSlot, EntityEquipment> slots;
    private final Consumer<Map<EquipmentSlot, EntityEquipment>> onChange;
    private final Consumer<Player> onBack;

    public EquipmentEditorGui(String title, Map<EquipmentSlot, EntityEquipment> initialSlots,
                              Consumer<Map<EquipmentSlot, EntityEquipment>> onChange, Consumer<Player> onBack) {
        this.title = title;
        this.slots = new EnumMap<>(EquipmentSlot.class);
        this.slots.putAll(initialSlots);
        this.onChange = onChange;
        this.onBack = onBack;
    }

    public Dialog buildListDialog(Player player) {
        DialogBuilder builder = DialogBuilder.create(Component.text(title))
                .canCloseWithEscape(false)
                .externalTitle(Component.text("equipment", slots.isEmpty() ? NamedTextColor.GRAY : NamedTextColor.GREEN))
                .addTextBody(Component.text(slots.size() + " ô trang bị đã đặt"));

        DialogTypeBuilder.MultiAction multi = builder.multiAction();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            EntityEquipment current = slots.get(slot);
            multi.addButton(Component.text(slot.name(), current != null ? NamedTextColor.GREEN : NamedTextColor.GRAY),
                    current != null ? Component.text(current.getMaterial().name(), NamedTextColor.GRAY) : null,
                    (response, audience) -> Utils.runLater(() -> player.showDialog(buildSlotDialog(player, slot))));
        }
        if (!slots.isEmpty()) {
            multi.addButton(Component.text("Xoá toàn bộ", NamedTextColor.RED), null, (response, audience) -> {
                slots.clear();
                commit();
                Utils.runLater(() -> onBack.accept(player));
            });
        }
        multi.columns(3);
        multi.exitButton(Component.text("« Quay lại"), (response, audience) -> Utils.runLater(() -> onBack.accept(player)));

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
                .addTextInput("trim_material", Component.text("Trim material (vd: minecraft:redstone, để trống nếu không có)"),
                        b -> b.initial(current == null || current.getTrimMaterial() == null ? "" : current.getTrimMaterial().asString()).maxLength(64))
                .addTextInput("trim_pattern", Component.text("Trim pattern (vd: minecraft:sentry, để trống nếu không có)"),
                        b -> b.initial(current == null || current.getTrimPattern() == null ? "" : current.getTrimPattern().asString()).maxLength(64))
                .addTextInput("dye_color", Component.text("Dye color (hex, vd: #9B5DE5 - để trống nếu không có)"),
                        b -> b.initial(current == null || current.getDyeColor() == null ? "" : String.format("#%06X", current.getDyeColor().asRGB())).maxLength(32))
                .addTextInput("head_skin", Component.text("Head skin ID (trong SkinRegistry, để trống nếu không có)"),
                        b -> b.initial(current == null || current.getHeadSkin() == null ? "" : current.getHeadSkin()).maxLength(64));

        DialogTypeBuilder.MultiAction multi = builder.multiAction();
        multi.addButton(Component.text("Lưu"), null, (response, audience) -> {
            String materialRaw = response.getText("material");
            Material material = materialRaw == null ? null : Material.matchMaterial(materialRaw.trim());
            if (material == null) {
                retry(player, slot, "Material không hợp lệ.");
                return;
            }

            String displayName = blankToNull(response.getText("display_name"));
            String loreRaw = response.getText("lore");
            List<String> lore = (loreRaw == null || loreRaw.isBlank()) ? null : new ArrayList<>(Arrays.asList(loreRaw.split("\n", -1)));
            Boolean enchantGlint = response.getBoolean("enchant_glint");

            Key trimMaterial;
            Key trimPattern;
            try {
                trimMaterial = parseKey(response.getText("trim_material"));
                trimPattern = parseKey(response.getText("trim_pattern"));
            } catch (InvalidKeyException e) {
                retry(player, slot, "Trim key không hợp lệ, dùng dạng namespace:id.");
                return;
            }
            if ((trimMaterial == null) != (trimPattern == null)) {
                retry(player, slot, "Cần điền cả trim material và trim pattern (hoặc để trống cả hai).");
                return;
            }
            if (trimMaterial != null && RegistryAccess.registryAccess().getRegistry(RegistryKey.TRIM_MATERIAL).get(trimMaterial) == null) {
                retry(player, slot, "Không tìm thấy trim material '" + trimMaterial.asString() + "'.");
                return;
            }
            if (trimPattern != null && RegistryAccess.registryAccess().getRegistry(RegistryKey.TRIM_PATTERN).get(trimPattern) == null) {
                retry(player, slot, "Không tìm thấy trim pattern '" + trimPattern.asString() + "'.");
                return;
            }

            String dyeColorRaw = blankToNull(response.getText("dye_color"));
            DataResult<Color> dyeResult = dyeColorRaw == null ? null : Codec.HEX_COLOR.decode(JsonOps.INSTANCE.createString(dyeColorRaw), JsonOps.INSTANCE);
            if (dyeResult != null && !dyeResult.isSuccess()) {
                retry(player, slot, "Màu nhuộm không hợp lệ, dùng dạng #RRGGBB.");
                return;
            }
            Color dyeColor = dyeResult == null ? null : dyeResult.result();
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

    private void retry(Player player, EquipmentSlot slot, String message) {
        player.sendMessage(Utils.text(message, NamedTextColor.RED));
        Utils.runLater(() -> player.showDialog(buildSlotDialog(player, slot)));
    }

    private void commit() {
        onChange.accept(new EnumMap<>(slots));
    }

    private static @Nullable Key parseKey(@Nullable String raw) {
        String text = blankToNull(raw);
        return text == null ? null : Key.key(text.toLowerCase());
    }

    private static @Nullable String blankToNull(@Nullable String raw) {
        return (raw == null || raw.isBlank()) ? null : raw.trim();
    }
}
