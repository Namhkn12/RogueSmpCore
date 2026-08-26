package com.roguesmp.gui.itemcreator;

import com.roguesmp.codec.Codec;
import com.roguesmp.codec.DataResult;
import com.roguesmp.codec.JsonOps;
import com.roguesmp.item.ability.ItemAbility;
import com.roguesmp.item.ability.impl.Barking;
import com.roguesmp.item.ability.impl.EntityZapper;
import com.roguesmp.item.ability.impl.UnyieldingEdge;
import com.roguesmp.item.component.ItemComponentKeys;
import com.roguesmp.item.component.impl.PassiveAbilityComponent;
import com.roguesmp.registry.Registries;
import com.roguesmp.utils.Utils;
import com.roguesmp.utils.dialog.DialogBuilder;
import com.roguesmp.utils.dialog.DialogTypeBuilder;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.action.DialogActionCallback;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Dialog-based sub-editor of {@link ItemCreatorGui} for a {@link PassiveAbilityComponent}'s
 * ability list. New entries are picked from every type registered in
 * {@link Registries#ITEM_ABILITY_CODEC} and decoded with default values (an empty JSON object
 * through the type's own codec - every registered ability's fields are optional-with-defaults, so
 * this always succeeds), mirroring {@code EntitySpellListEditorGui}'s add-by-type shape.
 * <p>
 * Unlike spells, {@link ItemAbility} decodes straight into a finished, tunable instance (no
 * separate params record - see {@link ItemAbility}'s own javadoc), so each entry also gets its own
 * field-editing dialog here (one per concrete ability type) instead of only supporting
 * remove-and-re-add - that's the whole point of collapsing params, per-item tuning should actually
 * be reachable from the GUI.
 */
public class ItemAbilityListEditorGui {

    private final ItemCreatorGui parent;
    private boolean present;
    private final List<ItemAbility> abilities;

    public ItemAbilityListEditorGui(ItemCreatorGui parent, @Nullable PassiveAbilityComponent current) {
        this.parent = parent;
        this.present = current != null;
        this.abilities = new ArrayList<>(current == null ? List.of() : current.getAbilities());
    }

    public Dialog buildListDialog(Player player) {
        DialogBuilder builder = DialogBuilder.create(Component.text("Passive ability"))
                .canCloseWithEscape(false)
                .externalTitle(Component.text(ItemComponentKeys.PASSIVE_ABILITY.id(), present ? NamedTextColor.GREEN : NamedTextColor.GRAY))
                .addTextBody(Component.text(abilities.size() + " ability"));

        DialogTypeBuilder.MultiAction multi = builder.multiAction();
        multi.addButton(Component.text("+ Thêm ability", NamedTextColor.GREEN), null,
                (response, audience) -> Utils.runLater(() -> player.showDialog(buildAddDialog(player))));
        for (int i = 0; i < abilities.size(); i++) {
            int index = i;
            ItemAbility ability = abilities.get(i);
            multi.addButton(Component.text((index + 1) + ". " + ability.getTypeId()), null,
                    (response, audience) -> Utils.runLater(() -> player.showDialog(buildEntryDialog(player, index))));
        }
        if (present) {
            multi.addButton(Component.text("Xoá toàn bộ", NamedTextColor.RED), null, (response, audience) -> {
                present = false;
                abilities.clear();
                parent.removePassiveAbility();
                Utils.runLater(() -> parent.reopen(player));
            });
        }
        multi.columns(3);
        multi.exitButton(Component.text("« Quay lại"), (response, audience) -> Utils.runLater(() -> parent.reopen(player)));

        return multi.build();
    }

    private Dialog buildAddDialog(Player player) {
        Map<String, Codec<? extends ItemAbility>> types = new TreeMap<>(Registries.ITEM_ABILITY_CODEC.getAll());

        DialogBuilder builder = DialogBuilder.create(Component.text("Chọn loại ability"))
                .canCloseWithEscape(false);

        DialogTypeBuilder.MultiAction multi = builder.multiAction();
        for (Map.Entry<String, Codec<? extends ItemAbility>> entry : types.entrySet()) {
            String id = entry.getKey();
            multi.addButton(Component.text(id), null, (response, audience) -> {
                ItemAbility ability = createDefault(entry.getValue());
                if (ability == null) {
                    player.sendMessage(Utils.text("Không thể tạo ability mặc định cho '" + id + "'.", NamedTextColor.RED));
                } else {
                    abilities.add(ability);
                    commit();
                }
                Utils.runLater(() -> player.showDialog(buildListDialog(player)));
            });
        }
        multi.columns(3);
        multi.exitButton(Component.text("Huỷ"), (response, audience) -> Utils.runLater(() -> player.showDialog(buildListDialog(player))));

        return multi.build();
    }

    private Dialog buildEntryDialog(Player player, int index) {
        ItemAbility ability = abilities.get(index);

        if (ability instanceof UnyieldingEdge unyieldingEdge) return buildUnyieldingEdgeDialog(player, index, unyieldingEdge);
        if (ability instanceof Barking barking) return buildBarkingDialog(player, index, barking);
        if (ability instanceof EntityZapper zapper) return buildZapperDialog(player, index, zapper);

        return buildGenericEntryDialog(player, index, ability);
    }

    private Dialog buildZapperDialog(Player player, int index, EntityZapper zapper) {
        DialogBuilder builder = DialogBuilder.create(Component.text("Ability: " + zapper.getTypeId()))
                .canCloseWithEscape(false);
        Dialog dialog = builder.multiAction()
                .addButton(Component.text("Thêm"), null, (s, audience) -> {
                    abilities.remove(index);
                    commit();
                    Utils.runLater(() -> player.showDialog(buildListDialog(player)));
                })
                .addButton(Component.text("Xóa"), null, (response, audience) -> {
                    abilities.set(index, new EntityZapper());
                    commit();
                    Utils.runLater(() -> player.showDialog(buildListDialog(player)));
                })
                .columns(3)
                .exitButton(Component.text("« Quay lại"), (response, audience) -> Utils.runLater(() -> player.showDialog(buildListDialog(player))))
                .build();

        return dialog;
    }

    private Dialog buildUnyieldingEdgeDialog(Player player, int index, UnyieldingEdge current) {
        DialogBuilder builder = DialogBuilder.create(Component.text("Ability: " + current.getTypeId()))
                .canCloseWithEscape(false)
                .addTextInput("dmg_per_unit", Component.text("ATK cộng thêm mỗi đơn vị độ bền mất"), b -> b.initial(Utils.formatDecimal(current.getDmgPerUnitLoss())).maxLength(16))
                .addTextInput("amount_per_unit", Component.text("Số độ bền mất / 1 đơn vị"), b -> b.initial(String.valueOf(current.getAmountLossPerUnit())).maxLength(16));

        return wrapEntryDialog(player, index, builder, (response, audience) -> {
            Float dmgPerUnit = DialogInputUtils.parseFloat(response.getText("dmg_per_unit"), 0f, Float.MAX_VALUE);
            Float amountPerUnit = DialogInputUtils.parseFloat(response.getText("amount_per_unit"), 1f, Float.MAX_VALUE);
            abilities.set(index, new UnyieldingEdge(
                    dmgPerUnit == null ? current.getDmgPerUnitLoss() : (double) dmgPerUnit,
                    amountPerUnit == null ? current.getAmountLossPerUnit() : Math.round(amountPerUnit)));
            commit();
            Utils.runLater(() -> player.showDialog(buildListDialog(player)));
        });
    }

    private Dialog buildBarkingDialog(Player player, int index, Barking current) {
        DialogBuilder builder = DialogBuilder.create(Component.text("Ability: " + current.getTypeId()))
                .canCloseWithEscape(false)
                .addTextInput("interval", Component.text("Khoảng cách giữa các lần phát (tick)"), b -> b.initial(String.valueOf(current.getInterval())).maxLength(16))
                .addTextBody(Component.text(current.getSounds().size() + " âm thanh"));

        DialogTypeBuilder.MultiAction multi = builder.multiAction();
        multi.addButton(Component.text("Lưu"), null, (response, audience) -> {
            Float interval = DialogInputUtils.parseFloat(response.getText("interval"), 1f, Float.MAX_VALUE);
            abilities.set(index, new Barking(interval == null ? current.getInterval() : Math.round(interval), current.getSounds()));
            commit();
            Utils.runLater(() -> player.showDialog(buildListDialog(player)));
        });
        multi.addButton(Component.text("Chỉnh âm thanh"), null, (response, audience) ->
                Utils.runLater(() -> player.showDialog(buildBarkingSoundListDialog(player, index))));
        multi.addButton(Component.text("Xoá", NamedTextColor.RED), null, (response, audience) -> {
            abilities.remove(index);
            commit();
            Utils.runLater(() -> player.showDialog(buildListDialog(player)));
        });
        multi.columns(3);
        multi.exitButton(Component.text("« Quay lại"), (response, audience) -> Utils.runLater(() -> player.showDialog(buildListDialog(player))));

        return multi.build();
    }

    private Dialog buildBarkingSoundListDialog(Player player, int index) {
        Barking current = (Barking) abilities.get(index);
        List<Barking.SoundEntry> sounds = current.getSounds();

        DialogBuilder builder = DialogBuilder.create(Component.text("Âm thanh - " + current.getTypeId()))
                .canCloseWithEscape(false)
                .addTextBody(Component.text(sounds.size() + " âm thanh"));

        DialogTypeBuilder.MultiAction multi = builder.multiAction();
        multi.addButton(Component.text("+ Thêm âm thanh", NamedTextColor.GREEN), null, (response, audience) ->
                Utils.runLater(() -> player.showDialog(buildBarkingSoundEntryDialog(player, index, -1))));
        for (int i = 0; i < sounds.size(); i++) {
            int soundIndex = i;
            Barking.SoundEntry entry = sounds.get(i);
            multi.addButton(Component.text((soundIndex + 1) + ". " + entry.key().asString()), null,
                    (response, audience) -> Utils.runLater(() -> player.showDialog(buildBarkingSoundEntryDialog(player, index, soundIndex))));
        }
        multi.columns(3);
        multi.exitButton(Component.text("« Quay lại"), (response, audience) -> Utils.runLater(() -> player.showDialog(buildEntryDialog(player, index))));

        return multi.build();
    }

    private Dialog buildBarkingSoundEntryDialog(Player player, int index, int soundIndex) {
        Barking current = (Barking) abilities.get(index);
        List<Barking.SoundEntry> sounds = current.getSounds();
        boolean editing = soundIndex >= 0 && soundIndex < sounds.size();
        Barking.SoundEntry existing = editing ? sounds.get(soundIndex) : null;

        DialogBuilder builder = DialogBuilder.create(Component.text(editing ? "Sửa âm thanh" : "Thêm âm thanh"))
                .canCloseWithEscape(false)
                .addTextInput("key", Component.text("Sound key (vd: entity.wolf.growl)"), b -> b.initial(existing == null ? "" : existing.key().asString()).maxLength(128))
                .addTextInput("volume", Component.text("Âm lượng"), b -> b.initial(existing == null ? "1" : Utils.formatDecimal(existing.volume())).maxLength(16))
                .addTextInput("pitch", Component.text("Cao độ"), b -> b.initial(existing == null ? "1" : Utils.formatDecimal(existing.pitch())).maxLength(16));

        DialogTypeBuilder.MultiAction multi = builder.multiAction();
        multi.addButton(Component.text("Lưu"), null, (response, audience) -> {
            String keyText = response.getText("key");
            Float volume = DialogInputUtils.parseFloat(response.getText("volume"), 0f, Float.MAX_VALUE);
            Float pitch = DialogInputUtils.parseFloat(response.getText("pitch"), 0f, 2f);

            if (keyText == null || keyText.isBlank()) {
                player.sendMessage(Utils.text("Sound key không hợp lệ.", NamedTextColor.RED));
            } else {
                try {
                    Key soundKey = parseSoundKey(keyText);
                    List<Barking.SoundEntry> updated = new ArrayList<>(sounds);
                    Barking.SoundEntry newEntry = new Barking.SoundEntry(soundKey, volume == null ? 1f : volume, pitch == null ? 1f : pitch);
                    if (editing) updated.set(soundIndex, newEntry);
                    else updated.add(newEntry);
                    abilities.set(index, new Barking(current.getInterval(), updated));
                    commit();
                } catch (Exception e) {
                    player.sendMessage(Utils.text("Sound key không hợp lệ.", NamedTextColor.RED));
                }
            }
            Utils.runLater(() -> player.showDialog(buildBarkingSoundListDialog(player, index)));
        });
        if (editing) {
            multi.addButton(Component.text("Xoá", NamedTextColor.RED), null, (response, audience) -> {
                List<Barking.SoundEntry> updated = new ArrayList<>(sounds);
                updated.remove(soundIndex);
                abilities.set(index, new Barking(current.getInterval(), updated));
                commit();
                Utils.runLater(() -> player.showDialog(buildBarkingSoundListDialog(player, index)));
            });
        }
        multi.columns(editing ? 3 : 2);
        multi.exitButton(Component.text("« Quay lại"), (response, audience) -> Utils.runLater(() -> player.showDialog(buildBarkingSoundListDialog(player, index))));

        return multi.build();
    }

    private static Key parseSoundKey(String raw) {
        String trimmed = raw.trim();
        return trimmed.contains(":") ? Key.key(trimmed) : Key.key(Key.MINECRAFT_NAMESPACE, trimmed);
    }

    private Dialog buildGenericEntryDialog(Player player, int index, ItemAbility ability) {
        DialogBuilder builder = DialogBuilder.create(Component.text("Ability: " + ability.getTypeId()))
                .canCloseWithEscape(false)
                .addTextBody(Component.text("Ability này chưa có giao diện chỉnh sửa riêng - chỉ có thể xoá."));

        DialogTypeBuilder.MultiAction multi = builder.multiAction();
        multi.addButton(Component.text("Xoá", NamedTextColor.RED), null, (response, audience) -> {
            abilities.remove(index);
            commit();
            Utils.runLater(() -> player.showDialog(buildListDialog(player)));
        });
        multi.columns(2);
        multi.exitButton(Component.text("« Quay lại"), (response, audience) -> Utils.runLater(() -> player.showDialog(buildListDialog(player))));

        return multi.build();
    }

    /**
     * Wraps a field-editing {@link DialogBuilder} (inputs already added) into "Lưu" / "Xoá" (this
     * entry) / exit, mirroring {@code ItemCreatorGui}'s private {@code wrapComponentDialog} (not
     * reusable here since it's private to that class).
     */
    private Dialog wrapEntryDialog(Player player, int index, DialogBuilder builder, DialogActionCallback onSave) {
        DialogTypeBuilder.MultiAction multi = builder.multiAction();
        multi.addButton(Component.text("Lưu"), null, onSave);
        multi.addButton(Component.text("Xoá", NamedTextColor.RED), null, (response, audience) -> {
            abilities.remove(index);
            commit();
            Utils.runLater(() -> player.showDialog(buildListDialog(player)));
        });
        multi.columns(3);
        multi.exitButton(Component.text("« Quay lại"), (response, audience) -> Utils.runLater(() -> player.showDialog(buildListDialog(player))));

        return multi.build();
    }

    private void commit() {
        present = true;
        parent.applyPassiveAbility(new PassiveAbilityComponent(new ArrayList<>(abilities)));
    }

    private static <T extends ItemAbility> @Nullable ItemAbility createDefault(Codec<T> codec) {
        DataResult<T> result = codec.decode(JsonOps.INSTANCE.emptyMap(), JsonOps.INSTANCE);
        return result.isSuccess() ? result.result() : null;
    }
}
