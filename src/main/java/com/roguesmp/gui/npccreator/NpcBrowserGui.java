package com.roguesmp.gui.npccreator;

import com.roguesmp.gui.BaseGui;
import com.roguesmp.npc.BaseNpc;
import com.roguesmp.registry.Registries;
import com.roguesmp.utils.Utils;
import com.roguesmp.utils.dialog.DialogBuilder;
import dev.jorel.commandapi.CommandAPICommand;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Inventory-based, searchable browser for every registered {@link BaseNpc} - the read-only-list
 * counterpart to {@link NpcCreatorGui}'s dialog editor, same paginated-chest shape as
 * {@code gui.loottablecreator.LootTableBrowserGui}. Left-click an entry to edit it, right-click to
 * spawn it at your location (same duality as {@code gui.entitycreator.EntityBrowserGui}, just
 * mirrored since editing is the primary action here).
 * <p>
 * "+ Tạo mới" lives in the top row ({@link #CREATE_NEW_SLOT}) alongside search/guide, not among the
 * entries - so it's always visible in the same spot regardless of page, and entries get the full
 * {@link #PAGE_SIZE} content slots per page.
 */
public class NpcBrowserGui extends BaseGui {

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

    public NpcBrowserGui(Player player) {
        super(Component.text("NPC Browser", NamedTextColor.DARK_GRAY), 6);
        this.player = player;

        this.allIds = new ArrayList<>(Registries.NPC.getAll().keySet());
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
            new NpcCreatorGui().openMainDialog(player);
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
                Utils.text("Click \"+ Tạo mới\" để tạo NPC mới.", NamedTextColor.GREEN),
                Utils.text("Chuột trái một NPC để chỉnh sửa.", NamedTextColor.GREEN),
                Utils.text("Chuột phải để spawn tại vị trí của bạn.", NamedTextColor.GREEN)
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
                if (event.getClick() == ClickType.RIGHT) {
                    spawnNpc(id);
                } else {
                    openEditor(id);
                }
            });
            slot++;
        }
    }

    @Override
    public void onClickBottomInventory(InventoryClickEvent event) {
        event.setCancelled(true);
    }

    private void openEditor(String id) {
        BaseNpc npc = Registries.NPC.get(id);
        if (npc == null) {
            player.sendMessage(Utils.text("NPC '" + id + "' không còn tồn tại.", NamedTextColor.RED));
            updateFilters();
            setup();
            return;
        }
        player.closeInventory();
        NpcCreatorGui.editExisting(id, npc).openMainDialog(player);
    }

    private void spawnNpc(String id) {
        BaseNpc npc = Registries.NPC.get(id);
        if (npc == null) {
            player.sendMessage(Utils.text("NPC '" + id + "' không còn tồn tại.", NamedTextColor.RED));
            updateFilters();
            setup();
            return;
        }
        npc.spawn(player.getLocation());
        player.sendMessage(Utils.text("Đã spawn NPC '" + id + "'.", NamedTextColor.GREEN));
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
        item.setData(DataComponentTypes.LORE, ItemLore.lore(List.of(Utils.text("Tạo một NPC mới.", NamedTextColor.GRAY))));
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
        Dialog dialog = DialogBuilder.create(Component.text("Tìm kiếm NPC"))
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
        BaseNpc npc = Registries.NPC.get(id);

        Material egg = npc == null ? null : Bukkit.getItemFactory().getSpawnEgg(npc.getEntityType());
        ItemStack item = ItemStack.of(egg == null ? Material.PAPER : egg);

        String displayName = npc == null || npc.getName() == null ? id : npc.getName();
        item.setData(DataComponentTypes.ITEM_NAME, Utils.fromString(displayName));

        List<Component> lore = new ArrayList<>();
        lore.add(Utils.text(id, NamedTextColor.DARK_GRAY));
        if (npc != null) {
            lore.add(Utils.text("Entity Type: " + npc.getEntityType().name(), NamedTextColor.GRAY));
            lore.add(Utils.text("Actions: " + npc.getActions().size(), NamedTextColor.GRAY));
        }
        lore.add(Component.empty());
        lore.add(Utils.text("Trái-click để chỉnh sửa.", NamedTextColor.GREEN));
        lore.add(Utils.text("Phải-click để spawn.", NamedTextColor.YELLOW));
        item.setData(DataComponentTypes.LORE, ItemLore.lore(lore));

        return item;
    }

    public static void registerCommand() {
        new CommandAPICommand("smpnpcbrowser")
                .executesPlayer((player, args) -> {
                    new NpcBrowserGui(player).showInventory(player);
                })
                .register();
    }
}
