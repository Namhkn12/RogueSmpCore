package com.roguesmp.billboard.gui;

import com.roguesmp.billboard.Billboard;
import com.roguesmp.billboard.BillboardManager;
import com.roguesmp.billboard.DisplaySettings;
import com.roguesmp.billboard.line.AnimatedLine;
import com.roguesmp.billboard.line.BillboardLine;
import com.roguesmp.billboard.line.TextLine;
import com.roguesmp.codec.Codec;
import com.roguesmp.command.CommandFeedback;
import com.roguesmp.gui.BaseGui;
import com.roguesmp.registry.Registries;
import com.roguesmp.utils.Utils;
import com.roguesmp.utils.dialog.JsonEntryDialog;
import io.papermc.paper.dialog.Dialog;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Marker;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import static com.roguesmp.gui.GuiItems.hint;
import static com.roguesmp.gui.GuiItems.item;
import static com.roguesmp.gui.GuiItems.line;

public class BillboardEditorGui extends BaseGui {

    private static final int BACK_SLOT = 0;
    private static final int INFO_SLOT = 4;
    private static final int DELETE_SLOT = 8;
    private static final int PLACE_SLOT = 10;
    private static final int NUDGE_X_SLOT = 11;
    private static final int NUDGE_Y_SLOT = 12;
    private static final int NUDGE_Z_SLOT = 13;
    private static final int VIEW_RANGE_SLOT = 14;
    private static final int REFRESH_SLOT = 15;
    private static final int SPACING_SLOT = 16;
    private static final int DISPLAY_SLOT = 19;
    private static final int REMOVE_COPY_SLOT = 21;
    private static final int RAW_JSON_SLOT = 20;
    private static final int COPIES_SLOT = 22;
    private static final int ADD_TEXT_SLOT = 24;
    private static final int ADD_ANIMATED_SLOT = 25;
    private static final int FIRST_LINE_SLOT = 27;
    private static final int LINE_SLOTS = 18;
    private static final double NEAREST_RADIUS = 4;

    private final Player player;
    private final String billboardId;
    private final BaseGui parent;

    public BillboardEditorGui(Player player, String billboardId) {
        this(player, billboardId, new BillboardBrowserGui(player));
    }

    public BillboardEditorGui(Player player, String billboardId, BaseGui parent) {
        super(Component.text("Billboard: " + billboardId), 5);
        this.player = player;
        this.billboardId = billboardId;
        this.parent = parent;
    }

    @Override
    public void setup() {
        Billboard billboard = Registries.BILLBOARD.get(billboardId);
        if (billboard == null) {
            player.closeInventory();
            return;
        }

        clearUi();
        List<BillboardManager.Placed> copies = BillboardManager.getInstance().copiesOf(billboardId);

        addButton(BACK_SLOT, item(Material.ARROW, "Back", NamedTextColor.GRAY), event -> {
            event.setCancelled(true);
            Utils.runLater(() -> parent.showInventory(player));
        });

        addButton(INFO_SLOT, item(Material.OAK_SIGN, billboardId, NamedTextColor.AQUA,
                line(copies.size() + " loaded copy(ies)"),
                line(billboard.lines().size() + " line(s)")), ClickHandler.noAction());

        addButton(DELETE_SLOT, item(Material.BARRIER, "Delete billboard", NamedTextColor.DARK_RED,
                line("Removes loaded copies; unloaded ones stay as strays"), hint("Shift + right click")), event -> {
            event.setCancelled(true);
            if (event.getClick() != ClickType.SHIFT_RIGHT) return;
            BillboardManager.getInstance().delete(billboardId);
            player.sendMessage(Utils.fromString("<yellow>Billboard '" + billboardId + "' deleted."));
            Utils.runLater(() -> parent.showInventory(player));
        });

        addButton(PLACE_SLOT, item(Material.ARMOR_STAND, "Place here", NamedTextColor.GREEN,
                line("Adds a copy at your position"), hint("Click")), event -> {
            event.setCancelled(true);
            BillboardManager.getInstance().place(billboardId, player.getLocation());
            player.sendMessage(Utils.fromString("<green>Placed billboard '" + billboardId + "' at your position."));
            Utils.runLater(this::setup);
        });

        Marker copy = BillboardManager.nearestPlaced(player.getLocation(), NEAREST_RADIUS, billboardId);
        if (copy != null) {
            Location at = copy.getLocation();
            addNudgeButton(NUDGE_X_SLOT, "X", at.getX(), copy, 1, 0, 0);
            addNudgeButton(NUDGE_Y_SLOT, "Y", at.getY(), copy, 0, 1, 0);
            addNudgeButton(NUDGE_Z_SLOT, "Z", at.getZ(), copy, 0, 0, 1);
            addButton(REMOVE_COPY_SLOT, item(Material.TNT, "Remove nearest copy", NamedTextColor.RED,
                    line(String.format("At %.1f, %.1f, %.1f", at.getX(), at.getY(), at.getZ())),
                    line("Other copies are not affected"), hint("Shift + right click")), event -> {
                event.setCancelled(true);
                if (event.getClick() != ClickType.SHIFT_RIGHT) return;
                if (copy.isValid()) {
                    copy.remove();
                    player.sendMessage(Utils.fromString("<yellow>Removed a copy of '" + billboardId + "'."));
                }
                Utils.runLater(this::setup);
            });
        } else {
            addButton(NUDGE_Y_SLOT, item(Material.GRAY_DYE, "No copy nearby", NamedTextColor.GRAY,
                    line("Stand within " + (int) NEAREST_RADIUS + " blocks of a copy to move or remove it")), ClickHandler.noAction());
        }

        addButton(COPIES_SLOT, item(Material.ENDER_PEARL, "Copies (" + copies.size() + " loaded)", NamedTextColor.DARK_AQUA,
                line("Where this billboard is placed"),
                line("Teleport to or remove each copy"), hint("Click")), event -> {
            event.setCancelled(true);
            Utils.runLater(() -> new BillboardCopiesGui(player, billboardId, this).showInventory(player));
        });

        addButton(VIEW_RANGE_SLOT, item(Material.SPYGLASS, "View range: " + billboard.viewRange(), NamedTextColor.YELLOW,
                hint("Left / right click: +4 / -4"), hint("Shift: +16 / -16")), event -> {
            event.setCancelled(true);
            double delta = step(event, 4, 16);
            apply(current -> current.withViewRange(Math.max(1, current.viewRange() + delta)));
        });

        addButton(REFRESH_SLOT, item(Material.CLOCK, "Refresh: " + billboard.refresh() + " ticks", NamedTextColor.YELLOW,
                line("How often per-player and animated text updates"),
                hint("Left / right click: +1 / -1"), hint("Shift: +10 / -10")), event -> {
            event.setCancelled(true);
            int delta = (int) step(event, 1, 10);
            apply(current -> current.withRefresh(Math.max(1, current.refresh() + delta)));
        });

        addButton(SPACING_SLOT, item(Material.LADDER, String.format("Line spacing: %.2f", billboard.lineSpacing()), NamedTextColor.YELLOW,
                line("Gap between differently styled segments"),
                hint("Left / right click: +0.01 / -0.01"), hint("Shift: +0.1 / -0.1")), event -> {
            event.setCancelled(true);
            double delta = step(event, 0.01, 0.1);
            apply(current -> current.withLineSpacing(Math.max(0, current.lineSpacing() + delta)));
        });

        addButton(DISPLAY_SLOT, item(Material.PAINTING, "Display defaults", NamedTextColor.LIGHT_PURPLE,
                line("Style every line inherits unless it overrides it"),
                line("Billboard mode, scale, shadow, background..."), hint("Click")), event -> {
            event.setCancelled(true);
            Utils.runLater(() -> new DisplaySettingsGui(player, billboardId, this).showInventory(player));
        });

        addButton(RAW_JSON_SLOT, item(Material.WRITABLE_BOOK, "Edit whole billboard", NamedTextColor.LIGHT_PURPLE,
                line("Everything, including line conditions"), hint("Click: edit as JSON")), event -> {
            event.setCancelled(true);
            openJson("Billboard: " + billboardId, Billboard.CODEC, billboard,
                    updated -> apply(current -> updated), null);
        });

        addButton(ADD_TEXT_SLOT, item(Material.PAPER, "Add text line", NamedTextColor.GREEN, hint("Click")), event -> {
            event.setCancelled(true);
            addLine(TextLine.of("<white>New line"));
        });

        addButton(ADD_ANIMATED_SLOT, item(Material.FIREWORK_STAR, "Add animated line", NamedTextColor.GREEN, hint("Click")), event -> {
            event.setCancelled(true);
            addLine(new AnimatedLine(List.of(TextLine.of("<gold>Frame 1"), TextLine.of("<yellow>Frame 2")), 10, DisplaySettings.EMPTY, null));
        });

        List<BillboardLine> lines = billboard.lines();
        for (int i = 0; i < Math.min(lines.size(), LINE_SLOTS); i++) {
            addLineButton(FIRST_LINE_SLOT + i, i, lines.get(i), lines.size());
        }

        fillEmpty(FILLER);
    }

    private void addNudgeButton(int slot, String axis, double value, Marker copy, int dx, int dy, int dz) {
        addButton(slot, item(Material.AMETHYST_SHARD, "Nudge nearest copy " + axis + String.format(": %.2f", value), NamedTextColor.AQUA,
                line("Moves only the placed copy closest to you"),
                hint("Left / right click: +0.1 / -0.1"), hint("Shift: +1 / -1")), event -> {
            event.setCancelled(true);
            if (!copy.isValid()) {
                Utils.runLater(this::setup);
                return;
            }
            double delta = step(event, 0.1, 1);
            BillboardManager.getInstance().nudge(copy, dx * delta, dy * delta, dz * delta);
            Utils.runLater(this::setup);
        });
    }

    private void addLineButton(int slot, int index, BillboardLine billboardLine, int total) {
        Material material = billboardLine instanceof AnimatedLine ? Material.FIREWORK_STAR : Material.PAPER;
        String name = "Line " + (index + 1) + " (" + billboardLine.getTypeId() + ")";
        addButton(slot, item(material, name, NamedTextColor.WHITE,
                billboardLine.render(player, 0),
                Component.empty(),
                hint("Click: edit as JSON"),
                hint("Shift + left: move up"),
                hint("Shift + right: move down")), event -> {
            event.setCancelled(true);
            ClickType click = event.getClick();
            if (click == ClickType.SHIFT_LEFT && index > 0) {
                moveLine(index, index - 1);
                return;
            }
            if (click == ClickType.SHIFT_RIGHT && index < total - 1) {
                moveLine(index, index + 1);
                return;
            }
            if (click.isShiftClick()) return;
            openJson(name, BillboardLine.CODEC, billboardLine,
                    updated -> editLines(lines -> lines.set(index, updated)),
                    () -> editLines(lines -> lines.remove(index)));
        });
    }

    private static double step(InventoryClickEvent event, double small, double large) {
        double amount = event.getClick().isShiftClick() ? large : small;
        return event.getClick().isRightClick() ? -amount : amount;
    }

    private void addLine(BillboardLine line) {
        editLines(lines -> lines.add(line));
    }

    private void moveLine(int from, int to) {
        editLines(lines -> lines.add(to, lines.remove(from)));
    }

    private void editLines(Consumer<List<BillboardLine>> edit) {
        apply(current -> {
            List<BillboardLine> lines = new ArrayList<>(current.lines());
            edit.accept(lines);
            return current.withLines(lines);
        });
    }

    private void apply(UnaryOperator<Billboard> change) {
        CommandFeedback.failed(player, BillboardManager.getInstance().update(billboardId, change));
        showInventory(player);
    }

    private <T> void openJson(String title, Codec<T> codec, T current, Consumer<T> onSave, @Nullable Runnable onDelete) {
        Dialog dialog = JsonEntryDialog.of(title, codec, onSave, onDelete, () -> showInventory(player)).build(player, current);
        player.closeInventory();
        player.showDialog(dialog);
    }
}
