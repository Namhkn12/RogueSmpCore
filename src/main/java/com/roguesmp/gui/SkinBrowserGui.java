package com.roguesmp.gui;

import com.roguesmp.registry.SkinRegistry;
import com.roguesmp.utils.Utils;
import dev.jorel.commandapi.CommandAPICommand;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class SkinBrowserGui extends BaseGui {

    private final List<String> skinIds;
    private int currentPage = 0;

    // 4 rows (9-1) * 7 columns (9-2) = 28 items per page
    private static final int ITEMS_PER_PAGE = 28;

    public SkinBrowserGui() {
        super(Component.text("Skin Browser"), 6);
        this.skinIds = new ArrayList<>(SkinRegistry.getInstance().getSkinIds());
    }

    @Override
    public void setup() {
        clearUi();

        drawBorder();

        int start = currentPage * ITEMS_PER_PAGE;
        int itemsPlaced = 0;

        for (int r = 1; r <= 4; r++) {
            for (int c = 1; c <= 7; c++) {

                int listIndex = start + itemsPlaced;

                // Stop if we've run out of skins to display
                if (listIndex >= skinIds.size()) break;

                String skinId = skinIds.get(listIndex);
                ItemStack head = SkinRegistry.getInstance().getHead(skinId);

                // Add the button directly at the row/column coordinate
                addButton(r, c, head, event -> {
                    event.setCancelled(true);
                    Player player = (Player) event.getWhoClicked();
                    player.getInventory().addItem(head.clone());
                });

                itemsPlaced++;
            }

            // Safety break for the outer loop if we finished the list
            if (start + itemsPlaced >= skinIds.size()) break;
        }

        addNavigation();
        addInfoItem();

        fillEmpty();
    }

    private void addNavigation() {
        if (currentPage > 0) {
            addButton(5, 3, PREV_PAGE_BUTTON, event -> {
                event.setCancelled(true);
                currentPage--;
                setup();
            });
        }

        // Check if there is at least one item on the next page
        if ((currentPage + 1) * ITEMS_PER_PAGE < skinIds.size()) {
            addButton(5, 5, NEXT_PAGE_BUTTON, event -> {
                event.setCancelled(true);
                currentPage++;
                setup();
            });
        }
    }

    private void drawBorder() {
        // Top and Bottom rows
        for (int col = 0; col < 9; col++) {
            addButton(0, col, FILLER_BLACK, ClickHandler.noAction());
            addButton(5, col, FILLER_BLACK, ClickHandler.noAction());
        }
        // Left and Right columns
        for (int row = 1; row < 5; row++) {
            addButton(row, 0, FILLER_BLACK, ClickHandler.noAction());
            addButton(row, 8, FILLER_BLACK, ClickHandler.noAction());
        }
    }

    private void addInfoItem() {
        ItemStack info = new ItemStack(Material.WRITTEN_BOOK);

        // Set Item Name
        info.setData(DataComponentTypes.ITEM_NAME, Utils.fromString("<aqua><bold>Cách thêm skin mới"));

        // Set Lore (List of Components)
        List<Component> lore = List.of(
                Component.empty(),
                Utils.fromString("<!i><gray>Dùng lệnh này để thêm:"),
                Component.empty(),
                Utils.fromString("<!i><yellow>/skinfetch <mineskin_uuid> <id>"),
                Component.empty(),
                Utils.fromString("<!i><dark_gray>Ví dụ:"),
                Utils.fromString("<!i><dark_gray>/skinfetch 691a14f8153742b8abf4247ca6b9140a catgirl_uwu"),
                Component.empty(),
                Utils.fromString("<!i><dark_gray>id trùng nhau sẽ bị ghi đè."),
                Utils.fromString("<!i><dark_gray>uuid là phần này: https://minesk.in/<gray>691a14f8153742b8abf4247ca6b9140a")
        );
        info.setData(DataComponentTypes.LORE, ItemLore.lore(lore));

        // Place it in the bottom-right corner of the border
        addButton(5, 8, info, ClickHandler.noAction());
    }

    public static void registerCommand() {
        new CommandAPICommand("skinbrowser")
                .executesPlayer((player, args) -> {
                    new SkinBrowserGui().showInventory(player);
                })
                .register();
    }
}
