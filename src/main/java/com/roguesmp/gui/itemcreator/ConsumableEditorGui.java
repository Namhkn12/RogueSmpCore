package com.roguesmp.gui.itemcreator;

import com.roguesmp.effect.SmpEffect;
import com.roguesmp.item.component.ItemComponentKeys;
import com.roguesmp.item.component.impl.ConsumableComponent;
import com.roguesmp.utils.Utils;
import com.roguesmp.utils.dialog.DialogBuilder;
import com.roguesmp.utils.dialog.DialogTypeBuilder;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation;
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Dialog-based hub for a {@link ConsumableComponent}: its scalar fields (hunger, saturation,
 * animation, sound, ...) in one packed dialog, plus its effect list (delegated to
 * {@link ConsumableEffectEditorGui}). Every leaf edit re-commits the whole component into the
 * parent {@link ItemCreatorGui} draft, since {@code ConsumableComponent} has no partial
 * constructor - every field must be known at once.
 */
public class ConsumableEditorGui {

    private final ItemCreatorGui parent;
    private boolean present;
    private int hunger;
    private float saturation;
    private boolean canAlwaysEat;
    private float consumeSeconds;
    private ItemUseAnimation animation;
    private Key sound;
    private boolean hasParticles;
    private final Map<String, SmpEffect> effects;

    public ConsumableEditorGui(ItemCreatorGui parent, @Nullable ConsumableComponent current) {
        this.parent = parent;
        this.present = current != null;
        this.hunger = current == null ? 4 : current.getHunger();
        this.saturation = current == null ? 0.3f : current.getSaturation();
        this.canAlwaysEat = current != null && current.canAlwaysEat();
        this.consumeSeconds = current == null ? 1.6f : current.getConsumeSeconds();
        this.animation = current == null ? ItemUseAnimation.EAT : current.getAnimation();
        this.sound = current == null ? Key.key(Key.MINECRAFT_NAMESPACE, "entity.generic.eat") : current.getSound();
        this.hasParticles = current == null || current.hasParticles();
        this.effects = new LinkedHashMap<>();
        if (current != null) current.getEffects().forEach(effect -> effects.put(effect.getEffectID(), effect));
    }

    public Dialog buildHubDialog(Player player) {
        DialogBuilder builder = DialogBuilder.create(Component.text("Vật phẩm ăn/uống"))
                .canCloseWithEscape(false)
                .externalTitle(Component.text(ItemComponentKeys.CONSUMABLE.id(), present ? NamedTextColor.GREEN : NamedTextColor.GRAY))
                .addTextBody(Component.text("Đói: " + hunger + "  |  No: " + saturation + "  |  Thời gian dùng: " + consumeSeconds + "s"))
                .addTextBody(Component.text(effects.size() + " hiệu ứng khi sử dụng"));

        DialogTypeBuilder.MultiAction multi = builder.multiAction();
        multi.addButton(Component.text("Chỉnh thông số"), null, (response, audience) -> Utils.runLater(() -> player.showDialog(buildFieldsDialog(player))));
        multi.addButton(Component.text("Chỉnh hiệu ứng"), null, (response, audience) ->
                Utils.runLater(() -> player.showDialog(new ConsumableEffectEditorGui(this, effects).buildListDialog(player))));
        if (present) {
            multi.addButton(Component.text("Xoá toàn bộ"), null, (response, audience) -> {
                present = false;
                parent.removeConsumable();
                Utils.runLater(() -> parent.reopen(player));
            });
        }
        multi.columns(present ? 3 : 2);
        multi.exitButton(Component.text("« Quay lại"), (response, audience) -> Utils.runLater(() -> parent.reopen(player)));

        return multi.build();
    }

    private Dialog buildFieldsDialog(Player player) {
        List<SingleOptionDialogInput.OptionEntry> animationOptions = new ArrayList<>();
        for (ItemUseAnimation value : ItemUseAnimation.values()) {
            animationOptions.add(SingleOptionDialogInput.OptionEntry.create(value.name(), Component.text(value.name()), value == animation));
        }

        return DialogBuilder.create(Component.text("Thông số ăn/uống"))
                .canCloseWithEscape(false)
                .addTextInput("hunger", Component.text("Độ đói (thanh no, 0-20)"), b -> b.initial(String.valueOf(hunger)).maxLength(16))
                .addTextInput("saturation", Component.text("Độ bão hoà (0-20)"), b -> b.initial(Utils.formatDecimal(saturation)).maxLength(16))
                .addCheckboxInput("can_always_eat", Component.text("Ăn được kể cả khi no"), canAlwaysEat, "true", "false")
                .addTextInput("consume_seconds", Component.text("Thời gian sử dụng (giây, 0.1-10)"), b -> b.initial(Utils.formatDecimal(consumeSeconds)).maxLength(16))
                .addSingleOptionInput("animation", Component.text("Hoạt ảnh"), animationOptions, b -> {})
                .addTextInput("sound", Component.text("Âm thanh (vd: entity.generic.eat)"), b -> b.initial(sound.asString()).maxLength(128))
                .addCheckboxInput("has_particles", Component.text("Hiện hạt khi dùng"), hasParticles, "true", "false")
                .confirmation()
                .yesButton(Component.text("Lưu"), null, (response, audience) -> {
                    Float hungerValue = DialogInputUtils.parseFloat(response.getText("hunger"), 0f, 20f);
                    Float saturationValue = DialogInputUtils.parseFloat(response.getText("saturation"), 0f, 20f);
                    Boolean canAlwaysEatValue = response.getBoolean("can_always_eat");
                    Float consumeSecondsValue = DialogInputUtils.parseFloat(response.getText("consume_seconds"), 0.1f, 10f);
                    String animationValue = response.getText("animation");
                    String soundValue = response.getText("sound");
                    Boolean hasParticlesValue = response.getBoolean("has_particles");

                    if (hungerValue != null) hunger = Math.round(hungerValue);
                    if (saturationValue != null) saturation = saturationValue;
                    if (canAlwaysEatValue != null) canAlwaysEat = canAlwaysEatValue;
                    if (consumeSecondsValue != null) consumeSeconds = consumeSecondsValue;
                    if (animationValue != null) {
                        try {
                            animation = ItemUseAnimation.valueOf(animationValue);
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                    if (soundValue != null && !soundValue.isBlank()) {
                        try {
                            sound = parseSoundKey(soundValue);
                        } catch (Exception e) {
                            player.sendMessage(Utils.text("Key âm thanh không hợp lệ.", NamedTextColor.RED));
                        }
                    }
                    if (hasParticlesValue != null) hasParticles = hasParticlesValue;

                    commit();
                    Utils.runLater(() -> player.showDialog(buildHubDialog(player)));
                })
                .noButton(Component.text("Huỷ"), null, (response, audience) -> Utils.runLater(() -> player.showDialog(buildHubDialog(player))))
                .build();
    }

    void commitEffects(Map<String, SmpEffect> newEffects) {
        effects.clear();
        effects.putAll(newEffects);
        commit();
    }

    void reopen(Player player) {
        player.showDialog(buildHubDialog(player));
    }

    private void commit() {
        present = true;
        parent.applyConsumable(new ConsumableComponent(new ArrayList<>(effects.values()), hunger, saturation, canAlwaysEat, consumeSeconds, animation, sound, hasParticles));
    }

    private static Key parseSoundKey(String raw) {
        String trimmed = raw.trim();
        return trimmed.contains(":") ? Key.key(trimmed) : Key.key(Key.MINECRAFT_NAMESPACE, trimmed);
    }
}
