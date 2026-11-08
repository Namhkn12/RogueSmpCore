package com.roguesmp.gui.itemcreator;

import com.roguesmp.block.BlockData;
import com.roguesmp.block.BlockType;
import com.roguesmp.block.upgrade.UpgradeSynergy;
import com.roguesmp.block.upgrade.effect.UpgradeEffect;
import com.roguesmp.block.upgrade.DurationUnit;
import com.roguesmp.block.upgrade.UpgradeType;
import com.roguesmp.item.component.ItemComponentKeys;
import com.roguesmp.item.component.impl.BlockUpgradeComponent;
import com.roguesmp.registry.Holder;
import com.roguesmp.registry.Registries;
import com.roguesmp.utils.Utils;
import com.roguesmp.utils.dialog.DialogBuilder;
import com.roguesmp.utils.dialog.DialogTypeBuilder;
import com.roguesmp.utils.dialog.JsonEntryDialog;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TreeSet;

public class BlockUpgradeEditorGui {

    private final ItemCreatorGui parent;
    private boolean present;
    private UpgradeType type;
    private List<String> blockTypes;
    private List<String> blocks;
    private int duration;
    private DurationUnit unit;
    private List<String> incompatible;
    private int maxPerBlock;
    private final UpgradeEffectEditorGui effect;
    private final List<UpgradeSynergy> synergies = new ArrayList<>();

    public BlockUpgradeEditorGui(ItemCreatorGui parent, @Nullable BlockUpgradeComponent current) {
        this.parent = parent;
        this.present = current != null;
        this.type = current == null ? UpgradeType.MODULE : current.type();
        this.blockTypes = current == null ? List.of() : current.blockTypes().stream().map(Holder::getId).toList();
        this.blocks = current == null ? List.of() : current.blocks().stream().map(Holder::getId).toList();
        this.duration = current == null ? 0 : current.duration();
        this.unit = current == null ? DurationUnit.TICKS : current.unit();
        this.incompatible = current == null ? List.of() : current.incompatible();
        this.maxPerBlock = current == null ? 0 : current.maxPerBlock();
        this.effect = new UpgradeEffectEditorGui(current == null ? UpgradeEffect.EMPTY : current.effect(), value -> commit());
        if (current != null) synergies.addAll(current.synergies());
    }

    public Dialog buildHubDialog(Player player) {
        DialogTypeBuilder.MultiAction multi = DialogBuilder.create(Component.text("Nâng cấp khối"))
                .canCloseWithEscape(false)
                .externalTitle(Component.text(ItemComponentKeys.BLOCK_UPGRADE.id(), present ? NamedTextColor.GREEN : NamedTextColor.GRAY))
                .addTextBody(Component.text("Loại: " + type.displayName() + "  |  Dùng cho: " + targetsText()))
                .addTextBody(Component.text("Kéo dài: " + (duration <= 0 ? "(vĩnh viễn)" : unit.format(duration))))
                .addTextBody(Component.text("Tối đa mỗi máy: " + (maxPerBlock <= 0 ? "(không giới hạn)" : maxPerBlock)))
                .addTextBody(Component.text("Xung khắc: " + (incompatible.isEmpty() ? "(không)" : String.join(", ", incompatible))))
                .addTextBody(Component.text("Hiệu ứng: " + effect.size() + "  |  Synergy: " + synergies.size()))
                .addTextBody(Component.text("Chất xúc tác cần thời lượng, module thì không.", NamedTextColor.DARK_GRAY))
                .multiAction();

        multi.addButton(Component.text("settings", NamedTextColor.GREEN), null,
                (response, audience) -> Utils.runLater(() -> player.showDialog(buildSettingsDialog(player))));
        multi.addButton(Component.text("duration", NamedTextColor.GREEN), null,
                (response, audience) -> Utils.runLater(() -> player.showDialog(buildDurationDialog(player))));
        multi.addButton(Component.text("+ Thêm synergy", NamedTextColor.GREEN), null,
                (response, audience) -> Utils.runLater(() -> player.showDialog(synergyDialog(player, -1).buildWithText(player, "type: "))));
        effect.addButtons(multi, player, this::buildHubDialog);
        for (int i = 0; i < synergies.size(); i++) {
            int index = i;
            UpgradeSynergy synergy = synergies.get(i);
            UpgradeEffect synergyEffect = synergy.effect();
            multi.addButton(Component.text("synergy: " + synergy.type().getId(), NamedTextColor.LIGHT_PURPLE),
                    Component.text("Hiệu ứng: " + (synergyEffect.modifiers().size() + synergyEffect.behaviors().size()), NamedTextColor.GRAY),
                    (response, audience) -> Utils.runLater(() -> player.showDialog(synergyDialog(player, index).build(player, synergy))));
        }
        if (present) {
            multi.addButton(Component.text("Xoá toàn bộ", NamedTextColor.RED), null, (response, audience) -> {
                present = false;
                parent.removeComponent(ItemComponentKeys.BLOCK_UPGRADE);
                Utils.runLater(() -> parent.reopen(player));
            });
        }
        multi.columns(2);
        multi.exitButton(Component.text("« Quay lại"), (response, audience) -> Utils.runLater(() -> parent.reopen(player)));
        return multi.build();
    }

    private Dialog buildSettingsDialog(Player player) {
        return DialogBuilder.create(Component.text("Cài đặt nâng cấp"))
                .canCloseWithEscape(false)
                .addSingleOptionInput("type", Component.text("Loại nâng cấp"), Arrays.stream(UpgradeType.values())
                        .map(value -> SingleOptionDialogInput.OptionEntry.create(value.name(), Component.text(value.displayName()), value == type))
                        .toList(), b -> {})
                .addTextInput("block_types", Component.text("Loại khối dùng được (" + String.join(", ", new TreeSet<>(Registries.BLOCK_TYPE.getAll().keySet())) + ")"),
                        b -> b.initial(String.join(" ", blockTypes)).maxLength(1000))
                .addTextInput("blocks", Component.text("ID khối cụ thể dùng được (cách nhau bởi dấu cách)"),
                        b -> b.initial(String.join(" ", blocks)).maxLength(1000))
                .addTextInput("max", Component.text("Tối đa mỗi máy (0 = không giới hạn)"), b -> b.initial(String.valueOf(maxPerBlock)).maxLength(16))
                .addTextInput("incompatible", Component.text("ID vật phẩm xung khắc (cách nhau bởi dấu cách)"), b -> b.initial(String.join(" ", incompatible)).maxLength(1000))
                .confirmation()
                .yesButton(Component.text("Lưu"), null, (response, audience) -> {
                    Integer max = DialogInputUtils.parseInt(response.getText("max"), 0, Integer.MAX_VALUE);
                    List<String> typeIds = splitIds(response.getText("block_types"));
                    List<String> blockIds = splitIds(response.getText("blocks"));
                    List<String> itemIds = splitIds(response.getText("incompatible"));
                    List<String> unknownTypes = typeIds.stream().filter(id -> Registries.BLOCK_TYPE.get(id) == null).toList();
                    List<String> unknownBlocks = blockIds.stream().filter(id -> Registries.BLOCK.get(id) == null).toList();
                    List<String> unknownItems = itemIds.stream().filter(id -> Registries.ITEM.get(id) == null).toList();
                    if (!unknownTypes.isEmpty()) {
                        player.sendMessage(Utils.text("Không tìm thấy loại khối: " + String.join(", ", unknownTypes), NamedTextColor.RED));
                    } else if (!unknownBlocks.isEmpty()) {
                        player.sendMessage(Utils.text("Không tìm thấy khối: " + String.join(", ", unknownBlocks), NamedTextColor.RED));
                    } else if (!unknownItems.isEmpty()) {
                        player.sendMessage(Utils.text("Không tìm thấy vật phẩm: " + String.join(", ", unknownItems), NamedTextColor.RED));
                    } else if (max != null) {
                        type = UpgradeType.valueOf(Objects.requireNonNullElse(response.getText("type"), type.name()));
                        blockTypes = typeIds;
                        blocks = blockIds;
                        maxPerBlock = max;
                        incompatible = itemIds;
                        commit();
                    }
                    Utils.runLater(() -> player.showDialog(buildHubDialog(player)));
                })
                .noButton(Component.text("Huỷ"), null, (response, audience) -> Utils.runLater(() -> player.showDialog(buildHubDialog(player))))
                .build();
    }

    private Dialog buildDurationDialog(Player player) {
        return DialogBuilder.create(Component.text("Thời lượng"))
                .canCloseWithEscape(false)
                .addTextInput("duration", Component.text("Thời lượng (0 = vĩnh viễn)"), b -> b.initial(String.valueOf(duration)).maxLength(16))
                .addSingleOptionInput("unit", Component.text("Đơn vị"), Arrays.stream(DurationUnit.values())
                        .map(value -> SingleOptionDialogInput.OptionEntry.create(value.name(), Component.text(value.name().toLowerCase(Locale.ROOT)), value == unit))
                        .toList(), b -> {})
                .confirmation()
                .yesButton(Component.text("Lưu"), null, (response, audience) -> {
                    Integer value = DialogInputUtils.parseInt(response.getText("duration"), 0, Integer.MAX_VALUE);
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

    private JsonEntryDialog<UpgradeSynergy> synergyDialog(Player player, int index) {
        boolean editing = index >= 0;
        return JsonEntryDialog.of(editing ? "Synergy: " + synergies.get(index).type().getId() : "Synergy mới", UpgradeSynergy.CODEC,
                synergy -> {
                    if (editing) synergies.set(index, synergy);
                    else synergies.add(synergy);
                    commit();
                    player.showDialog(buildHubDialog(player));
                },
                editing ? () -> {
                    synergies.remove(index);
                    commit();
                    player.showDialog(buildHubDialog(player));
                } : null,
                () -> player.showDialog(buildHubDialog(player)))
                .withHelp(List.of(
                        Component.text("type (chất xúc tác): ", NamedTextColor.YELLOW)
                                .append(Component.text(String.join(", ", new TreeSet<>(Registries.ITEM_TYPE.getAll().keySet())), NamedTextColor.GRAY))));
    }

    private String targetsText() {
        List<String> targets = new ArrayList<>(blockTypes);
        targets.addAll(blocks);
        return targets.isEmpty() ? "(chưa chọn)" : String.join(", ", targets);
    }

    private static List<String> splitIds(@Nullable String text) {
        return Arrays.stream(Objects.requireNonNullElse(text, "").trim().split("\\s+"))
                .filter(id -> !id.isEmpty())
                .toList();
    }

    private void commit() {
        present = true;
        List<Holder<BlockType<?, ?>>> typeHolders = blockTypes.stream().map(Registries.BLOCK_TYPE::getHolder).toList();
        List<Holder<BlockData>> blockHolders = blocks.stream().map(Registries.BLOCK::getHolder).toList();
        parent.applyComponent(ItemComponentKeys.BLOCK_UPGRADE, new BlockUpgradeComponent(type, typeHolders, blockHolders, effect.effect(),
                duration, unit, incompatible, maxPerBlock, List.copyOf(synergies)));
    }
}
