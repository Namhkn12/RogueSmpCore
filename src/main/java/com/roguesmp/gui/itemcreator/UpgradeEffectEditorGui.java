package com.roguesmp.gui.itemcreator;

import com.roguesmp.block.upgrade.effect.UpgradeEffect;
import com.roguesmp.codec.Codec;
import com.roguesmp.registry.Registries;
import com.roguesmp.utils.Utils;
import com.roguesmp.utils.dialog.DialogTypeBuilder;
import com.roguesmp.utils.dialog.JsonEntryDialog;
import com.roguesmp.utils.dialog.LenientJson;
import io.papermc.paper.dialog.Dialog;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class UpgradeEffectEditorGui {

    private static final Codec<UpgradeEffect> CODEC = UpgradeEffect.CODEC.codec();

    private UpgradeEffect effect;
    private final Consumer<UpgradeEffect> onChange;

    public UpgradeEffectEditorGui(UpgradeEffect initial, Consumer<UpgradeEffect> onChange) {
        this.effect = initial;
        this.onChange = onChange;
    }

    public UpgradeEffect effect() {
        return effect;
    }

    public int size() {
        return effect.modifiers().size() + effect.behaviors().size();
    }

    public void addButtons(DialogTypeBuilder.MultiAction multi, Player player, Function<Player, Dialog> hub) {
        Component tooltip = size() == 0
                ? Component.text("Chưa có hiệu ứng", NamedTextColor.GRAY)
                : Component.join(JoinConfiguration.newlines(), effect.describe());
        multi.addButton(Component.text("effect (" + size() + ")", NamedTextColor.GOLD), tooltip,
                (response, audience) -> Utils.runLater(() -> player.showDialog(buildDialog(player, hub))));
    }

    private Dialog buildDialog(Player player, Function<Player, Dialog> hub) {
        return JsonEntryDialog.of("Hiệu ứng", CODEC,
                updated -> {
                    effect = updated;
                    onChange.accept(effect);
                    player.showDialog(hub.apply(player));
                },
                null,
                () -> player.showDialog(hub.apply(player)))
                .withHelp(List.of(LenientJson.typesHelp(Registries.UPGRADE_BEHAVIOR_CODEC.getAll().keySet())))
                .build(player, effect);
    }
}
