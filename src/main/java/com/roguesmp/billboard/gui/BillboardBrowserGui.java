package com.roguesmp.billboard.gui;

import com.roguesmp.billboard.Billboard;
import com.roguesmp.billboard.BillboardManager;
import com.roguesmp.codec.DataResult;
import com.roguesmp.command.CommandFeedback;
import com.roguesmp.gui.BaseGui;
import com.roguesmp.registry.Registries;
import com.roguesmp.utils.Utils;
import com.roguesmp.utils.dialog.DialogBuilder;
import io.papermc.paper.dialog.Dialog;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;
import java.util.List;
import java.util.Map;

import static com.roguesmp.gui.GuiItems.hint;
import static com.roguesmp.gui.GuiItems.item;
import static com.roguesmp.gui.GuiItems.line;

public class BillboardBrowserGui extends BaseGui {

    private static final int[] CONTENT_SLOTS = contentSlots();
    private static final int HEADER_SLOT = 4;
    private static final int CREATE_SLOT = 8;
    private static final int PREV_SLOT = 48;
    private static final int PAGE_SLOT = 49;
    private static final int NEXT_SLOT = 50;
    private static final int CLOSE_SLOT = 53;
    private static final int EMPTY_SLOT = 22;
    private static final int FILTER_SLOT = 0;
    private static final int SEARCH_SLOT = 1;
    private static final String FILTER_ALL = "All worlds";
    private static final String FILTER_HERE = "This world";
    private static final String FILTER_UNLOADED = "No loaded copies";

    private final Player player;
    private int currentPage = 0;
    private String filter = FILTER_ALL;
    private String query;

    public BillboardBrowserGui(Player player) {
        this(player, "");
    }

    public BillboardBrowserGui(Player player, String query) {
        super(Component.text("Billboards", NamedTextColor.DARK_AQUA), 6);
        this.player = player;
        this.query = query.trim().toLowerCase();
    }

    private static int[] contentSlots() {
        int[] slots = new int[28];
        int index = 0;
        for (int row = 1; row <= 4; row++) {
            for (int col = 1; col <= 7; col++) {
                slots[index++] = row * 9 + col;
            }
        }
        return slots;
    }

    @Override
    public void setup() {
        clearUi();
        for (int i = 0; i < 54; i++) {
            int row = i / 9;
            int col = i % 9;
            if (row == 0 || row == 5 || col == 0 || col == 8) addButton(i, FILLER_BLACK, ClickHandler.noAction());
        }

        Map<String, Set<String>> worldsById = new HashMap<>();
        BillboardManager.getInstance().getPlaced().values().forEach(entry -> worldsById
                .computeIfAbsent(entry.id(), id -> new TreeSet<>())
                .add(entry.instance().getAnchor().getWorld().getName()));

        List<String> filters = new ArrayList<>(List.of(FILTER_ALL, FILTER_HERE));
        worldsById.values().stream().flatMap(Set::stream).distinct().sorted().forEach(filters::add);
        filters.add(FILTER_UNLOADED);
        if (!filters.contains(filter)) filter = FILTER_ALL;

        String here = player.getWorld().getName();
        List<Map.Entry<String, Billboard>> billboards = Registries.BILLBOARD.getAll().entrySet().stream()
                .filter(entry -> query.isEmpty() || entry.getKey().contains(query))
                .filter(entry -> {
                    Set<String> worlds = worldsById.getOrDefault(entry.getKey(), Set.of());
                    return switch (filter) {
                        case FILTER_ALL -> true;
                        case FILTER_HERE -> worlds.contains(here);
                        case FILTER_UNLOADED -> worlds.isEmpty();
                        default -> worlds.contains(filter);
                    };
                })
                .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
                .toList();

        List<Component> filterLore = new ArrayList<>();
        filters.forEach(option -> filterLore.add(line((option.equals(filter) ? "> " : "  ") + option)));
        filterLore.add(hint("Left / right click: next / previous"));
        addButton(FILTER_SLOT, item(Material.HOPPER, "Filter: " + filter, NamedTextColor.YELLOW, filterLore.toArray(Component[]::new)), event -> {
            event.setCancelled(true);
            int direction = event.getClick().isRightClick() ? -1 : 1;
            filter = filters.get(Math.floorMod(filters.indexOf(filter) + direction, filters.size()));
            currentPage = 0;
            setup();
        });
        addButton(SEARCH_SLOT, item(Material.NAME_TAG, query.isEmpty() ? "Search" : "Search: " + query, NamedTextColor.YELLOW,
                line("Matches billboard ids containing the text"),
                hint("Click: search"), hint("Right click: clear")), event -> {
            event.setCancelled(true);
            if (event.getClick().isRightClick()) {
                query = "";
                currentPage = 0;
                setup();
                return;
            }
            openSearchDialog();
        });

        int totalPages = Math.max(1, (int) Math.ceil(billboards.size() / (double) CONTENT_SLOTS.length));
        currentPage = Math.min(currentPage, totalPages - 1);

        addButton(HEADER_SLOT, item(Material.OAK_SIGN, "Billboards", NamedTextColor.AQUA,
                line(billboards.size() + " billboard(s)")), ClickHandler.noAction());

        addButton(CREATE_SLOT, item(Material.EMERALD, "Create billboard", NamedTextColor.GREEN,
                line("Creates one and places a copy here"), hint("Click")), event -> {
            event.setCancelled(true);
            openCreateDialog();
        });

        addButton(CLOSE_SLOT, item(Material.BARRIER, "Close", NamedTextColor.RED), event -> {
            event.setCancelled(true);
            Utils.runLater(player::closeInventory);
        });

        if (currentPage > 0) {
            addButton(PREV_SLOT, PREV_PAGE_BUTTON, event -> {
                event.setCancelled(true);
                currentPage--;
                setup();
            });
        }
        if (currentPage < totalPages - 1) {
            addButton(NEXT_SLOT, NEXT_PAGE_BUTTON, event -> {
                event.setCancelled(true);
                currentPage++;
                setup();
            });
        }
        addButton(PAGE_SLOT, item(Material.PAPER, "Page " + (currentPage + 1) + "/" + totalPages, NamedTextColor.YELLOW), ClickHandler.noAction());

        if (billboards.isEmpty()) {
            addButton(EMPTY_SLOT, item(Material.STRUCTURE_VOID, query.isEmpty() && filter.equals(FILTER_ALL) ? "No billboards yet" : "No matching billboards", NamedTextColor.GRAY), ClickHandler.noAction());
        }

        int from = currentPage * CONTENT_SLOTS.length;
        List<Map.Entry<String, Billboard>> page = billboards.subList(from, Math.min(from + CONTENT_SLOTS.length, billboards.size()));
        for (int i = 0; i < page.size(); i++) {
            String id = page.get(i).getKey();
            Billboard billboard = page.get(i).getValue();
            Set<String> worlds = worldsById.getOrDefault(id, Set.of());
            long copyCount = BillboardManager.getInstance().copiesOf(id).size();
            addButton(CONTENT_SLOTS[i], item(Material.OAK_SIGN, id, NamedTextColor.WHITE,
                    line(copyCount + " loaded copy(ies)"),
                    line(worlds.isEmpty() ? "Not loaded in any world" : "In: " + String.join(", ", worlds)),
                    line(billboard.lines().size() + " line(s)"),
                    hint("Click: edit"),
                    hint("Shift + click: place here")), event -> {
                event.setCancelled(true);
                if (event.getClick().isShiftClick()) {
                    player.closeInventory();
                    BillboardManager.getInstance().place(id, player.getLocation());
                    player.sendMessage(Utils.fromString("<green>Placed billboard '" + id + "' at your position."));
                    return;
                }
                Utils.runLater(() -> new BillboardEditorGui(player, id, this).showInventory(player));
            });
        }
        fillEmpty(FILLER);
    }

    private void openSearchDialog() {
        Dialog dialog = DialogBuilder.create(Component.text("Search billboards"))
                .addTextInput("query", Component.text("Id contains"))
                .confirmation()
                .yesButton(Component.text("Search"), null, (response, audience) -> {
                    String raw = response.getText("query");
                    Utils.runLater(() -> {
                        query = raw == null ? "" : raw.trim().toLowerCase();
                        currentPage = 0;
                        showInventory(player);
                    });
                })
                .noButton(Component.text("Cancel"), null, (response, audience) -> Utils.runLater(() -> showInventory(player)))
                .build();

        player.closeInventory();
        player.showDialog(dialog);
    }

    private void openCreateDialog() {
        Dialog dialog = DialogBuilder.create(Component.text("Create billboard"))
                .addTextInput("billboard_id", Component.text("Billboard id"))
                .confirmation()
                .yesButton(Component.text("Create"), null, (response, audience) -> {
                    String raw = response.getText("billboard_id");
                    String id = raw == null ? "" : raw.trim().toLowerCase();
                    Utils.runLater(() -> create(id));
                })
                .noButton(Component.text("Cancel"), null, (response, audience) -> Utils.runLater(() -> showInventory(player)))
                .build();

        player.closeInventory();
        player.showDialog(dialog);
    }

    private void create(String id) {
        DataResult<Billboard> result = BillboardManager.getInstance().create(id, player.getLocation());
        if (CommandFeedback.failed(player, result)) {
            showInventory(player);
            return;
        }
        player.sendMessage(Utils.fromString("<green>Billboard '" + id + "' created."));
        new BillboardEditorGui(player, id, this).showInventory(player);
    }
}
