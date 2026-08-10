package com.roguesmp.gui.itemcreator;

import com.roguesmp.item.component.ItemComponentKeys;
import com.roguesmp.item.component.impl.PotionContentComponent;
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
 * Dialog-based hub for a {@link PotionContentComponent}: an ARGB color field plus its effect
 * list (delegated to {@link PotionEffectEditorGui}). Every leaf edit (color save, an effect
 * save/clear) immediately rebuilds and re-commits the whole component into the parent
 * {@link ItemCreatorGui} draft - there's no separate top-level apply step.
 */
public class PotionContentEditorGui {

    private final ItemCreatorGui parent;
    private boolean present;
    private String color;
    private List<PotionContentComponent.StoredEffect> effects;

    public PotionContentEditorGui(ItemCreatorGui parent, @Nullable PotionContentComponent current) {
        this.parent = parent;
        this.present = current != null;
        this.color = current == null ? "255,255,255,255" : current.getColor();
        this.effects = current == null ? new ArrayList<>() : new ArrayList<>(current.getEffects());
    }

    public Dialog buildHubDialog(Player player) {
        DialogBuilder builder = DialogBuilder.create(Component.text("Nội dung thuốc"))
                .canCloseWithEscape(false)
                .externalTitle(Component.text(ItemComponentKeys.POTION_CONTENT.id(), present ? NamedTextColor.GREEN : NamedTextColor.GRAY))
                .addTextBody(Component.text("Màu hiện tại: " + color))
                .addTextBody(Component.text(effects.size() + " hiệu ứng đã đặt"));

        DialogTypeBuilder.MultiAction multi = builder.multiAction();
        multi.addButton(Component.text("Đổi màu"), null, (response, audience) -> Utils.runLater(() -> player.showDialog(buildColorDialog(player))));
        multi.addButton(Component.text("Chỉnh hiệu ứng"), null, (response, audience) ->
                Utils.runLater(() -> player.showDialog(new PotionEffectEditorGui(this, effects).buildListDialog(player))));
        if (present) {
            multi.addButton(Component.text("Xoá toàn bộ"), null, (response, audience) -> {
                present = false;
                color = "255,255,255,255";
                effects = new ArrayList<>();
                parent.removePotionContent();
                Utils.runLater(() -> parent.reopen(player));
            });
        }
        multi.columns(present ? 3 : 2);
        multi.exitButton(Component.text("« Quay lại"), (response, audience) -> Utils.runLater(() -> parent.reopen(player)));

        return multi.build();
    }

    private Dialog buildColorDialog(Player player) {
        return DialogBuilder.create(Component.text("Màu thuốc"))
                .canCloseWithEscape(false)
                .addTextInput("value", Component.text("Màu ARGB (vd: 255,120,50,200)"), b -> b.initial(color).maxLength(32))
                .confirmation()
                .yesButton(Component.text("Xác nhận"), null, (response, audience) -> {
                    String text = response.getText("value");
                    if (isValidArgb(text)) {
                        color = text.trim();
                        commit();
                    } else {
                        player.sendMessage(Utils.text("Định dạng màu không hợp lệ. Dùng: a,r,g,b (0-255).", NamedTextColor.RED));
                    }
                    Utils.runLater(() -> player.showDialog(buildHubDialog(player)));
                })
                .noButton(Component.text("Huỷ"), null, (response, audience) -> Utils.runLater(() -> player.showDialog(buildHubDialog(player))))
                .build();
    }

    void commitEffects(List<PotionContentComponent.StoredEffect> newEffects) {
        this.effects = newEffects;
        commit();
    }

    void reopen(Player player) {
        player.showDialog(buildHubDialog(player));
    }

    private void commit() {
        present = true;
        parent.applyPotionContent(new PotionContentComponent(color, effects));
    }

    private static boolean isValidArgb(@Nullable String text) {
        if (text == null) return false;
        String[] parts = text.trim().split(",");
        if (parts.length != 4) return false;
        for (String part : parts) {
            try {
                int value = Integer.parseInt(part.trim());
                if (value < 0 || value > 255) return false;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return true;
    }
}
