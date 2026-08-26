package com.roguesmp.gui.loottablecreator;

import com.roguesmp.loot.LootTable;
import com.roguesmp.registry.Registries;
import com.roguesmp.utils.Utils;
import com.roguesmp.utils.dialog.DialogBuilder;
import dev.jorel.commandapi.CommandAPICommand;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import com.roguesmp.gui.BaseGui;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Inventory-based, searchable browser for every registered {@link LootTable} - the read-only-list
 * counterpart to {@link LootTableGuiCreator}'s dialog editor, same paginated-chest shape as
 * {@code gui.crafting.RecipeBrowserGui}. Since {@link LootTableGuiCreator} itself is dialog-based,
 * every click here that leaves this inventory ("+ Tạo mới", an entry) closes it first, same
 * inventory-to-dialog convention used everywhere else in this plugin.
 * <p>
 * "+ Tạo mới" lives in the top row ({@link #CREATE_NEW_SLOT}) alongside search/guide, not among the
 * entries - so it's always visible in the same spot regardless of page, and entries get the full
 * {@link #PAGE_SIZE} content slots per page.
 */
public class LootTableBrowserGui extends BaseGui {

    private static final int PAGE_SIZE = 36;
    private static final int CREATE_NEW_SLOT = 0;
    private static final int GUIDE_SLOT = 4;
    private static final int SEARCH_SLOT = 8;

    private final Player player;
    private final List<String> allIds;
    private List<String> filteredIds;

    private String searchQuery = null;
    private int currentPage = 0;
    private int totalPages;

    public LootTableBrowserGui(Player player) {
        super(Component.text("Loot Table Browser", NamedTextColor.DARK_GRAY), 6);
        this.player = player;

        this.allIds = new ArrayList<>(Registries.LOOT_TABLE.getAll().keySet());
        Collections.sort(allIds);
        updateFilters();
    }

    @Override
    public void setup() {
        clearUi();
        for (int i = 0; i < 9; i++) {
            addButton(i, FILLER_BLACK, ClickHandler.noAction());
        }
        for (int i = 45; i < 54; i++) {
            addButton(i, FILLER_BLACK, ClickHandler.noAction());
        }
        fillEmpty(FILLER);

        addButton(CREATE_NEW_SLOT, createNewButton(), event -> {
            event.setCancelled(true);
            player.closeInventory();
            new LootTableGuiCreator().openMainDialog(player);
        });

        addButton(SEARCH_SLOT, searchButton(), event -> {
            event.setCancelled(true);
            if (event.getClick().isShiftClick()) {
                searchQuery = null;
                updateFilters();
                setup();
                return;
            }
            openSearchDialog();
        });

        ItemStack guide = ItemStack.of(Material.BOOK);
        guide.setData(DataComponentTypes.ITEM_NAME, Component.text("Guide", NamedTextColor.GREEN));
        guide.setData(DataComponentTypes.LORE, ItemLore.lore(List.of(
                Utils.text("Click \"+ Tạo mới\" để tạo loot table mới.", NamedTextColor.GREEN),
                Utils.text("Click một loot table để chỉnh sửa.", NamedTextColor.GREEN)
        )));
        addButton(GUIDE_SLOT, guide, ClickHandler.noAction());

        if (totalPages > 1) {
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
        }

        int slot = 9;
        for (String id : getPage()) {
            addButton(slot, previewButtonFor(id), event -> {
                event.setCancelled(true);
                openEditor(id);
            });
            slot++;
        }
    }

    @Override
    public void onClickBottomInventory(InventoryClickEvent event) {
        event.setCancelled(true);
    }

    private void openEditor(String id) {
        LootTable table = Registries.LOOT_TABLE.get(id);
        if (table == null) {
            player.sendMessage(Utils.text("Loot table '" + id + "' không còn tồn tại.", NamedTextColor.RED));
            updateFilters();
            setup();
            return;
        }
        LootTableGuiCreator.editExisting(id, table).openMainDialog(player);
    }

    private void updateFilters() {
        if (searchQuery == null || searchQuery.isBlank()) {
            this.filteredIds = new ArrayList<>(allIds);
        } else {
            String query = searchQuery.toLowerCase().trim();
            this.filteredIds = new ArrayList<>();
            for (String id : allIds) {
                if (id.toLowerCase().contains(query)) filteredIds.add(id);
            }
        }
        this.totalPages = Math.max(1, (int) Math.ceil(filteredIds.size() / (double) PAGE_SIZE));
        this.currentPage = 0;
    }

    private List<String> getPage() {
        int from = currentPage * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, filteredIds.size());
        if (from >= filteredIds.size()) return Collections.emptyList();
        return filteredIds.subList(from, to);
    }

    private ItemStack createNewButton() {
        ItemStack item = ItemStack.of(Material.EMERALD);
        item.setData(DataComponentTypes.ITEM_NAME, Component.text("+ Tạo mới", NamedTextColor.GREEN));
        item.setData(DataComponentTypes.LORE, ItemLore.lore(List.of(Utils.text("Tạo một loot table mới.", NamedTextColor.GRAY))));
        return item;
    }

    private ItemStack searchButton() {
        ItemStack item = ItemStack.of(Material.COMPASS);
        item.setData(DataComponentTypes.ITEM_NAME, Component.text("Tìm kiếm theo ID", NamedTextColor.GOLD));
        item.setData(DataComponentTypes.LORE, ItemLore.lore(List.of(
                Utils.text("Từ khóa hiện tại: " + (searchQuery == null || searchQuery.isBlank() ? "Trống" : searchQuery), NamedTextColor.YELLOW),
                Utils.text("Click để đổi từ khóa.", NamedTextColor.GRAY),
                Utils.text("Shift-Click để xoá bộ lọc.", NamedTextColor.RED)
        )));
        return item;
    }

    private void openSearchDialog() {
        Dialog dialog = DialogBuilder.create(Component.text("Tìm kiếm loot table"))
                .addTextInput("value", Component.text("Nhập ID hoặc một phần ID..."), b -> b.initial(searchQuery == null ? "" : searchQuery))
                .confirmation()
                .yesButton(Component.text("Tìm kiếm"), null, (response, audience) -> {
                    searchQuery = response.getText("value");
                    updateFilters();
                    Utils.runLater(() -> showInventory(player));
                })
                .noButton(Component.text("Huỷ"), null, (response, audience) -> Utils.runLater(() -> showInventory(player)))
                .build();

        player.closeInventory();
        player.showDialog(dialog);
    }

    private ItemStack previewButtonFor(String id) {
        LootTable table = Registries.LOOT_TABLE.get(id);
        int poolCount = table == null ? 0 : table.getPools().size();

        ItemStack item = ItemStack.of(Material.CHEST);
        item.setData(DataComponentTypes.ITEM_NAME, Component.text(id, NamedTextColor.WHITE));
        item.setData(DataComponentTypes.LORE, ItemLore.lore(List.of(
                Utils.text(poolCount + " pool" + (poolCount == 1 ? "" : "s"), NamedTextColor.GRAY),
                Component.empty(),
                Utils.text("Click để chỉnh sửa.", NamedTextColor.GREEN)
        )));
        return item;
    }

    public static void registerCommand() {
        new CommandAPICommand("smploot")
                .executesPlayer((player, args) -> {
                    new LootTableBrowserGui(player).showInventory(player);
                })
                .register();
    }
}
