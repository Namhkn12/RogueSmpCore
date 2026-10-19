package com.roguesmp.gui.itemcreator;

import com.roguesmp.block.impl.generator.part.DurationUnit;
import com.roguesmp.item.component.ItemComponentKeys;
import com.roguesmp.item.component.impl.GeneratorCatalystComponent;
import com.roguesmp.utils.Utils;
import com.roguesmp.utils.dialog.DialogBuilder;
import com.roguesmp.utils.dialog.DialogTypeBuilder;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

public class GeneratorCatalystEditorGui {

    private final ItemCreatorGui parent;
    private boolean present;
    private int duration;
    private DurationUnit unit;
    private final GeneratorEffectEditorGui effect;

    public GeneratorCatalystEditorGui(ItemCreatorGui parent, @Nullable GeneratorCatalystComponent current) {
        this.parent = parent;
        this.present = current != null;
        this.duration = current == null ? 1200 : current.duration();
        this.unit = current == null ? DurationUnit.TICKS : current.unit();
        this.effect = new GeneratorEffectEditorGui(current == null ? GeneratorEffectEditorGui.empty() : current.effect(), value -> commit());
    }

    public Dialog buildHubDialog(Player player) {
        DialogTypeBuilder.MultiAction multi = DialogBuilder.create(Component.text("Chất xúc tác"))
                .canCloseWithEscape(false)
                .externalTitle(Component.text(ItemComponentKeys.GENERATOR_CATALYST.id(), present ? NamedTextColor.GREEN : NamedTextColor.GRAY))
                .addTextBody(Component.text("Kéo dài: " + unit.format(duration)))
                .addTextBody(Component.text("Hiệu ứng: " + effect.size()))
                .addTextBody(Component.text("Vàng = modifier chỉ số, xanh = behavior.", NamedTextColor.DARK_GRAY))
                .multiAction();

        multi.addButton(Component.text("duration", NamedTextColor.GREEN), null,
                (response, audience) -> Utils.runLater(() -> player.showDialog(buildDurationDialog(player))));
        effect.addButtons(multi, player, this::buildHubDialog);
        if (present) {
            multi.addButton(Component.text("Xoá toàn bộ", NamedTextColor.RED), null, (response, audience) -> {
                present = false;
                parent.removeComponent(ItemComponentKeys.GENERATOR_CATALYST);
                Utils.runLater(() -> parent.reopen(player));
            });
        }
        multi.columns(2);
        multi.exitButton(Component.text("« Quay lại"), (response, audience) -> Utils.runLater(() -> parent.reopen(player)));
        return multi.build();
    }

    private Dialog buildDurationDialog(Player player) {
        return DialogBuilder.create(Component.text("Thời lượng"))
                .canCloseWithEscape(false)
                .addTextInput("duration", Component.text("Thời lượng (>= 1)"), b -> b.initial(String.valueOf(duration)).maxLength(16))
                .addSingleOptionInput("unit", Component.text("Đơn vị"), Arrays.stream(DurationUnit.values())
                        .map(value -> SingleOptionDialogInput.OptionEntry.create(value.name(), Component.text(value.name().toLowerCase(Locale.ROOT)), value == unit))
                        .toList(), b -> {})
                .confirmation()
                .yesButton(Component.text("Lưu"), null, (response, audience) -> {
                    Integer value = DialogInputUtils.parseInt(response.getText("duration"), 1, Integer.MAX_VALUE);
                    if (value != null) {
                        duration = value;
                        unit = DurationUnit.valueOf(Objects.requireNonNullElse(response.getText("unit"), unit.name()));
                        commit();
                    }
                    Utils.runLater(() -> player.showDialog(buildHubDialog(player)));
                })
                .noButton(Component.text("Huỷ"), null, (response, audience) -> Utils.runLater(() -> player.showDialog(buildHubDialog(player))))
                .build();
    }

    private void commit() {
        present = true;
        parent.applyComponent(ItemComponentKeys.GENERATOR_CATALYST, new GeneratorCatalystComponent(effect.effect(), duration, unit));
    }
}
