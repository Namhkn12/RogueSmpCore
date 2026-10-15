package com.roguesmp.region.editor;

import com.roguesmp.gui.BaseGui;
import com.roguesmp.region.RegionSelection;
import com.roguesmp.region.ServerRegionManager;
import com.roguesmp.utils.Utils;
import com.roguesmp.utils.dialog.DialogBuilder;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import io.papermc.paper.dialog.Dialog;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.roguesmp.region.editor.RegionGuiItems.describeRegion;
import static com.roguesmp.region.editor.RegionGuiItems.hint;
import static com.roguesmp.region.editor.RegionGuiItems.item;
import static com.roguesmp.region.editor.RegionGuiItems.line;
import static com.roguesmp.region.editor.RegionGuiItems.visibleFlags;

public class RegionBrowserGui extends BaseGui {

    private static final int PAGE_SIZE = 36;
    private static final int NEW_SLOT = 0;
    private static final int HEIGHT_SLOT = 2;
    private static final int WAND_SLOT = 4;
    private static final int GLOBAL_SLOT = 6;

    private final Player player;
    private final World world;
    private int currentPage = 0;

    public RegionBrowserGui(Player player) {
        super(Component.text("Regions: " + player.getWorld().getName()), 6);
        this.player = player;
        this.world = player.getWorld();
    }

    @Override
    public void setup() {
        clearUi();
        for (int i = 0; i < 9; i++) {
            addButton(i, FILLER_BLACK, ClickHandler.noAction());
            addButton(45 + i, FILLER_BLACK, ClickHandler.noAction());
        }

        addButton(NEW_SLOT, item(Material.EMERALD, "Create or merge region", NamedTextColor.GREEN,
                line("Creates a region from your selection"),
                line("An existing id merges the selection into it"), hint("Click")), event -> {
            event.setCancelled(true);
            openCreateDialog();
        });

        boolean fullHeight = RegionSelections.getInstance().isFullHeight(player);
        addButton(HEIGHT_SLOT, item(fullHeight ? Material.LIME_DYE : Material.GRAY_DYE, "Full height selection: " + (fullHeight ? "ON" : "OFF"),
                fullHeight ? NamedTextColor.GREEN : NamedTextColor.GRAY,
                line("ON: selections span the whole world height"), hint("Click to toggle")), event -> {
            event.setCancelled(true);
            RegionSelections.getInstance().toggleFullHeight(player);
            setup();
        });

        addButton(WAND_SLOT, item(Material.BLAZE_ROD, "Get region wand", NamedTextColor.GOLD,
                line("Right click: add corner"), line("Left click: undo corner"), line("Sneak + right click: this menu")), event -> {
            event.setCancelled(true);
            player.getInventory().addItem(RegionWand.create());
        });

        ProtectedRegion global = ServerRegionManager.getInstance().global(world);
        int globalFlags = global == null ? 0 : visibleFlags(global).size();
        addButton(GLOBAL_SLOT, item(Material.FILLED_MAP, ServerRegionManager.GLOBAL_ID, NamedTextColor.LIGHT_PURPLE,
                line("Applies to the whole world"), line("Flags: " + globalFlags), hint("Click to edit flags")), event -> {
            event.setCancelled(true);
            Utils.runLater(() -> new RegionFlagGui(player, ServerRegionManager.GLOBAL_ID).showInventory(player));
        });

        List<Map.Entry<String, List<ProtectedRegion>>> regions = new ArrayList<>(ServerRegionManager.getInstance().regions(world).entrySet());

        int totalPages = Math.max(1, (int) Math.ceil(regions.size() / (double) PAGE_SIZE));
        currentPage = Math.min(currentPage, totalPages - 1);

        if (currentPage > 0) {
            addButton(5, 3, PREV_PAGE_BUTTON, event -> {
                event.setCancelled(true);
                currentPage--;
                setup();
            });
        }
        if (currentPage < totalPages - 1) {
            addButton(5, 5, NEXT_PAGE_BUTTON, event -> {
                event.setCancelled(true);
                currentPage++;
                setup();
            });
        }

        int from = currentPage * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, regions.size());
        int slot = 9;
        for (Map.Entry<String, List<ProtectedRegion>> entry : regions.subList(from, to)) {
            String id = entry.getKey();
            addButton(slot++, regionButton(id, entry.getValue()), event -> {
                event.setCancelled(true);
                if (event.getClick() == ClickType.RIGHT) {
                    ServerRegionManager.getInstance().teleport(player, id);
                    return;
                }
                Utils.runLater(() -> new RegionEditorGui(player, id).showInventory(player));
            });
        }
        fillEmpty(FILLER);
    }

    private ItemStack regionButton(String id, List<ProtectedRegion> pieces) {
        ProtectedRegion primary = ServerRegionManager.getInstance().get(world, id);
        return item(Material.GRASS_BLOCK, id, NamedTextColor.AQUA,
                line(describeRegion(pieces)),
                line("Priority: " + primary.getPriority() + "  Flags: " + visibleFlags(primary).size()),
                hint("Left click: edit"),
                hint("Right click: teleport"));
    }

    private void openCreateDialog() {
        RegionSelection selection = RegionSelections.getInstance().current(player);
        if (selection == null || !selection.isValid()) {
            player.sendMessage(Utils.fromString("<red>Select at least two corners with the region wand first."));
            return;
        }

        Dialog dialog = DialogBuilder.create(Component.text("Create or merge region"))
                .addTextBody(Component.text("Use an existing id to merge the selection into that region.", NamedTextColor.GRAY))
                .addTextInput("region_id", Component.text("Region id"))
                .confirmation()
                .yesButton(Component.text("Confirm"), null, (response, audience) -> {
                    String id = response.getText("region_id") == null ? "" : response.getText("region_id").trim();
                    Utils.runLater(() -> createOrMerge(id, selection));
                })
                .noButton(Component.text("Cancel"), null, (response, audience) -> Utils.runLater(() -> showInventory(player)))
                .build();

        player.closeInventory();
        player.showDialog(dialog);
    }

    private void createOrMerge(String id, RegionSelection selection) {
        boolean exists = ServerRegionManager.getInstance().get(world, id) != null;
        if (!exists && (!ProtectedRegion.isValidId(id) || id.startsWith("__"))) {
            player.sendMessage(Utils.fromString("<red>Invalid id. Use letters, digits and _ , ' - + /"));
            showInventory(player);
            return;
        }

        ServerRegionManager.getInstance().merge(world, id, selection);
        ServerRegionManager.getInstance().saveChanges(world);
        RegionSelections.getInstance().clear(player);
        player.sendMessage(Utils.fromString(exists ? "<green>Selection merged into '" + id + "'." : "<green>Region '" + id + "' created."));
        new RegionEditorGui(player, id).showInventory(player);
    }
}
