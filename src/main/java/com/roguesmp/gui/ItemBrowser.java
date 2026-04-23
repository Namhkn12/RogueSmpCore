package com.roguesmp.gui;

import com.roguesmp.gui.enchant.EnchantingGui;
import com.roguesmp.gui.gem.GemSocketGui;
import com.roguesmp.item.BaseItem;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.registry.ItemRegistry;
import com.roguesmp.utils.ItemStackUtils;
import com.roguesmp.utils.Utils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ItemBrowser extends BaseGui {
    private final List<Map.Entry<String, BaseItem>> entries;

    private final ItemStack filler = ItemStackUtils.hideTooltip(ItemStack.of(Material.BLACK_STAINED_GLASS_PANE));
    private final ItemStack nextPage = ItemStack.of(Material.ARROW);
    private final ItemStack prePage = ItemStack.of(Material.ARROW);
    private final ItemStack infoBook = ItemStack.of(Material.BOOK);

    private final int pageSize = 36;
    private final int totalPages;
    private int currentPage = 0;

    public ItemBrowser() {
        super(Utils.fromString("Item Browser"), 6);

        entries = new ArrayList<>(ItemRegistry.getInstance().getRegistry().entrySet());
        entries.sort(Map.Entry.comparingByKey());

        nextPage.setData(DataComponentTypes.ITEM_NAME, Component.text("Trang kế", NamedTextColor.GREEN));
        prePage.setData(DataComponentTypes.ITEM_NAME, Component.text("Trang trước", NamedTextColor.GREEN));
        infoBook.setData(DataComponentTypes.ITEM_NAME, Component.text("Item Browser", NamedTextColor.GREEN));
        infoBook.setData(DataComponentTypes.LORE, ItemLore.lore(Collections.singletonList(Component.text("Bấm vào một item để nhận", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false))));
        totalPages = getTotalPages();
    }

    @Override
    public void setup() {
        clearUi();
        for (int i = 0; i < 9; i++) {
            this.addButton(0, i, filler, ClickHandler.noAction());
            this.addButton(5, i, filler, ClickHandler.noAction());
        }
        this.addButton(0, 4, infoBook, ClickHandler.noAction());
        if (totalPages > 1) {
            if (currentPage > 0) {
                this.addButton(5, 3, prePage, click -> {
                    click.setCancelled(true);
                    currentPage--;
                    this.setup();
                });
            }

            // Show Next if not on last page
            if (currentPage < totalPages) {
                this.addButton(5, 5, nextPage, click -> {
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
            this.addButton(i, itemStack, event -> {
                event.getWhoClicked().getInventory().addItem(itemStack);
                event.setCancelled(true);
            });
            i++;
        }
        this.fillEmpty();
    }

    public List<Map.Entry<String, BaseItem>> getPage(int page) {
        int fromIndex = page * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, entries.size());

        if (fromIndex >= entries.size() || fromIndex < 0) {
            return Collections.emptyList(); // no results on this page
        }

        return entries.subList(fromIndex, toIndex);
    }

    private int getTotalPages() {
        return (int) Math.ceil((double) entries.size() / pageSize);
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
                .withSubcommand(new CommandAPICommand("gem")
                        .executesPlayer((player, commandArguments) -> {
                            new GemSocketGui(PlayerManager.getInstance().getSmpPlayer(player.getUniqueId())).showInventory(player);
                        }))
                .withSubcommand(new CommandAPICommand("enchant")
                        .executesPlayer((player, commandArguments) -> {
                            new EnchantingGui(PlayerManager.getInstance().getSmpPlayer(player.getUniqueId())).showInventory(player);
                        })
                )
                .withSubcommand(new CommandAPICommand("reload") // WILL CAUSE THE SERVER TO FREEZE
                        .executesPlayer((player1, commandArguments) -> {
                            Utils.runLater(() -> ItemRegistry.getInstance().loadFromFile());
                        }))
                .register();
    }

}
