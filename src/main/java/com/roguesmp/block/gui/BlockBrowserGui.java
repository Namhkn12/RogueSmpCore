package com.roguesmp.block.gui;

import com.roguesmp.block.BlockData;
import com.roguesmp.block.BlockVisual;
import com.roguesmp.gui.BaseGui;
import com.roguesmp.registry.Registries;
import com.roguesmp.utils.Utils;
import com.roguesmp.utils.dialog.DialogBuilder;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import io.papermc.paper.dialog.Dialog;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

public class BlockBrowserGui extends BaseGui {

    private static final int PER_PAGE = 28;
    private static final int SEARCH_SLOT = 8;

    private final Player player;
    private int page;
    private String searchQuery;

    public BlockBrowserGui(Player player) {
        this(player, 0);
    }

    public BlockBrowserGui(Player player, String query) {
        this(player, 0);
        this.searchQuery = query;
    }

    public BlockBrowserGui(Player player, int page) {
        super(Component.text("Custom Blocks"), 6);
        this.player = player;
        this.page = page;
    }

    @Override
    public void setup() {
        clearUi();
        for (int r = 0; r <= 5; r++) {
            for (int c = 0; c < 9; c++) {
                if (r == 0 || r == 5 || c == 0 || c == 8) addButton(r, c, FILLER_BLACK, ClickHandler.noAction());
            }
        }
        addButton(SEARCH_SLOT, searchButton(), event -> {
            event.setCancelled(true);
            if (event.getClick().isShiftClick()) {
                searchQuery = null;
                page = 0;
                setup();
                return;
            }
            openSearchDialog();
        });

        List<Map.Entry<String, BlockData>> blocks = filteredBlocks();
        int pages = Math.max(1, (blocks.size() + PER_PAGE - 1) / PER_PAGE);
        page = Math.min(page, pages - 1);
        int index = page * PER_PAGE;
        for (int r = 1; r <= 4; r++) {
            for (int c = 1; c <= 7; c++) {
                if (index >= blocks.size()) break;
                Map.Entry<String, BlockData> entry = blocks.get(index++);
                String id = entry.getKey();
                addButton(r, c, icon(id, entry.getValue()), event -> {
                    event.setCancelled(true);
                    player.closeInventory();
                    new BlockJsonDialog(id, page).open(player);
                });
            }
        }
        if (page > 0) addButton(5, 3, PREV_PAGE_BUTTON, event -> {
            event.setCancelled(true);
            page--;
            setup();
        });
        if (page < pages - 1) addButton(5, 5, NEXT_PAGE_BUTTON, event -> {
            event.setCancelled(true);
            page++;
            setup();
        });
        fillEmpty(FILLER);
    }

    private List<Map.Entry<String, BlockData>> filteredBlocks() {
        String query = searchQuery == null || searchQuery.isBlank() ? null : Utils.removeVietnameseTones(searchQuery.toLowerCase().trim());
        return Registries.BLOCK.getAll().entrySet().stream()
                .filter(entry -> query == null || matches(entry.getKey(), entry.getValue(), query))
                .sorted(Map.Entry.comparingByKey())
                .toList();
    }

    private static boolean matches(String id, BlockData block, String query) {
        if (Utils.removeVietnameseTones(id.toLowerCase()).contains(query)) return true;
        String name = PlainTextComponentSerializer.plainText().serialize(Utils.fromString(block.properties().displayName()));
        return Utils.removeVietnameseTones(name.toLowerCase()).contains(query);
    }

    private void openSearchDialog() {
        Dialog dialog = DialogBuilder.create(Component.text("Tìm kiếm block"))
                .addTextInput("value", Component.text("Nhập ID hoặc tên block..."), b -> b.initial(searchQuery == null ? "" : searchQuery))
                .confirmation()
                .yesButton(Component.text("Tìm kiếm"), null, (response, audience) -> {
                    searchQuery = response.getText("value");
                    page = 0;
                    Utils.runLater(() -> showInventory(player));
                })
                .noButton(Component.text("Huỷ"), null, (response, audience) -> Utils.runLater(() -> showInventory(player)))
                .build();

        player.closeInventory();
        player.showDialog(dialog);
    }

    private ItemStack searchButton() {
        ItemStack item = ItemStack.of(Material.COMPASS);
        item.setData(DataComponentTypes.ITEM_NAME, Component.text("Tìm kiếm theo ID hoặc tên", NamedTextColor.GOLD));
        item.setData(DataComponentTypes.LORE, ItemLore.lore(List.of(
                Utils.text("Từ khóa hiện tại: " + (searchQuery == null || searchQuery.isBlank() ? "Trống" : searchQuery), NamedTextColor.YELLOW),
                Utils.text("Click để đổi từ khóa.", NamedTextColor.GRAY),
                Utils.text("Shift-Click để xoá bộ lọc.", NamedTextColor.RED)
        )));
        return item;
    }

    private static ItemStack icon(String id, BlockData block) {
        ItemStack stack = BlockVisual.displayStack(block.properties().model());
        String name = block.properties().displayName().isEmpty() ? id : block.properties().displayName();
        stack.setData(DataComponentTypes.ITEM_NAME, Utils.fromString(name));
        stack.setData(DataComponentTypes.LORE, ItemLore.lore(List.of(
                Utils.fromString("<!i><gray>ID: <white>" + id),
                Utils.fromString("<!i><gray>Type: <white>" + block.type().key()),
                Utils.fromString("<!i><gray>Model: <white>" + block.properties().model().asString()),
                Component.empty(),
                Utils.fromString("<!i><yellow>▶ Click để xem/sửa JSON")
        )));
        return stack;
    }

    @Override
    public void onClickBottomInventory(InventoryClickEvent event) {
        event.setCancelled(true);
    }
}
