package com.roguesmp.warp.gui;

import com.roguesmp.codec.DataResult;
import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.key.Key;
import org.bukkit.inventory.ItemStack;
import com.roguesmp.command.CommandFeedback;
import com.roguesmp.gui.BaseGui;
import com.roguesmp.registry.Registries;
import com.roguesmp.teleport.TeleportDestination;
import com.roguesmp.teleport.TeleportDestinationParser;
import com.roguesmp.teleport.TeleportOptions;
import com.roguesmp.teleport.TeleportService;
import com.roguesmp.teleport.destination.LocationDestination;
import com.roguesmp.teleport.effect.TeleportEffects;
import com.roguesmp.utils.Utils;
import com.roguesmp.utils.dialog.DialogBuilder;
import com.roguesmp.utils.dialog.JsonEntryDialog;
import com.roguesmp.warp.Warp;
import com.roguesmp.warp.WarpService;
import io.papermc.paper.dialog.Dialog;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import static com.roguesmp.gui.GuiItems.hint;
import static com.roguesmp.gui.GuiItems.item;
import static com.roguesmp.gui.GuiItems.line;

public class WarpEditorGui extends BaseGui {

    private static final String LORE_SEPARATOR = "|";

    private static final int BACK_SLOT = 0;
    private static final int PREVIEW_SLOT = 4;
    private static final int DESTINATION_SLOT = 10;
    private static final int ICON_SLOT = 11;
    private static final int NAME_SLOT = 12;
    private static final int LORE_SLOT = 13;
    private static final int PERMISSION_SLOT = 14;
    private static final int ORDER_SLOT = 15;
    private static final int EFFECT_SLOT = 16;
    private static final int WARMUP_SLOT = 20;
    private static final int TEST_SLOT = 22;
    private static final int DELETE_SLOT = 26;

    private final Player player;
    private final String warpId;

    public WarpEditorGui(Player player, String warpId) {
        super(Component.text("Warp: " + warpId), 3);
        this.player = player;
        this.warpId = warpId;
    }

    @Override
    public void setup() {
        Warp warp = Registries.WARP.get(warpId);
        if (warp == null) {
            player.closeInventory();
            return;
        }

        clearUi();

        addButton(BACK_SLOT, item(Material.ARROW, "Back", NamedTextColor.GRAY), event -> {
            event.setCancelled(true);
            Utils.runLater(() -> new WarpAdminGui(player).showInventory(player));
        });

        addButton(PREVIEW_SLOT, item(warp.createIcon(), Utils.fromString(warp.displayName()), Utils.fromStrings(warp.lore())), ClickHandler.noAction());

        boolean available = warp.destination().isAvailable(player);
        addButton(DESTINATION_SLOT, item(available ? Material.COMPASS : Material.RECOVERY_COMPASS, "Destination", NamedTextColor.AQUA,
                line(warp.destination().format()),
                available ? line("Destination OK") : Utils.text("⚠ Destination can't be resolved", NamedTextColor.RED),
                hint("Left click: set to your position"),
                hint("Right click: type a destination")), event -> {
            event.setCancelled(true);
            if (event.getClick() == ClickType.RIGHT) {
                openTextDialog("Destination (" + TeleportDestinationParser.USAGE + ")", warp.destination().format(), this::setDestination);
                return;
            }
            apply(current -> current.withDestination(LocationDestination.of(player.getLocation())));
        });

        addButton(ICON_SLOT, item(warp.createIcon(), "Icon", NamedTextColor.GOLD,
                line(warp.icon().asString()),
                hint("Left click: use the model of your held item"),
                hint("Right click: type a model key")), event -> {
            event.setCancelled(true);
            if (event.getClick() == ClickType.RIGHT) {
                openTextDialog("Item model key (e.g. minecraft:compass)", warp.icon().asString(), this::setIcon);
                return;
            }

            ItemStack held = player.getInventory().getItemInMainHand();
            if (held.isEmpty()) {
                player.sendMessage(Utils.fromString("<red>Hold the item you want as the icon."));
                return;
            }
            Key model = held.hasData(DataComponentTypes.ITEM_MODEL) ? held.getData(DataComponentTypes.ITEM_MODEL) : held.getType().key();
            apply(current -> current.withIcon(model));
        });

        addButton(NAME_SLOT, item(Material.NAME_TAG, "Display name", NamedTextColor.LIGHT_PURPLE,
                Utils.fromString(warp.displayName()), hint("Click to edit (MiniMessage)")), event -> {
            event.setCancelled(true);
            openTextDialog("Display name (MiniMessage)", warp.displayName(), text -> apply(current -> current.withDisplayName(text)));
        });

        addButton(LORE_SLOT, item(Material.WRITABLE_BOOK, "Lore", NamedTextColor.LIGHT_PURPLE,
                line(warp.lore().isEmpty() ? "(none)" : warp.lore().size() + " lines"),
                hint("Click to edit, separate lines with " + LORE_SEPARATOR)), event -> {
            event.setCancelled(true);
            openTextDialog("Lore (lines separated by " + LORE_SEPARATOR + ")", String.join(LORE_SEPARATOR, warp.lore()),
                    text -> apply(current -> current.withLore(splitLore(text))));
        });

        addButton(PERMISSION_SLOT, item(Material.TRIPWIRE_HOOK, "Permission", NamedTextColor.RED,
                line(warp.permission() == null ? "(everyone)" : warp.permission()),
                hint("Click to edit, leave empty for everyone")), event -> {
            event.setCancelled(true);
            openTextDialog("Permission node", warp.permission() == null ? "" : warp.permission(),
                    text -> apply(current -> current.withPermission(text.isEmpty() ? null : text)));
        });

        addButton(ORDER_SLOT, item(Material.HOPPER, "Order: " + warp.order(), NamedTextColor.YELLOW,
                line("Lower comes first in the menu"),
                hint("Left / right click: +1 / -1"),
                hint("Shift: +10 / -10")), event -> {
            event.setCancelled(true);
            int step = event.getClick().isShiftClick() ? 10 : 1;
            int delta = event.getClick().isRightClick() ? -step : step;
            apply(current -> current.withOrder(current.order() + delta));
        });

        boolean hasEffect = warp.options().hasEffect();
        addButton(EFFECT_SLOT, item(hasEffect ? Material.LIME_DYE : Material.GRAY_DYE, "Effect: " + warp.options().effect().getTypeId(),
                hasEffect ? NamedTextColor.GREEN : NamedTextColor.GRAY,
                line("Presets: " + String.join(", ", TeleportEffects.PRESETS.keySet())),
                hint("Left click: cycle preset"),
                hint("Right click: edit options as JSON")), event -> {
            event.setCancelled(true);
            if (event.getClick().isRightClick()) {
                openOptionsDialog(warp.options());
                return;
            }
            apply(current -> current.withOptions(current.options().withEffect(TeleportEffects.next(current.options().effect()))));
        });

        int warmup = warp.options().warmupTicks();
        addButton(WARMUP_SLOT, item(Material.CLOCK, "Warmup: " + warmup / 20.0 + "s", NamedTextColor.YELLOW,
                line(warmup + " ticks"),
                hint("Left / right click: +5 / -5 ticks"),
                hint("Shift: +20 / -20 ticks")), event -> {
            event.setCancelled(true);
            int step = event.getClick().isShiftClick() ? 20 : 5;
            int delta = event.getClick().isRightClick() ? -step : step;
            apply(current -> current.withOptions(current.options().withWarmup(Math.max(0, current.options().warmupTicks() + delta))));
        });

        addButton(TEST_SLOT, item(Material.ENDER_PEARL, "Test teleport", NamedTextColor.DARK_AQUA, hint("Click")), event -> {
            event.setCancelled(true);
            player.closeInventory();
            TeleportService.getInstance().teleport(player, warp.destination(), warp.options());
        });

        addButton(DELETE_SLOT, item(Material.BARRIER, "Delete warp", NamedTextColor.DARK_RED, hint("Shift + right click")), event -> {
            event.setCancelled(true);
            if (event.getClick() != ClickType.SHIFT_RIGHT) return;
            WarpService.getInstance().delete(warpId);
            player.sendMessage(Utils.fromString("<yellow>Warp '" + warpId + "' deleted."));
            Utils.runLater(() -> new WarpAdminGui(player).showInventory(player));
        });

        fillEmpty(FILLER);
    }

    private void setDestination(String text) {
        DataResult<TeleportDestination> destination = TeleportDestinationParser.parse(text);
        if (CommandFeedback.failed(player, destination)) {
            showInventory(player);
            return;
        }
        apply(current -> current.withDestination(destination.result()));
    }

    private void setIcon(String text) {
        Key icon;
        try {
            icon = Key.key(text);
        } catch (InvalidKeyException e) {
            player.sendMessage(Utils.fromString("<red>Invalid model key: " + Utils.MINI_MESSAGE.escapeTags(text)));
            showInventory(player);
            return;
        }
        apply(current -> current.withIcon(icon));
    }

    private void apply(UnaryOperator<Warp> change) {
        CommandFeedback.failed(player, WarpService.getInstance().update(warpId, change));
        showInventory(player);
    }

    private void openOptionsDialog(TeleportOptions options) {
        Dialog dialog = JsonEntryDialog.of("Warp options: " + warpId, TeleportOptions.CODEC,
                updated -> apply(current -> current.withOptions(updated)),
                null,
                () -> showInventory(player))
                .build(player, options);

        player.closeInventory();
        player.showDialog(dialog);
    }

    private void openTextDialog(String label, String initial, Consumer<String> onSave) {
        Dialog dialog = DialogBuilder.create(Component.text("Warp: " + warpId))
                .addTextInput("value", Component.text(label), 300, true, initial, 512, null)
                .confirmation()
                .yesButton(Component.text("Save"), null, (response, audience) -> {
                    String value = clean(response.getText("value"));
                    Utils.runLater(() -> onSave.accept(value));
                })
                .noButton(Component.text("Cancel"), null, (response, audience) -> Utils.runLater(() -> showInventory(player)))
                .build();

        player.closeInventory();
        player.showDialog(dialog);
    }

    private static List<String> splitLore(String text) {
        if (text.isEmpty()) return List.of();
        return Arrays.stream(text.split("\\" + LORE_SEPARATOR)).map(String::trim).toList();
    }

    private static String clean(@Nullable String text) {
        return text == null ? "" : text.trim();
    }
}
