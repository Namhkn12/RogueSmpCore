package com.roguesmp.gui.entitycreator;

import com.roguesmp.codec.DataResult;
import com.roguesmp.codec.JsonOps;
import com.roguesmp.entity.component.EntityComponentKeys;
import com.roguesmp.entity.component.impl.SpellComponent;
import com.roguesmp.entity.spell.SpellParams;
import com.roguesmp.entity.spell.SpellType;
import com.roguesmp.entity.spell.SpellUsage;
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
import java.util.Map;
import java.util.TreeMap;

/**
 * Dialog-based hub for a {@link SpellComponent}: separate active/passive spell lists, each entry
 * added by picking a registered {@link SpellType} id from {@link Registries#ENTITY_SPELL} and
 * decoding its default {@link SpellParams} (an empty JSON object through the type's own codec -
 * every registered spell's params are either zero-field or fully optional-with-defaults today, so
 * this always succeeds). Per-field param editing is out of scope here - remove and re-add to pick
 * a different type. The {@link SpellType#usage()} this project just added is used to color-code
 * the picker (green = matches this list, red = declared for the other list) as a warning only,
 * never a hard block. Plus the {@code passiveInterval}/{@code canCastSameSpellTwice} scalars.
 */
public class EntitySpellListEditorGui {

    private final EntityCreatorGui parent;
    private boolean present;
    private final List<SpellParams> activeSpells;
    private final List<SpellParams> passiveSpells;
    private int passiveInterval;
    private boolean canCastSameSpellTwice;

    public EntitySpellListEditorGui(EntityCreatorGui parent, @Nullable SpellComponent current) {
        this.parent = parent;
        this.present = current != null;
        this.activeSpells = new ArrayList<>(current == null ? List.of() : current.getActiveSpellParams());
        this.passiveSpells = new ArrayList<>(current == null ? List.of() : current.getPassiveSpellParams());
        this.passiveInterval = current == null ? 0 : current.getPassiveIntervalConfig();
        this.canCastSameSpellTwice = current != null && current.isCanCastSameSpellTwiceConfig();
    }

    public Dialog buildHubDialog(Player player) {
        DialogBuilder builder = DialogBuilder.create(Component.text("Spell entity"))
                .canCloseWithEscape(false)
                .externalTitle(Component.text(EntityComponentKeys.SPELLS.id(), present ? NamedTextColor.GREEN : NamedTextColor.GRAY))
                .addTextBody(Component.text("Active: " + activeSpells.size() + "  |  Passive: " + passiveSpells.size()))
                .addTextBody(Component.text("Passive interval: " + passiveInterval + " tick  |  Cast trùng spell: " + (canCastSameSpellTwice ? "Bật" : "Tắt")));

        DialogTypeBuilder.MultiAction multi = builder.multiAction();
        multi.addButton(Component.text("active_spell", NamedTextColor.GREEN), null,
                (response, audience) -> Utils.runLater(() -> player.showDialog(buildListDialog(player, true))));
        multi.addButton(Component.text("passive_spell", NamedTextColor.AQUA), null,
                (response, audience) -> Utils.runLater(() -> player.showDialog(buildListDialog(player, false))));
        multi.addButton(Component.text("settings"), null,
                (response, audience) -> Utils.runLater(() -> player.showDialog(buildSettingsDialog(player))));
        if (present) {
            multi.addButton(Component.text("Xoá toàn bộ", NamedTextColor.RED), null, (response, audience) -> {
                present = false;
                activeSpells.clear();
                passiveSpells.clear();
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
                .addTextInput("passive_interval", Component.text("Passive interval (tick, 0 = mặc định)"), b -> b.initial(String.valueOf(passiveInterval)).maxLength(16))
                .addCheckboxInput("can_cast_same_twice", Component.text("Cho phép cast trùng spell 2 lần liên tiếp"), canCastSameSpellTwice, "true", "false");

        DialogTypeBuilder.MultiAction multi = builder.multiAction();
        multi.addButton(Component.text("Lưu"), null, (response, audience) -> {
            Float interval = DialogInputUtils.parseFloat(response.getText("passive_interval"), 0f, 72000f);
            Boolean canCastTwice = response.getBoolean("can_cast_same_twice");
            if (interval != null) passiveInterval = Math.round(interval);
            if (canCastTwice != null) canCastSameSpellTwice = canCastTwice;
            commit();
            Utils.runLater(() -> player.showDialog(buildHubDialog(player)));
        });
        multi.columns(2);
        multi.exitButton(Component.text("Huỷ"), (response, audience) -> Utils.runLater(() -> player.showDialog(buildHubDialog(player))));

        return multi.build();
    }

    private Dialog buildListDialog(Player player, boolean active) {
        List<SpellParams> list = active ? activeSpells : passiveSpells;

        DialogBuilder builder = DialogBuilder.create(Component.text(active ? "Active spell" : "Passive spell"))
                .canCloseWithEscape(false)
                .externalTitle(Component.text(active ? "active_spell" : "passive_spell", list.isEmpty() ? NamedTextColor.GRAY : NamedTextColor.GREEN))
                .addTextBody(Component.text(list.size() + " spell"));

        DialogTypeBuilder.MultiAction multi = builder.multiAction();
        multi.addButton(Component.text("+ Thêm spell", NamedTextColor.GREEN), null,
                (response, audience) -> Utils.runLater(() -> player.showDialog(buildAddDialog(player, active))));
        for (int i = 0; i < list.size(); i++) {
            int index = i;
            SpellParams params = list.get(i);
            multi.addButton(Component.text((index + 1) + ". " + params.getTypeId()), usageTooltip(params.getTypeId()),
                    (response, audience) -> Utils.runLater(() -> player.showDialog(buildEntryDialog(player, active, index))));
        }
        multi.columns(2);
        multi.exitButton(Component.text("« Quay lại"), (response, audience) -> Utils.runLater(() -> player.showDialog(buildHubDialog(player))));

        return multi.build();
    }

    private Dialog buildAddDialog(Player player, boolean active) {
        Map<String, SpellType<? extends SpellParams>> types = new TreeMap<>(Registries.ENTITY_SPELL.getAll());

        DialogBuilder builder = DialogBuilder.create(Component.text("Chọn loại spell"))
                .canCloseWithEscape(false)
                .addTextBody(Component.text("Đỏ = usage không khớp danh sách này (vẫn có thể thêm)."));

        DialogTypeBuilder.MultiAction multi = builder.multiAction();
        for (Map.Entry<String, SpellType<? extends SpellParams>> entry : types.entrySet()) {
            String id = entry.getKey();
            SpellUsage usage = entry.getValue().usage();
            boolean matches = usage == SpellUsage.EITHER || (active == (usage == SpellUsage.ACTIVE));
            multi.addButton(Component.text(id, matches ? NamedTextColor.GREEN : NamedTextColor.RED),
                    Component.text("Usage: " + usage.name(), NamedTextColor.GRAY),
                    (response, audience) -> {
                        SpellParams params = createDefaultParams(entry.getValue());
                        if (params == null) {
                            player.sendMessage(Utils.text("Không thể tạo tham số mặc định cho spell '" + id + "'.", NamedTextColor.RED));
                        } else {
                            (active ? activeSpells : passiveSpells).add(params);
                            commit();
                        }
                        Utils.runLater(() -> player.showDialog(buildListDialog(player, active)));
                    });
        }
        multi.columns(3);
        multi.exitButton(Component.text("Huỷ"), (response, audience) -> Utils.runLater(() -> player.showDialog(buildListDialog(player, active))));

        return multi.build();
    }

    private Dialog buildEntryDialog(Player player, boolean active, int index) {
        List<SpellParams> list = active ? activeSpells : passiveSpells;
        SpellParams params = list.get(index);
        SpellType<? extends SpellParams> type = Registries.ENTITY_SPELL.get(params.getTypeId());
        SpellUsage usage = type != null ? type.usage() : null;

        DialogBuilder builder = DialogBuilder.create(Component.text("Spell: " + params.getTypeId()))
                .canCloseWithEscape(false)
                .addTextBody(Component.text("Usage: " + (usage == null ? "?" : usage.name())));

        DialogTypeBuilder.MultiAction multi = builder.multiAction();
        multi.addButton(Component.text("Xoá", NamedTextColor.RED), null, (response, audience) -> {
            list.remove(index);
            commit();
            Utils.runLater(() -> player.showDialog(buildListDialog(player, active)));
        });
        multi.columns(2);
        multi.exitButton(Component.text("« Quay lại"), (response, audience) -> Utils.runLater(() -> player.showDialog(buildListDialog(player, active))));

        return multi.build();
    }

    private void commit() {
        present = true;
        parent.applySpells(new SpellComponent(new ArrayList<>(activeSpells), new ArrayList<>(passiveSpells), passiveInterval, canCastSameSpellTwice));
    }

    private static @Nullable Component usageTooltip(String typeId) {
        SpellType<? extends SpellParams> type = Registries.ENTITY_SPELL.get(typeId);
        return type == null ? null : Component.text("Usage: " + type.usage().name(), NamedTextColor.GRAY);
    }

    private static <P extends SpellParams> @Nullable SpellParams createDefaultParams(SpellType<P> type) {
        DataResult<P> result = type.paramsCodec().decode(JsonOps.INSTANCE.emptyMap(), JsonOps.INSTANCE);
        return result.isSuccess() ? result.result() : null;
    }
}
