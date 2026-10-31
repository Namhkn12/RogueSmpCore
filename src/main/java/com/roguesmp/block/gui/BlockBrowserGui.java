package com.roguesmp.block.gui;

import com.roguesmp.block.BlockData;
import com.roguesmp.block.BlockVisual;
import com.roguesmp.gui.BaseGui;
import com.roguesmp.registry.Registries;
import com.roguesmp.utils.Utils;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

public class BlockBrowserGui extends BaseGui {

    private static final int PER_PAGE = 28;

    private final Player player;
    private int page;

    public BlockBrowserGui(Player player) {
        this(player, 0);
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

        List<Map.Entry<String, BlockData>> blocks = Registries.BLOCK.getAll().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .toList();
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
