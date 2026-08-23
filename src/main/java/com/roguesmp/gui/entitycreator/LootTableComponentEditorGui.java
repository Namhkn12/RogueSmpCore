package com.roguesmp.gui.entitycreator;

import com.roguesmp.entity.component.EntityComponentKeys;
import com.roguesmp.entity.component.impl.LootTableComponent;
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
import java.util.List;

/**
 * Dialog-based hub for a {@link LootTableComponent}: a flat list of loot table ids, each rolled
 * independently on death. Mirrors {@link EntitySpellListEditorGui}'s hub/list/add/entry shape,
 * simplified since entries here are bare ids with no sub-fields - "editing" one is just removing
 * it and adding the id you actually want.
 */
public class LootTableComponentEditorGui {

    private final EntityCreatorGui parent;
    private boolean present;
    private final List<String> lootTableIds;

    public LootTableComponentEditorGui(EntityCreatorGui parent, @Nullable LootTableComponent current) {
        this.parent = parent;
        this.present = current != null;
        this.lootTableIds = new ArrayList<>(current == null ? List.of() : current.lootTableIds());
    }

    public Dialog buildHubDialog(Player player) {
        DialogBuilder builder = DialogBuilder.create(Component.text("Loot table"))
                .canCloseWithEscape(false)
                .externalTitle(Component.text(EntityComponentKeys.LOOT_TABLE.id(), present ? NamedTextColor.GREEN : NamedTextColor.GRAY))
                .addTextBody(Component.text(lootTableIds.size() + " loot table"));

        DialogTypeBuilder.MultiAction multi = builder.multiAction();
        multi.addButton(Component.text("+ Thêm loot table", NamedTextColor.GREEN), null,
                (response, audience) -> Utils.runLater(() -> player.showDialog(buildAddDialog(player))));

        for (int i = 0; i < lootTableIds.size(); i++) {
            int index = i;
            String tableId = lootTableIds.get(i);
            boolean exists = Registries.LOOT_TABLE.get(tableId) != null;
            multi.addButton(Component.text((index + 1) + ". " + tableId, exists ? NamedTextColor.WHITE : NamedTextColor.RED),
                    exists ? null : Component.text("Không tìm thấy loot table này (chưa tạo hoặc chưa load)", NamedTextColor.RED),
                    (response, audience) -> Utils.runLater(() -> player.showDialog(buildEntryDialog(player, index))));
        }

        if (present) {
            multi.addButton(Component.text("Xoá toàn bộ", NamedTextColor.RED), null, (response, audience) -> {
                present = false;
                lootTableIds.clear();
                parent.removeLootTable();
                Utils.runLater(() -> parent.reopen(player));
            });
        }

        multi.columns(2);
        multi.exitButton(Component.text("« Quay lại"), (response, audience) -> Utils.runLater(() -> parent.reopen(player)));

        return multi.build();
    }

    private Dialog buildAddDialog(Player player) {
        DialogBuilder builder = DialogBuilder.create(Component.text("Thêm loot table"))
                .canCloseWithEscape(false)
                .addTextInput("value", Component.text("Loot table id (vd: dungeons/goblin_drops)"), b -> b.initial("").maxLength(128));

        DialogTypeBuilder.MultiAction multi = builder.multiAction();
        multi.addButton(Component.text("Thêm"), null, (response, audience) -> {
            String value = response.getText("value");
            if (value != null && !value.isBlank()) {
                lootTableIds.add(value.trim());
                commit();
            }
            Utils.runLater(() -> player.showDialog(buildHubDialog(player)));
        });
        multi.columns(2);
        multi.exitButton(Component.text("Huỷ"), (response, audience) -> Utils.runLater(() -> player.showDialog(buildHubDialog(player))));

        return multi.build();
    }

    private Dialog buildEntryDialog(Player player, int index) {
        String tableId = lootTableIds.get(index);
        boolean exists = Registries.LOOT_TABLE.get(tableId) != null;

        DialogBuilder builder = DialogBuilder.create(Component.text("Loot table: " + tableId))
                .canCloseWithEscape(false)
                .addTextBody(Component.text(exists ? "Tồn tại trong Registries.LOOT_TABLE" : "Không tìm thấy trong Registries.LOOT_TABLE",
                        exists ? NamedTextColor.GREEN : NamedTextColor.RED));

        DialogTypeBuilder.MultiAction multi = builder.multiAction();
        multi.addButton(Component.text("Xoá", NamedTextColor.RED), null, (response, audience) -> {
            lootTableIds.remove(index);
            commit();
            Utils.runLater(() -> player.showDialog(buildHubDialog(player)));
        });
        multi.columns(2);
        multi.exitButton(Component.text("« Quay lại"), (response, audience) -> Utils.runLater(() -> player.showDialog(buildHubDialog(player))));

        return multi.build();
    }

    private void commit() {
        present = true;
        parent.applyLootTable(new LootTableComponent(new ArrayList<>(lootTableIds)));
    }
}
