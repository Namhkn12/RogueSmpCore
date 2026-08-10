package com.roguesmp.gui.itemcreator;

import com.roguesmp.effect.SmpEffect;
import com.roguesmp.effect.impl.DamageIncreaseEffect;
import com.roguesmp.effect.impl.ResistanceEffect;
import com.roguesmp.effect.impl.SpeedEffect;
import com.roguesmp.utils.Utils;
import com.roguesmp.utils.dialog.DialogBuilder;
import com.roguesmp.utils.dialog.DialogTypeBuilder;
import io.papermc.paper.dialog.Dialog;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Dialog-based editor for a {@link ConsumableComponent}'s effect list. Only three
 * {@link SmpEffect} subclasses exist in this codebase ({@link SpeedEffect},
 * {@link DamageIncreaseEffect}, {@link ResistanceEffect}), so unlike
 * {@link AttributeEditorGui}/{@link EnchantEditorGui} this is a fixed 3-entry list rather than an
 * enum sweep, and each entry packs that effect's own fields into one dialog instead of a single
 * slider. {@link ResistanceEffect}'s damage-type allowlist is left at its default
 * ({@link ResistanceEffect#DEFAULT_DAMAGE_TYPE}) - exposing a multi-select for it wasn't worth
 * the added complexity here.
 */
public class ConsumableEffectEditorGui {

    private final ConsumableEditorGui parent;
    private final Map<String, SmpEffect> effects;

    public ConsumableEffectEditorGui(ConsumableEditorGui parent, Map<String, SmpEffect> initialEffects) {
        this.parent = parent;
        this.effects = new LinkedHashMap<>(initialEffects);
    }

    public Dialog buildListDialog(Player player) {
        DialogTypeBuilder.DialogList listBuilder = DialogBuilder.create(Component.text("Hiệu ứng khi sử dụng"))
                .canCloseWithEscape(false)
                .dialogList()
                .columns(1)
                .buttonWidth(150)
                .exitButton(Component.text("« Quay lại"), (response, audience) -> Utils.runLater(() -> parent.reopen(player)));

        listBuilder.addDialog(buildSpeedDialog(player));
        listBuilder.addDialog(buildDamageIncreaseDialog(player));
        listBuilder.addDialog(buildResistanceDialog(player));

        return listBuilder.build();
    }

    private Dialog buildSpeedDialog(Player player) {
        SpeedEffect current = (SpeedEffect) effects.get(SpeedEffect.EFFECT_ID);
        boolean present = current != null;

        DialogBuilder builder = DialogBuilder.create(Component.text("Hiệu ứng: speed"))
                .canCloseWithEscape(false)
                .externalTitle(Component.text(SpeedEffect.EFFECT_ID, present ? NamedTextColor.GREEN : NamedTextColor.GRAY))
                .addTextInput("duration", Component.text("Thời gian (tick, 0-72000)"), b -> b.initial(String.valueOf(present ? current.getDuration() : 0)).maxLength(16))
                .addTextInput("percent", Component.text("Tốc chạy (%, -100 đến 500)"), b -> b.initial(present ? Utils.formatDecimal(current.getMagnitude() * 100) : "0").maxLength(16))
                .addTextInput("modifier_id", Component.text("Modifier ID"), b -> b.initial(present ? current.getModifierId() : "consumable_speed").maxLength(64));

        DialogTypeBuilder.MultiAction multi = builder.multiAction();
        multi.addButton(Component.text("Lưu"), null, (response, audience) -> {
            Float duration = DialogInputUtils.parseFloat(response.getText("duration"), 0f, 72000f);
            Float percent = DialogInputUtils.parseFloat(response.getText("percent"), -100f, 500f);
            String modifierId = response.getText("modifier_id");
            if (duration != null && duration > 0f && percent != null) {
                effects.put(SpeedEffect.EFFECT_ID, new SpeedEffect(Math.round(duration), percent / 100d, modifierId == null || modifierId.isBlank() ? "consumable_speed" : modifierId.trim()));
            } else {
                effects.remove(SpeedEffect.EFFECT_ID);
            }
            parent.commitEffects(effects);
            Utils.runLater(() -> player.showDialog(buildListDialog(player)));
        });
        if (present) {
            multi.addButton(Component.text("Xoá"), null, (response, audience) -> {
                effects.remove(SpeedEffect.EFFECT_ID);
                parent.commitEffects(effects);
                Utils.runLater(() -> player.showDialog(buildListDialog(player)));
            });
        }
        multi.columns(present ? 3 : 2);
        multi.exitButton(Component.text("Huỷ"), (response, audience) -> Utils.runLater(() -> player.showDialog(buildListDialog(player))));

        return multi.build();
    }

    private Dialog buildDamageIncreaseDialog(Player player) {
        DamageIncreaseEffect current = (DamageIncreaseEffect) effects.get(DamageIncreaseEffect.ID);
        boolean present = current != null;

        DialogBuilder builder = DialogBuilder.create(Component.text("Hiệu ứng: damage_increase"))
                .canCloseWithEscape(false)
                .externalTitle(Component.text(DamageIncreaseEffect.ID, present ? NamedTextColor.GREEN : NamedTextColor.GRAY))
                .addTextInput("duration", Component.text("Thời gian (tick, 0-72000)"), b -> b.initial(String.valueOf(present ? current.getDuration() : 0)).maxLength(16))
                .addTextInput("value", Component.text("Sát thương cộng thêm (-100 đến 100)"), b -> b.initial(present ? Utils.formatDecimal(current.getMagnitude()) : "0").maxLength(16));

        DialogTypeBuilder.MultiAction multi = builder.multiAction();
        multi.addButton(Component.text("Lưu"), null, (response, audience) -> {
            Float duration = DialogInputUtils.parseFloat(response.getText("duration"), 0f, 72000f);
            Float value = DialogInputUtils.parseFloat(response.getText("value"), -100f, 100f);
            if (duration != null && duration > 0f && value != null) {
                effects.put(DamageIncreaseEffect.ID, new DamageIncreaseEffect(Math.round(duration), value));
            } else {
                effects.remove(DamageIncreaseEffect.ID);
            }
            parent.commitEffects(effects);
            Utils.runLater(() -> player.showDialog(buildListDialog(player)));
        });
        if (present) {
            multi.addButton(Component.text("Xoá"), null, (response, audience) -> {
                effects.remove(DamageIncreaseEffect.ID);
                parent.commitEffects(effects);
                Utils.runLater(() -> player.showDialog(buildListDialog(player)));
            });
        }
        multi.columns(present ? 3 : 2);
        multi.exitButton(Component.text("Huỷ"), (response, audience) -> Utils.runLater(() -> player.showDialog(buildListDialog(player))));

        return multi.build();
    }

    private Dialog buildResistanceDialog(Player player) {
        ResistanceEffect current = (ResistanceEffect) effects.get(ResistanceEffect.ID);
        boolean present = current != null;

        DialogBuilder builder = DialogBuilder.create(Component.text("Hiệu ứng: resistance"))
                .canCloseWithEscape(false)
                .externalTitle(Component.text(ResistanceEffect.ID, present ? NamedTextColor.GREEN : NamedTextColor.GRAY))
                .addTextInput("duration", Component.text("Thời gian (tick, 0-72000)"), b -> b.initial(String.valueOf(present ? current.getDuration() : 0)).maxLength(16))
                .addTextInput("percent", Component.text("Miễn thương (%, 0-100)"), b -> b.initial(present ? Utils.formatDecimal(current.getMagnitude() * 100) : "0").maxLength(16));

        DialogTypeBuilder.MultiAction multi = builder.multiAction();
        multi.addButton(Component.text("Lưu"), null, (response, audience) -> {
            Float duration = DialogInputUtils.parseFloat(response.getText("duration"), 0f, 72000f);
            Float percent = DialogInputUtils.parseFloat(response.getText("percent"), 0f, 100f);
            if (duration != null && duration > 0f && percent != null) {
                effects.put(ResistanceEffect.ID, new ResistanceEffect(Math.round(duration), percent / 100d, SmpEffect.DeathBehavior.HALVES_ON_DEATH));
            } else {
                effects.remove(ResistanceEffect.ID);
            }
            parent.commitEffects(effects);
            Utils.runLater(() -> player.showDialog(buildListDialog(player)));
        });
        if (present) {
            multi.addButton(Component.text("Xoá"), null, (response, audience) -> {
                effects.remove(ResistanceEffect.ID);
                parent.commitEffects(effects);
                Utils.runLater(() -> player.showDialog(buildListDialog(player)));
            });
        }
        multi.columns(present ? 3 : 2);
        multi.exitButton(Component.text("Huỷ"), (response, audience) -> Utils.runLater(() -> player.showDialog(buildListDialog(player))));

        return multi.build();
    }
}
