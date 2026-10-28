package com.roguesmp.gui.entitycreator;

import com.roguesmp.entity.component.EntityComponentKeys;
import com.roguesmp.entity.component.impl.SpellComponent;
import com.roguesmp.entity.spell.SpellConfig;
import com.roguesmp.registry.Registries;
import com.roguesmp.utils.Utils;
import com.roguesmp.utils.dialog.DialogBuilder;
import com.roguesmp.utils.dialog.DialogTypeBuilder;
import com.roguesmp.utils.dialog.JsonEntryDialog;
import com.roguesmp.utils.dialog.LenientJson;
import io.papermc.paper.dialog.Dialog;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class EntitySpellListEditorGui {

    private final EntityCreatorGui parent;
    private boolean present;
    private final List<SpellConfig> spells;
    private boolean canCastSameSpellTwice;

    public EntitySpellListEditorGui(EntityCreatorGui parent, @Nullable SpellComponent current) {
        this.parent = parent;
        this.present = current != null;
        this.spells = new ArrayList<>(current == null ? List.of() : current.getSpellConfigs());
        this.canCastSameSpellTwice = current != null && current.isCanCastSameSpellTwice();
    }

    public Dialog buildHubDialog(Player player) {
        DialogBuilder builder = DialogBuilder.create(Component.text("Spell entity"))
                .canCloseWithEscape(false)
                .externalTitle(Component.text(EntityComponentKeys.SPELLS.id(), present ? NamedTextColor.GREEN : NamedTextColor.GRAY))
                .addTextBody(Component.text(spells.size() + " spell  |  Cast trùng spell: " + (canCastSameSpellTwice ? "Bật" : "Tắt")));

        DialogTypeBuilder.MultiAction multi = builder.multiAction();
        multi.addButton(Component.text("spells", NamedTextColor.GREEN), null,
                (response, audience) -> Utils.runLater(() -> player.showDialog(buildListDialog(player))));
        multi.addButton(Component.text("settings"), null,
                (response, audience) -> Utils.runLater(() -> player.showDialog(buildSettingsDialog(player))));
        if (present) {
            multi.addButton(Component.text("Xoá toàn bộ", NamedTextColor.RED), null, (response, audience) -> {
                present = false;
                spells.clear();
                parent.removeSpells();
                Utils.runLater(() -> parent.reopen(player));
            });
        }
        multi.columns(3);
        multi.exitButton(Component.text("« Quay lại"), (response, audience) -> Utils.runLater(() -> parent.reopen(player)));

        return multi.build();
    }

    private Dialog buildSettingsDialog(Player player) {
        DialogBuilder builder = DialogBuilder.create(Component.text("Cài đặt spell"))
                .canCloseWithEscape(false)
                .addCheckboxInput("can_cast_same_twice", Component.text("Cho phép cast trùng spell 2 lần liên tiếp"), canCastSameSpellTwice, "true", "false");

        DialogTypeBuilder.MultiAction multi = builder.multiAction();
        multi.addButton(Component.text("Lưu"), null, (response, audience) -> {
            Boolean canCastTwice = response.getBoolean("can_cast_same_twice");
            if (canCastTwice != null) canCastSameSpellTwice = canCastTwice;
            commit();
            Utils.runLater(() -> player.showDialog(buildHubDialog(player)));
        });
        multi.columns(2);
        multi.exitButton(Component.text("Huỷ"), (response, audience) -> Utils.runLater(() -> player.showDialog(buildHubDialog(player))));

        return multi.build();
    }

    private Dialog buildListDialog(Player player) {
        DialogBuilder builder = DialogBuilder.create(Component.text("Spell"))
                .canCloseWithEscape(false)
                .externalTitle(Component.text("spells", spells.isEmpty() ? NamedTextColor.GRAY : NamedTextColor.GREEN))
                .addTextBody(Component.text(spells.size() + " spell"));

        DialogTypeBuilder.MultiAction multi = builder.multiAction();
        multi.addButton(Component.text("+ Thêm spell", NamedTextColor.GREEN), null,
                (response, audience) -> Utils.runLater(() -> player.showDialog(entryDialog(player, -1).buildWithText(player, "type: "))));
        for (int i = 0; i < spells.size(); i++) {
            int index = i;
            SpellConfig config = spells.get(i);
            multi.addButton(Component.text((index + 1) + ". " + config.type().id()), null,
                    (response, audience) -> Utils.runLater(() -> player.showDialog(entryDialog(player, index).build(player, spells.get(index)))));
        }
        multi.columns(2);
        multi.exitButton(Component.text("« Quay lại"), (response, audience) -> Utils.runLater(() -> player.showDialog(buildHubDialog(player))));

        return multi.build();
    }

    private JsonEntryDialog<SpellConfig> entryDialog(Player player, int index) {
        boolean editing = index >= 0;
        return JsonEntryDialog.of(editing ? "Spell: " + spells.get(index).type().id() : "Spell mới", SpellConfig.CODEC,
                value -> {
                    if (editing) spells.set(index, value);
                    else spells.add(value);
                    commit();
                    player.showDialog(buildListDialog(player));
                },
                editing ? () -> {
                    spells.remove(index);
                    commit();
                    player.showDialog(buildListDialog(player));
                } : null,
                () -> player.showDialog(buildListDialog(player)))
                .withHelp(List.of(LenientJson.typesHelp(Registries.ENTITY_SPELL.getAll().keySet())));
    }

    private void commit() {
        present = true;
        parent.applySpells(new SpellComponent(spells, canCastSameSpellTwice));
    }

}
