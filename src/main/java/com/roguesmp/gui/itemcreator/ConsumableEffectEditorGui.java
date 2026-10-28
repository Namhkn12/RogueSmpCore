package com.roguesmp.gui.itemcreator;

import com.roguesmp.effect.SmpEffect;
import com.roguesmp.item.component.impl.ConsumableComponent;
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Dialog-based editor for a {@link ConsumableComponent}'s effect list. Each effect is edited as
 * lenient JSON through {@link SmpEffect#CODEC}, so every registered effect type is available.
 */
public class ConsumableEffectEditorGui {

    private static final String TYPE_KEY = "id";

    private final ConsumableEditorGui parent;
    private final Map<String, SmpEffect> effects;

    public ConsumableEffectEditorGui(ConsumableEditorGui parent, Map<String, SmpEffect> initialEffects) {
        this.parent = parent;
        this.effects = new LinkedHashMap<>(initialEffects);
    }

    public Dialog buildListDialog(Player player) {
        DialogBuilder builder = DialogBuilder.create(Component.text("Hiệu ứng khi sử dụng")).canCloseWithEscape(false);
        if (effects.isEmpty()) builder.addTextBody(Component.text("Chưa có hiệu ứng nào.", NamedTextColor.GRAY));

        DialogTypeBuilder.MultiAction multi = builder.multiAction();
        for (Map.Entry<String, SmpEffect> entry : effects.entrySet()) {
            String id = entry.getKey();
            multi.addButton(Component.text(id, NamedTextColor.GREEN), null,
                    (response, audience) -> Utils.runLater(() -> player.showDialog(entryDialog(player, id).build(player, entry.getValue()))));
        }
        multi.addButton(Component.text("+ Thêm hiệu ứng", NamedTextColor.GREEN), null,
                (response, audience) -> Utils.runLater(() -> player.showDialog(entryDialog(player, null).buildWithText(player, TYPE_KEY + ": "))));
        multi.columns(2);
        multi.exitButton(Component.text("« Quay lại"), (response, audience) -> Utils.runLater(() -> parent.reopen(player)));
        return multi.build();
    }

    private JsonEntryDialog<SmpEffect> entryDialog(Player player, @Nullable String editingId) {
        return JsonEntryDialog.of(editingId == null ? "Hiệu ứng mới" : "Hiệu ứng: " + editingId, SmpEffect.CODEC,
                effect -> {
                    if (editingId != null) effects.remove(editingId);
                    effects.put(effect.getEffectID(), effect);
                    parent.commitEffects(effects);
                    player.showDialog(buildListDialog(player));
                },
                editingId != null ? () -> {
                    effects.remove(editingId);
                    parent.commitEffects(effects);
                    player.showDialog(buildListDialog(player));
                } : null,
                () -> player.showDialog(buildListDialog(player)))
                .withHelp(List.of(Component.text(TYPE_KEY + ": ", NamedTextColor.YELLOW)
                        .append(Component.text(String.join(", ", new TreeSet<>(Registries.EFFECT_CODEC.getAll().keySet())), NamedTextColor.GRAY))));
    }
}
