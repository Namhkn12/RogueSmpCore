package com.roguesmp.gui.itemcreator;

import com.roguesmp.item.ability.ItemAbility;
import com.roguesmp.item.component.ItemComponentKeys;
import com.roguesmp.item.component.impl.PassiveAbilityComponent;
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

/**
 * Dialog-based sub-editor of {@link ItemCreatorGui} for a {@link PassiveAbilityComponent}'s
 * ability list. New entries are picked from every type registered in
 * {@link Registries#ITEM_ABILITY_CODEC} and decoded with default values (an empty JSON object
 * through the type's own codec - every registered ability's fields are optional-with-defaults, so
 * this always succeeds), mirroring {@code EntitySpellListEditorGui}'s add-by-type shape.
 * <p>
 * Each entry is edited as lenient JSON through {@link ItemAbility#CODEC}, so every ability type is
 * tunable without a dedicated dialog.
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
                (response, audience) -> Utils.runLater(() -> player.showDialog(entryDialog(player, -1).buildWithText(player, "type: "))));
        for (int i = 0; i < abilities.size(); i++) {
            int index = i;
            ItemAbility ability = abilities.get(i);
            multi.addButton(Component.text((index + 1) + ". " + ability.getTypeId()), null,
                    (response, audience) -> Utils.runLater(() -> player.showDialog(entryDialog(player, index).build(player, abilities.get(index)))));
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

    private JsonEntryDialog<ItemAbility> entryDialog(Player player, int index) {
        boolean editing = index >= 0;
        return JsonEntryDialog.of(editing ? "Ability: " + abilities.get(index).getTypeId() : "Ability mới", ItemAbility.CODEC,
                value -> {
                    if (editing) abilities.set(index, value);
                    else abilities.add(value);
                    commit();
                    player.showDialog(buildListDialog(player));
                },
                editing ? () -> {
                    abilities.remove(index);
                    commit();
                    player.showDialog(buildListDialog(player));
                } : null,
                () -> player.showDialog(buildListDialog(player)))
                .withHelp(List.of(LenientJson.typesHelp(Registries.ITEM_ABILITY_CODEC.getAll().keySet())));
    }

    private void commit() {
        present = true;
        parent.applyPassiveAbility(new PassiveAbilityComponent(new ArrayList<>(abilities)));
    }
}
