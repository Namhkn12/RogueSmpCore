package com.roguesmp.gui;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.item.component.ItemComponentKeys;
import com.roguesmp.item.BaseItem;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.registry.ItemRegistry;
import com.roguesmp.registry.Registries;
import com.roguesmp.utils.Utils;
import com.roguesmp.utils.dialog.DialogBuilder;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.StringArgument;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ItemBrowser extends BaseGui {
    private final List<Map.Entry<String, BaseItem>> allEntries;
    private List<Map.Entry<String, BaseItem>> filteredEntries;

    private final ItemStack infoBook = ItemStack.of(Material.BOOK);
    private final ItemStack searchButton = ItemStack.of(Material.COMPASS);

    private final int pageSize = 36;
    private int totalPages;
    private int currentPage = 0;
    private String searchQuery = null;

    public ItemBrowser() {
        super(Utils.fromString("Item Browser"), 6);

        this.allEntries = new ArrayList<>(Registries.ITEM.getAll().entrySet());
        this.allEntries.sort(Map.Entry.comparingByKey());
        this.filteredEntries = new ArrayList<>(allEntries);

        infoBook.setData(DataComponentTypes.ITEM_NAME, Component.text("Item Browser", NamedTextColor.GREEN));
        infoBook.setData(DataComponentTypes.LORE, ItemLore.lore(List.of(
                Utils.text("Bấm vào một item để nhận", NamedTextColor.GREEN),
                Utils.text("Shift-Click để nhận stack.", NamedTextColor.GREEN))
        ));

        updateSearchFilters();
    }

    @Override
    public void setup() {
        clearUi();

        searchButton.setData(DataComponentTypes.ITEM_NAME, Component.text("Tìm kiếm Vật phẩm", NamedTextColor.GOLD));
        searchButton.setData(DataComponentTypes.LORE, ItemLore.lore(List.of(
                Utils.text("Từ khóa hiện tại: " + (searchQuery == null ? "Trống" : searchQuery), NamedTextColor.YELLOW),
                Utils.text("Bấm vào để thay đổi từ khóa", NamedTextColor.GRAY),
                Utils.text("Shift-Click để Xóa bộ lọc", NamedTextColor.RED),
                Component.empty(),
                Utils.text("Có thể tìm kiếm qua id, tên vật phẩm (tìm kiếm không dấu)")
        )));

        for (int i = 0; i < 9; i++) {
            this.addButton(0, i, FILLER_BLACK, ClickHandler.noAction());
            this.addButton(5, i, FILLER_BLACK, ClickHandler.noAction());
        }

        this.addButton(0, 4, infoBook, ClickHandler.noAction());

        this.addButton(0, 8, searchButton, event -> {
            event.setCancelled(true);
            Player player = (Player) event.getWhoClicked();

            if (event.getClick().isShiftClick()) {
                this.searchQuery = null;
                this.updateSearchFilters();
                this.setup();
                return;
            }

            player.closeInventory();
            openSearchDialog(player);
        });

        if (totalPages > 1) {
            if (currentPage > 0) {
                this.addButton(5, 3, PREV_PAGE_BUTTON, click -> {
                    click.setCancelled(true);
                    currentPage--;
                    this.setup();
                });
            }

            if (currentPage < totalPages - 1) {
                this.addButton(5, 5, NEXT_PAGE_BUTTON, click -> {
                    click.setCancelled(true);
                    currentPage++;
                    this.setup();
                });
            }
        }

        var pageEntries = getPage(currentPage);
        int i = 9;
        for (var entry : pageEntries) {
            ItemStack itemStack = entry.getValue().generateItemStack(null, 1);
            ItemLore itemLore = itemStack.getData(DataComponentTypes.LORE);
            List<Component> itemLoreComp = itemLore == null ? new ArrayList<>() : new ArrayList<>(itemLore.lines());
            Component itemId = Utils.text("ID: " + entry.getKey(), NamedTextColor.DARK_GRAY);
            itemLoreComp.addFirst(itemId);
            itemStack.setData(DataComponentTypes.LORE, ItemLore.lore(itemLoreComp));
            this.addButton(i, itemStack, event -> {
                event.setCancelled(true);
                ItemStack toGive = entry.getValue().generateItemStack(PlayerManager.getInstance().getSmpPlayer(event.getWhoClicked().getUniqueId()), 1);
                if (event.getClick().isShiftClick()) {
                    toGive.setAmount(itemStack.getDataOrDefault(DataComponentTypes.MAX_STACK_SIZE, 1));
                    event.getWhoClicked().getInventory().addItem(toGive);
                    return;
                }
                event.getWhoClicked().getInventory().addItem(toGive);
            });
            i++;
        }
        this.fillEmpty();
    }

    public List<Map.Entry<String, BaseItem>> getPage(int page) {
        int fromIndex = page * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, filteredEntries.size());

        if (fromIndex >= filteredEntries.size() || fromIndex < 0) {
            return Collections.emptyList();
        }

        return filteredEntries.subList(fromIndex, toIndex);
    }

    @Override
    public void onClickBottomInventory(InventoryClickEvent event) {
        event.setCancelled(true);
    }

    private void updateSearchFilters() {
        if (searchQuery == null || searchQuery.isBlank()) {
            this.filteredEntries = new ArrayList<>(allEntries);
        } else {
            // Normalize the search query
            String query = Utils.removeVietnameseTones(searchQuery.toLowerCase().trim());
            this.filteredEntries = new ArrayList<>();

            for (var entry : allEntries) {
                String itemId = Utils.removeVietnameseTones(entry.getKey().toLowerCase());
                BaseItem baseItem = entry.getValue();

                // 1. Check item_id match
                if (itemId.contains(query)) {
                    filteredEntries.add(entry);
                    continue;
                }

                // 2. Check Display Name match using Adventure plain text serializer
                var nameComp = baseItem.getComponent(ItemComponentKeys.ITEM_NAME);
                if (nameComp != null) {
                    Component name = Utils.fromString(nameComp.value());
                    String plainName = PlainTextComponentSerializer.plainText().serialize(name);
                    String normalizedName = Utils.removeVietnameseTones(plainName.toLowerCase());
                    if (normalizedName.contains(query)) {
                        filteredEntries.add(entry);
                        continue;
                    }
                }
            }
        }
        this.totalPages = (int) Math.ceil((double) filteredEntries.size() / pageSize);
        this.currentPage = 0;
    }

    private void openSearchDialog(Player p) {
        Dialog dialog = DialogBuilder.create(Component.text("Tìm kiếm"))
                .addTextInput("search_param", Component.text("Nhập từ khóa cần tìm..."))
                .confirmation()
                .yesButton(Component.text("Tìm kiếm"), null, (response, audience) -> {
                    String searchParam = response.getText("search_param");

                    this.searchQuery = searchParam;
                    this.updateSearchFilters();

                    Utils.runLater(() -> this.showInventory(p));
                })
                .noButton(Component.text("Hủy bỏ"), null, (response, audience) -> {
                    Utils.runLater(() -> this.showInventory(p));
                })
                .build();

        p.showDialog(dialog);
    }

    public static void registerCommand() {
        new CommandAPICommand("smpitem")
                .withSubcommand(new CommandAPICommand("give")
                        .withArguments(new StringArgument("item_id"), new IntegerArgument("amount"))
                        .executesPlayer((player, commandArguments) -> {
                            BaseItem template = ItemRegistry.getInstance().getBaseItem((String) commandArguments.get("item_id"));
                            if (template == null) {
                                player.sendMessage("No item with that id");
                                return;
                            }
                            player.getInventory().addItem(template.generateItemStack(PlayerManager.getInstance().getSmpPlayer(player.getUniqueId()), (Integer) commandArguments.get("amount")));
                        })
                )
                .withSubcommand(new CommandAPICommand("view")
                        .executesPlayer((player, commandArguments) -> {
                            new ItemBrowser().showInventory(player);
                        })
                )
                .withSubcommand(new CommandAPICommand("blacksmith")
                        .executesPlayer((player, commandArguments) -> {
                            new BlacksmithGui(player).showInventory(player);
                        }))
                .register();
    }
}
