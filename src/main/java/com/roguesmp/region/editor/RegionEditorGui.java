package com.roguesmp.region.editor;

import com.roguesmp.gui.BaseGui;
import com.roguesmp.region.RegionSelection;
import com.roguesmp.region.ServerRegionManager;
import com.roguesmp.utils.Utils;
import com.roguesmp.utils.dialog.DialogBuilder;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import io.papermc.paper.dialog.Dialog;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.List;
import java.util.function.Consumer;

import static com.roguesmp.region.editor.RegionGuiItems.describeRegion;
import static com.roguesmp.region.editor.RegionGuiItems.describeShape;
import static com.roguesmp.region.editor.RegionGuiItems.hint;
import static com.roguesmp.region.editor.RegionGuiItems.item;
import static com.roguesmp.region.editor.RegionGuiItems.line;
import static com.roguesmp.region.editor.RegionGuiItems.visibleFlags;

public class RegionEditorGui extends BaseGui {

    private static final int BACK_SLOT = 0;
    private static final int INFO_SLOT = 4;
    private static final int DELETE_SLOT = 8;
    private static final int MERGE_SLOT = 10;
    private static final int REPLACE_SLOT = 11;
    private static final int PRIORITY_SLOT = 13;
    private static final int FLAGS_SLOT = 15;
    private static final int TELEPORT_SLOT = 16;
    private static final int SHAPES_START = 18;
    private static final int MAX_SHAPES = 18;

    private final Player player;
    private final World world;
    private final String regionId;

    public RegionEditorGui(Player player, String regionId) {
        super(Component.text("Region: " + regionId), 4);
        this.player = player;
        this.world = player.getWorld();
        this.regionId = regionId;
        RegionVisualizer.getInstance().focus(player, regionId);
    }

    @Override
    public void setup() {
        List<ProtectedRegion> pieces = ServerRegionManager.getInstance().pieces(world, regionId);
        ProtectedRegion primary = ServerRegionManager.getInstance().get(world, regionId);
        if (pieces.isEmpty() || primary == null) {
            player.closeInventory();
            return;
        }

        clearUi();
        for (int i = 0; i < 9; i++) addButton(i, FILLER_BLACK, ClickHandler.noAction());

        addButton(BACK_SLOT, item(Material.ARROW, "Back", NamedTextColor.GRAY), event -> {
            event.setCancelled(true);
            Utils.runLater(() -> new RegionBrowserGui(player).showInventory(player));
        });

        addButton(INFO_SLOT, item(Material.PAPER, regionId, NamedTextColor.AQUA,
                line(describeRegion(pieces)),
                line("Priority: " + primary.getPriority()),
                line("Flags: " + visibleFlags(primary).size())), ClickHandler.noAction());

        addButton(DELETE_SLOT, item(Material.TNT, "Delete region", NamedTextColor.RED, hint("Click to confirm")), event -> {
            event.setCancelled(true);
            openDeleteDialog();
        });

        addButton(MERGE_SLOT, item(Material.EMERALD, "Merge selection into region", NamedTextColor.GREEN,
                line("Adds your selection as another shape"),
                line("Use it to build L-shapes and other combined shaped areas")), event -> {
            event.setCancelled(true);
            if (pieces.size() >= MAX_SHAPES) {
                player.sendMessage(Utils.fromString("<red>A region can have at most " + MAX_SHAPES + " shapes."));
                return;
            }
            withSelection(selection -> ServerRegionManager.getInstance().merge(world, regionId, selection));
        });

        addButton(REPLACE_SLOT, item(Material.ORANGE_DYE, "Replace all shapes with selection", NamedTextColor.GOLD,
                line("Keeps flags, priority and members"),
                line("2 corners = box, 3+ corners = polygon")), event -> {
            event.setCancelled(true);
            withSelection(selection -> ServerRegionManager.getInstance().replaceShape(world, regionId, selection));
        });

        addButton(PRIORITY_SLOT, item(Material.COMPARATOR, "Priority: " + primary.getPriority(), NamedTextColor.YELLOW,
                hint("Left click: +1"), hint("Right click: -1"), hint("Shift: x10")), event -> {
            event.setCancelled(true);
            int step = event.getClick().isShiftClick() ? 10 : 1;
            ServerRegionManager.getInstance().setPriority(world, regionId, primary.getPriority() + (event.getClick().isRightClick() ? -step : step));
            ServerRegionManager.getInstance().saveChanges(world);
            setup();
        });

        addButton(FLAGS_SLOT, item(Material.OAK_SIGN, "Flags", NamedTextColor.LIGHT_PURPLE,
                line(visibleFlags(primary).size() + " flag(s)"), hint("Click to edit")), event -> {
            event.setCancelled(true);
            Utils.runLater(() -> new RegionFlagGui(player, regionId).showInventory(player));
        });

        addButton(TELEPORT_SLOT, item(Material.ENDER_PEARL, "Teleport", NamedTextColor.DARK_AQUA), event -> {
            event.setCancelled(true);
            ServerRegionManager.getInstance().teleport(player, regionId);
        });

        RegionVisualizer.Focus focus = RegionVisualizer.getInstance().focusOf(player);
        for (int i = 0; i < pieces.size() && i < MAX_SHAPES; i++) {
            int index = i;
            ProtectedRegion piece = pieces.get(i);
            boolean focused = focus != null && focus.regionId().equals(regionId) && focus.pieceIndex() == i;
            BlockVector3 min = piece.getMinimumPoint();
            BlockVector3 max = piece.getMaximumPoint();
            addButton(SHAPES_START + i, item(focused ? Material.ORANGE_CONCRETE : Material.LIGHT_BLUE_CONCRETE,
                    "Shape " + (i + 1), focused ? NamedTextColor.RED : NamedTextColor.AQUA,
                    line(describeShape(piece)),
                    line(min.x() + ", " + min.y() + ", " + min.z() + " to " + max.x() + ", " + max.y() + ", " + max.z()),
                    hint("Click: highlight"),
                    hint("Shift + right click: remove")), event -> {
                event.setCancelled(true);
                if (event.getClick() == ClickType.SHIFT_RIGHT) {
                    removePiece(index);
                    return;
                }
                RegionVisualizer.getInstance().focus(player, regionId, focused ? -1 : index);
                setup();
            });
        }
        fillEmpty(FILLER);
    }

    private void withSelection(Consumer<RegionSelection> action) {
        RegionSelection selection = RegionSelections.getInstance().current(player);
        if (selection == null || !selection.isValid()) {
            player.sendMessage(Utils.fromString("<red>Select at least two corners with the region wand first."));
            return;
        }

        action.accept(selection);
        ServerRegionManager.getInstance().saveChanges(world);
        RegionSelections.getInstance().clear(player);
        setup();
    }

    private void removePiece(int index) {
        if (!ServerRegionManager.getInstance().removePiece(world, regionId, index)) {
            player.sendMessage(Utils.fromString("<red>A region needs at least one shape."));
            return;
        }
        ServerRegionManager.getInstance().saveChanges(world);
        RegionVisualizer.getInstance().focus(player, regionId);
        setup();
    }

    private void openDeleteDialog() {
        Dialog dialog = DialogBuilder.create(Component.text("Delete region '" + regionId + "'?"))
                .addTextBody(Component.text("This permanently removes the region from WorldGuard.", NamedTextColor.GRAY))
                .confirmation()
                .yesButton(Component.text("Delete"), null, (response, audience) -> Utils.runLater(() -> {
                    ServerRegionManager.getInstance().remove(world, regionId);
                    ServerRegionManager.getInstance().saveChanges(world);
                    RegionVisualizer.getInstance().clearFocus(player);
                    new RegionBrowserGui(player).showInventory(player);
                }))
                .noButton(Component.text("Cancel"), null, (response, audience) -> Utils.runLater(() -> showInventory(player)))
                .build();

        player.closeInventory();
        player.showDialog(dialog);
    }
}
