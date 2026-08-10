package com.roguesmp.gui.itemcreator;

import com.roguesmp.item.component.impl.PotionContentComponent;
import com.roguesmp.utils.Utils;
import com.roguesmp.utils.dialog.DialogBuilder;
import com.roguesmp.utils.dialog.DialogTypeBuilder;
import io.papermc.paper.dialog.Dialog;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dialog-based sub-editor for a {@link com.roguesmp.item.component.impl.PotionContentComponent}'s
 * effect list. A {@code dialogList()} with one entry per vanilla {@link PotionEffectType}
 * ({@link Registry#MOB_EFFECT}); each entry is a duration+amplifier dialog that commits straight
 * into the parent {@link PotionContentEditorGui} on "Lưu"/"Xoá".
 */
public class PotionEffectEditorGui {

    private record EffectValue(int duration, int amplifier) {}

    private final PotionContentEditorGui parent;
    private final Map<PotionEffectType, EffectValue> values;

    public PotionEffectEditorGui(PotionContentEditorGui parent, List<PotionContentComponent.StoredEffect> initialEffects) {
        this.parent = parent;
        this.values = new HashMap<>();
        for (PotionContentComponent.StoredEffect stored : initialEffects) {
            PotionEffectType type = Registry.MOB_EFFECT.get(NamespacedKey.minecraft(stored.effectType()));
            if (type != null) values.put(type, new EffectValue(stored.duration(), stored.amplifier()));
        }
    }

    private List<PotionContentComponent.StoredEffect> toStoredEffects() {
        List<PotionContentComponent.StoredEffect> result = new ArrayList<>();
        values.forEach((type, value) -> result.add(new PotionContentComponent.StoredEffect(type.getKey().getKey(), value.duration(), value.amplifier())));
        return result;
    }

    public Dialog buildListDialog(Player player) {
        List<Dialog> entries = new ArrayList<>();
        for (PotionEffectType type : Registry.MOB_EFFECT) entries.add(buildValueDialog(player, type));

        DialogTypeBuilder.DialogList listBuilder = DialogBuilder.create(Component.text("Hiệu ứng thuốc"))
                .canCloseWithEscape(false)
                .dialogList()
                .columns(3)
                .buttonWidth(120)
                .exitButton(Component.text("« Quay lại"), (response, audience) -> Utils.runLater(() -> parent.reopen(player)));

        for (Dialog entry : entries) listBuilder.addDialog(entry);

        return listBuilder.build();
    }

    private Dialog buildValueDialog(Player player, PotionEffectType type) {
        EffectValue current = values.get(type);
        boolean present = current != null && current.duration() > 0;

        DialogBuilder builder = DialogBuilder.create(Component.text("Hiệu ứng: " + type.getKey().getKey()))
                .canCloseWithEscape(false)
                .externalTitle(Component.text(type.getKey().getKey(), present ? NamedTextColor.GREEN : NamedTextColor.GRAY))
                .addTextInput("duration", Component.text("Thời gian (tick, 0-72000)"), b -> b.initial(String.valueOf(present ? current.duration() : 0)).maxLength(16))
                .addTextInput("amplifier", Component.text("Cấp độ (0-10, 0 = cấp I)"), b -> b.initial(String.valueOf(present ? current.amplifier() : 0)).maxLength(16));

        DialogTypeBuilder.MultiAction multi = builder.multiAction();
        multi.addButton(Component.text("Lưu"), null, (response, audience) -> {
            Float duration = DialogInputUtils.parseFloat(response.getText("duration"), 0f, 72000f);
            Float amplifier = DialogInputUtils.parseFloat(response.getText("amplifier"), 0f, 10f);
            if (duration != null && amplifier != null) {
                if (duration <= 0f) values.remove(type);
                else values.put(type, new EffectValue(Math.round(duration), Math.round(amplifier)));
            }
            parent.commitEffects(toStoredEffects());
            Utils.runLater(() -> player.showDialog(buildListDialog(player)));
        });
        if (present) {
            multi.addButton(Component.text("Xoá"), null, (response, audience) -> {
                values.remove(type);
                parent.commitEffects(toStoredEffects());
                Utils.runLater(() -> player.showDialog(buildListDialog(player)));
            });
        }
        multi.columns(present ? 3 : 2);
        multi.exitButton(Component.text("Huỷ"), (response, audience) -> Utils.runLater(() -> player.showDialog(buildListDialog(player))));

        return multi.build();
    }
}
