package com.roguesmp.gui.loottablecreator;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.loot.LootPool;
import com.roguesmp.loot.LootTable;
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
import java.util.List;
import java.util.Locale;

/**
 * In-game editor for {@link LootTable}s. The main dialog handles the id, save and delete; each pool
 * is edited as lenient JSON through {@link LootPool#CODEC}, so entries, conditions and functions
 * all round-trip without loss.
 */
public class LootTableGuiCreator {

    private @Nullable String id;
    private final List<LootPool> pools;

    public LootTableGuiCreator() {
        this(null, new ArrayList<>());
    }

    private LootTableGuiCreator(@Nullable String id, List<LootPool> pools) {
        this.id = id;
        this.pools = pools;
    }

    public static LootTableGuiCreator editExisting(String id, LootTable table) {
        return new LootTableGuiCreator(id, new ArrayList<>(table.getPools()));
    }

    public void openMainDialog(Player player) {
        DialogBuilder builder = DialogBuilder.create(Component.text("Trình Tạo Loot Table"))
                .canCloseWithEscape(false)
                .addTextBody(Component.text("ID: " + (id == null ? "(chưa đặt)" : id)))
                .addTextBody(Component.text(pools.size() + " pool"));

        DialogTypeBuilder.MultiAction multi = builder.multiAction();

        multi.addButton(Component.text("id", id == null ? NamedTextColor.RED : NamedTextColor.GREEN), null,
                (response, audience) -> Utils.runLater(() -> player.showDialog(buildIdDialog(player))));

        for (int i = 0; i < pools.size(); i++) {
            int index = i;
            multi.addButton(Component.text("pool[" + index + "]", NamedTextColor.AQUA), describePool(pools.get(index)),
                    (response, audience) -> Utils.runLater(() -> player.showDialog(buildPoolDialog(player, index).build(player, pools.get(index)))));
        }

        multi.addButton(Component.text("+ Thêm pool", NamedTextColor.GREEN), null,
                (response, audience) -> Utils.runLater(() -> player.showDialog(buildPoolDialog(player, -1).build(player, new LootPool(1, List.of(), List.of())))));

        multi.addButton(Component.text("save", NamedTextColor.GOLD), null,
                (response, audience) -> Utils.runLater(() -> player.showDialog(buildSaveDialog(player))));

        multi.addButton(Component.text("delete", NamedTextColor.RED), null,
                (response, audience) -> audience.showDialog(buildDeleteDialog()));
        multi.columns(3);
        multi.exitButton(Component.text("Đóng"), (response, audience) -> audience.closeDialog());

        player.showDialog(multi.build());
    }

    private JsonEntryDialog<LootPool> buildPoolDialog(Player player, int index) {
        boolean editing = index >= 0;
        return JsonEntryDialog.of(editing ? "Pool[" + index + "]" : "Pool mới", LootPool.CODEC,
                pool -> {
                    if (editing) pools.set(index, pool);
                    else pools.add(pool);
                    openMainDialog(player);
                },
                editing ? () -> {
                    pools.remove(index);
                    openMainDialog(player);
                } : null,
                () -> openMainDialog(player));
    }

    private static Component describePool(LootPool pool) {
        return Component.text("rolls=" + pool.getRolls() + ", " + pool.getEntries().size() + " entrie(s), "
                + pool.getConditions().size() + " condition(s)", NamedTextColor.GRAY);
    }

    private Dialog buildDeleteDialog() {
        return DialogBuilder.create(Component.text("Xác nhận xóa?"))
                .addTextBody(Component.text("Xác nhận xóa? Hành động này không thể hoàn tác."))
                .confirmation()
                .yesButton(Component.text("Vẫn xóa"), null, (response, audience) -> {
                    Registries.LOOT_TABLE.removeAndDeleteFiles(RogueSmpCore.getInstance(), id);
                    audience.sendMessage(Component.text("Deleted entry: " + id, NamedTextColor.RED));
                })
                .noButton(Component.text("Thôi, không xóa nữa"), null, (response, audience) -> openMainDialog((Player) audience))
                .build();
    }

    private Dialog buildIdDialog(Player player) {
        return DialogBuilder.create(Component.text("Đặt ID loot table"))
                .canCloseWithEscape(false)
                .externalTitle(Component.text("id", id == null ? NamedTextColor.RED : NamedTextColor.GREEN))
                .addTextInput("value", Component.text("ID = đường dẫn tương đối, vd dungeons/goblin_drops"), b -> b.initial(id == null ? "" : id).maxLength(128))
                .confirmation()
                .yesButton(Component.text("Xác nhận"), null, (response, audience) -> {
                    String sanitized = sanitizeId(response.getText("value"));
                    if (sanitized.isEmpty()) player.sendMessage(Utils.text("ID không hợp lệ.", NamedTextColor.RED));
                    else this.id = sanitized;
                    Utils.runLater(() -> openMainDialog(player));
                })
                .noButton(Component.text("Huỷ"), null, (response, audience) -> Utils.runLater(() -> openMainDialog(player)))
                .build();
    }

    private static String sanitizeId(@Nullable String raw) {
        if (raw == null) return "";
        return raw.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", "_");
    }

    private Dialog buildSaveDialog(Player player) {
        return DialogBuilder.create(Component.text("Lưu loot table"))
                .canCloseWithEscape(false)
                .externalTitle(Component.text("save", NamedTextColor.GOLD))
                .addTextBody(Component.text("ID: " + (id == null ? "(chưa đặt)" : id)))
                .addTextBody(Component.text(pools.size() + " pool" + (pools.isEmpty() ? " (bảng sẽ không roll ra gì cả)" : "")))
                .confirmation()
                .yesButton(Component.text("Lưu"), null, (response, audience) -> Utils.runLater(() -> attemptSave(player)))
                .noButton(Component.text("Huỷ"), null, (response, audience) -> Utils.runLater(() -> openMainDialog(player)))
                .build();
    }

    private void attemptSave(Player player) {
        if (id == null || id.isBlank()) {
            player.sendMessage(Utils.text("Bạn cần đặt ID trước khi lưu.", NamedTextColor.RED));
            openMainDialog(player);
            return;
        }

        if (Registries.LOOT_TABLE.get(id) != null) {
            player.showDialog(DialogBuilder.create(Component.text("Ghi đè loot table?"))
                    .canCloseWithEscape(false)
                    .addTextBody(Component.text("Đã tồn tại một loot table với ID '" + id + "'. Ghi đè?"))
                    .confirmation()
                    .yesButton(Component.text("Ghi đè"), null, (response, audience) -> doSave(player))
                    .noButton(Component.text("Huỷ"), null, (response, audience) -> Utils.runLater(() -> openMainDialog(player)))
                    .build());
            return;
        }

        doSave(player);
    }

    private void doSave(Player player) {
        Registries.LOOT_TABLE.registerAndSave(RogueSmpCore.getInstance(), id, new LootTable(List.copyOf(pools)));
        player.sendMessage(Utils.text("Đã lưu loot table '" + id + "'.", NamedTextColor.GREEN));
        Utils.runLater(() -> openMainDialog(player));
    }
}
