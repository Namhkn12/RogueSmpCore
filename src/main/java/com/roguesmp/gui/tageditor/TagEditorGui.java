package com.roguesmp.gui.tageditor;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.registry.Registry;
import com.roguesmp.tag.SmpTag;
import com.roguesmp.utils.Utils;
import com.roguesmp.utils.dialog.DialogBuilder;
import com.roguesmp.utils.dialog.DialogTypeBuilder;
import dev.jorel.commandapi.CommandAPICommand;
import io.papermc.paper.dialog.Dialog;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * In-game editor for {@link SmpTag}s, built on the Paper Dialog API ({@link DialogBuilder}).
 * Operates generically over any tag-capable {@link Registry} (see {@link Registry#getTaggableRegistries()})
 * rather than being wired to one specific registry - item tags, enchant tags, quest tags, etc. all
 * go through the same three screens: registry picker -> tag list (for that registry) -> tag detail
 * (view raw entries, add one, remove one, delete the whole tag).
 * <p>
 * Every add/remove/delete writes straight through to disk via {@link Registry#saveTag} /
 * {@link Registry#deleteTag} and reloads that registry's tags - there's no separate "save" step,
 * matching the immediate-commit shape of the item creator's sub-editors
 * (e.g. {@code com.roguesmp.gui.itemcreator.EnchantEditorGui}).
 */
public final class TagEditorGui {

    private TagEditorGui() {
    }

    public static void open(Player player) {
        DialogBuilder builder = DialogBuilder.create(Component.text("Trình Chỉnh Sửa Tag"))
                .canCloseWithEscape(false)
                .addTextBody(Component.text("Chọn registry để chỉnh sửa tag.", NamedTextColor.GRAY));

        DialogTypeBuilder.MultiAction multi = builder.multiAction();

        List<Registry<?>> registries = new ArrayList<>(Registry.getTaggableRegistries());
        registries.sort(Comparator.comparing(Registry::getLocationKey));

        for (Registry<?> registry : registries) {
            int tagCount = registry.getTags().size();
            multi.addButton(Component.text(registry.getLocationKey(), NamedTextColor.AQUA), Utils.text(tagCount + " tag", NamedTextColor.GRAY),
                    (response, audience) -> Utils.runLater(() -> player.showDialog(buildTagListDialog(player, registry))));
        }

        multi.columns(3);
        multi.exitButton(Component.text("Đóng"), (response, audience) -> audience.closeDialog());

        player.showDialog(multi.build());
    }

    // ==========================================
    // SCREEN 2: TAG LIST FOR A REGISTRY
    // ==========================================

    private static Dialog buildTagListDialog(Player player, Registry<?> registry) {
        DialogBuilder builder = DialogBuilder.create(Component.text("Tag - " + registry.getLocationKey()))
                .canCloseWithEscape(false)
                .externalTitle(Component.text(registry.getLocationKey()))
                .addTextBody(Component.text(registry.getTags().size() + " tag hiện có", NamedTextColor.GRAY));

        DialogTypeBuilder.MultiAction multi = builder.multiAction();

        multi.addButton(Component.text("new_tag", NamedTextColor.GOLD), null,
                (response, audience) -> Utils.runLater(() -> player.showDialog(buildNewTagDialog(player, registry))));

        List<String> tagIds = new ArrayList<>(registry.getTags().keySet());
        Collections.sort(tagIds);

        for (String tagId : tagIds) {
            SmpTag<?> tag = registry.getTag(tagId);
            int size = tag == null ? 0 : tag.getRawEntries().size();
            multi.addButton(Component.text(tagId, NamedTextColor.GREEN), Utils.text(size + " mục", NamedTextColor.GRAY),
                    (response, audience) -> Utils.runLater(() -> player.showDialog(buildTagDetailDialog(player, registry, tagId))));
        }

        multi.columns(3);
        multi.exitButton(Component.text("« Quay lại"), (response, audience) -> Utils.runLater(() -> open(player)));

        return multi.build();
    }

    private static Dialog buildNewTagDialog(Player player, Registry<?> registry) {
        return DialogBuilder.create(Component.text("Tag mới - " + registry.getLocationKey()))
                .canCloseWithEscape(false)
                .addTextInput("value", Component.text("ID tag (chữ thường, dùng _ thay khoảng trắng)"), b -> b.maxLength(64))
                .confirmation()
                .yesButton(Component.text("Tạo"), null, (response, audience) -> {
                    String sanitized = sanitizeId(response.getText("value"));
                    if (sanitized.isEmpty()) {
                        player.sendMessage(Utils.text("ID tag không hợp lệ.", NamedTextColor.RED));
                        Utils.runLater(() -> player.showDialog(buildTagListDialog(player, registry)));
                        return;
                    }

                    if (registry.getTag(sanitized) == null && !registry.saveTag(RogueSmpCore.getInstance(), sanitized, new ArrayList<>())) {
                        player.sendMessage(Utils.text("Lỗi khi tạo tag.", NamedTextColor.RED));
                    }

                    Utils.runLater(() -> player.showDialog(buildTagDetailDialog(player, registry, sanitized)));
                })
                .noButton(Component.text("Huỷ"), null, (response, audience) -> Utils.runLater(() -> player.showDialog(buildTagListDialog(player, registry))))
                .build();
    }

    // ==========================================
    // SCREEN 3: TAG DETAIL (VIEW / ADD / REMOVE / DELETE)
    // ==========================================

    private static Dialog buildTagDetailDialog(Player player, Registry<?> registry, String tagId) {
        List<String> rawEntries = rawEntriesOf(registry, tagId);

        DialogBuilder builder = DialogBuilder.create(Component.text("Tag: " + tagId))
                .canCloseWithEscape(false)
                .externalTitle(Component.text(tagId, NamedTextColor.GREEN))
                .addTextBody(Component.text(rawEntries.isEmpty() ? "Chưa có mục nào" : String.join(", ", rawEntries), NamedTextColor.GRAY));

        DialogTypeBuilder.MultiAction multi = builder.multiAction();

        multi.addButton(Component.text("add_entry", NamedTextColor.GOLD), null,
                (response, audience) -> Utils.runLater(() -> player.showDialog(buildAddEntryDialog(player, registry, tagId))));
        multi.addButton(Component.text("delete_tag", NamedTextColor.RED), null,
                (response, audience) -> Utils.runLater(() -> player.showDialog(buildDeleteTagDialog(player, registry, tagId))));

        for (String entry : rawEntries) {
            multi.addButton(Component.text(entry, NamedTextColor.GRAY), Utils.text("Bấm để xoá mục này", NamedTextColor.GRAY),
                    (response, audience) -> Utils.runLater(() -> player.showDialog(buildRemoveEntryDialog(player, registry, tagId, entry))));
        }

        multi.columns(3);
        multi.exitButton(Component.text("« Quay lại"), (response, audience) -> Utils.runLater(() -> player.showDialog(buildTagListDialog(player, registry))));

        return multi.build();
    }

    private static Dialog buildAddEntryDialog(Player player, Registry<?> registry, String tagId) {
        return DialogBuilder.create(Component.text("Thêm mục vào tag: " + tagId))
                .canCloseWithEscape(false)
                .addTextBody(Component.text("Nhập id (hoặc #tag_khac để tham chiếu tag khác)", NamedTextColor.GRAY))
                .addTextInput("value", Component.text("ID"), b -> b.maxLength(64))
                .confirmation()
                .yesButton(Component.text("Thêm"), null, (response, audience) -> {
                    String raw = response.getText("value");
                    String entry = raw == null ? "" : raw.trim();

                    if (entry.isEmpty()) {
                        player.sendMessage(Utils.text("Mục không hợp lệ.", NamedTextColor.RED));
                    } else {
                        if (!entryLooksValid(registry, entry)) {
                            player.sendMessage(Utils.text("Cảnh báo: Không tìm thấy '" + entry + "' trong registry '" + registry.getLocationKey() + "' (vẫn được thêm).", NamedTextColor.YELLOW));
                        }
                        addEntry(player, registry, tagId, entry);
                    }

                    Utils.runLater(() -> player.showDialog(buildTagDetailDialog(player, registry, tagId)));
                })
                .noButton(Component.text("Huỷ"), null, (response, audience) -> Utils.runLater(() -> player.showDialog(buildTagDetailDialog(player, registry, tagId))))
                .build();
    }

    private static Dialog buildRemoveEntryDialog(Player player, Registry<?> registry, String tagId, String entry) {
        return DialogBuilder.create(Component.text("Xoá mục"))
                .canCloseWithEscape(false)
                .externalTitle(Component.text(entry, NamedTextColor.RED))
                .addTextBody(Component.text("Xoá '" + entry + "' khỏi tag '" + tagId + "'?"))
                .confirmation()
                .yesButton(Component.text("Xoá"), null, (response, audience) -> {
                    removeEntry(player, registry, tagId, entry);
                    Utils.runLater(() -> player.showDialog(buildTagDetailDialog(player, registry, tagId)));
                })
                .noButton(Component.text("Huỷ"), null, (response, audience) -> Utils.runLater(() -> player.showDialog(buildTagDetailDialog(player, registry, tagId))))
                .build();
    }

    private static Dialog buildDeleteTagDialog(Player player, Registry<?> registry, String tagId) {
        return DialogBuilder.create(Component.text("Xoá tag"))
                .canCloseWithEscape(false)
                .externalTitle(Component.text("delete_tag", NamedTextColor.RED))
                .addTextBody(Component.text("Xoá toàn bộ tag '" + tagId + "'? Hành động này không thể hoàn tác."))
                .confirmation()
                .yesButton(Component.text("Xoá"), null, (response, audience) -> {
                    if (!registry.deleteTag(RogueSmpCore.getInstance(), tagId)) {
                        player.sendMessage(Utils.text("Lỗi khi xoá tag.", NamedTextColor.RED));
                    }
                    Utils.runLater(() -> player.showDialog(buildTagListDialog(player, registry)));
                })
                .noButton(Component.text("Huỷ"), null, (response, audience) -> Utils.runLater(() -> player.showDialog(buildTagDetailDialog(player, registry, tagId))))
                .build();
    }

    // ==========================================
    // HELPERS
    // ==========================================

    private static List<String> rawEntriesOf(Registry<?> registry, String tagId) {
        SmpTag<?> tag = registry.getTag(tagId);
        return tag == null ? List.of() : tag.getRawEntries();
    }

    private static void addEntry(Player player, Registry<?> registry, String tagId, String entry) {
        List<String> rawEntries = new ArrayList<>(rawEntriesOf(registry, tagId));
        if (!rawEntries.contains(entry)) rawEntries.add(entry);
        if (!registry.saveTag(RogueSmpCore.getInstance(), tagId, rawEntries)) {
            player.sendMessage(Utils.text("Lỗi khi lưu tag.", NamedTextColor.RED));
        }
    }

    private static void removeEntry(Player player, Registry<?> registry, String tagId, String entry) {
        List<String> rawEntries = new ArrayList<>(rawEntriesOf(registry, tagId));
        rawEntries.remove(entry);
        if (!registry.saveTag(RogueSmpCore.getInstance(), tagId, rawEntries)) {
            player.sendMessage(Utils.text("Lỗi khi lưu tag.", NamedTextColor.RED));
        }
    }

    /**
     * Best-effort validation only - matches the "warn but still allow" shape used for the item
     * creator's head_skin field, since a forward reference (an id/tag added later) is legitimate.
     */
    private static boolean entryLooksValid(Registry<?> registry, String entry) {
        if (entry.startsWith("#")) {
            String nestedId = entry.substring(1).toLowerCase(Locale.ROOT);
            return registry.getTags().containsKey(nestedId);
        }
        return registry.get(entry) != null;
    }

    private static String sanitizeId(@Nullable String raw) {
        if (raw == null) return "";
        return raw.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", "_");
    }

    // ==========================================
    // COMMAND
    // ==========================================

    public static void registerCommand() {
        new CommandAPICommand("smptag")
                .executesPlayer((player, args) -> {
                    open(player);
                })
                .register();
    }
}
