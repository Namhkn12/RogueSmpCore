package com.roguesmp.crafting.gui;

import com.roguesmp.crafting.quick.QuickCraftCandidate;
import com.roguesmp.crafting.quick.QuickCraftService;
import com.roguesmp.gui.BaseGui;
import com.roguesmp.gui.crafting.CraftingGui;
import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class QuickCraftListGui extends BaseGui {

    private static final int RESULT_COLS = 7;
    private static final int PAGE_SIZE = RESULT_COLS * 4;
    private static final int BOTTOM_ROW = 5;
    private static final int PREV_PAGE_COL = 3;
    private static final int BACK_COL = 4;
    private static final int NEXT_PAGE_COL = 5;

    private final Player player;
    private List<QuickCraftCandidate> candidates;
    private int page = 0;

    public QuickCraftListGui(Player player) {
        super(Component.text("Chế tạo nhanh", NamedTextColor.DARK_GRAY), BOTTOM_ROW + 1);
        this.player = player;
        this.candidates = QuickCraftService.findAll(player);
    }

    @Override
    public void setup() {
        clearUi();
        for (int col = 0; col < 9; col++) {
            addButton(0, col, FILLER_BLACK, ClickHandler.noAction());
            addButton(BOTTOM_ROW, col, FILLER_BLACK, ClickHandler.noAction());
        }
        for (int row = 1; row < BOTTOM_ROW; row++) {
            addButton(row, 0, FILLER_BLACK, ClickHandler.noAction());
            addButton(row, 8, FILLER_BLACK, ClickHandler.noAction());
        }

        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, candidates.size());
        for (int i = start; i < end; i++) {
            int index = i - start;
            QuickCraftCandidate candidate = candidates.get(i);
            addButton(1 + index / RESULT_COLS, 1 + index % RESULT_COLS, QuickCraftDisplay.render(candidate), event -> {
                event.setCancelled(true);
                QuickCraftService.craft(player, candidate.recipe(), event.isShiftClick());
                refresh();
            });
        }

        ItemStack back = ItemStack.of(Material.ARROW);
        back.setData(DataComponentTypes.ITEM_NAME, Component.text("Quay lại", NamedTextColor.YELLOW));
        addButton(BOTTOM_ROW, BACK_COL, back, ClickHandler.openGui(new CraftingGui(player)));

        if (page > 0) {
            addButton(BOTTOM_ROW, PREV_PAGE_COL, PREV_PAGE_BUTTON, event -> {
                event.setCancelled(true);
                page--;
                setup();
            });
        }
        if (end < candidates.size()) {
            addButton(BOTTOM_ROW, NEXT_PAGE_COL, NEXT_PAGE_BUTTON, event -> {
                event.setCancelled(true);
                page++;
                setup();
            });
        }
        fillEmpty();
    }

    @Override
    public void onClickBottomInventory(InventoryClickEvent event) {
        event.setCancelled(true);
    }

    @Override
    public void onDragInventory(InventoryDragEvent event) {
        event.setCancelled(true);
    }

    private void refresh() {
        candidates = QuickCraftService.findAll(player);
        int lastPage = Math.max(0, (candidates.size() - 1) / PAGE_SIZE);
        page = Math.min(page, lastPage);
        setup();
    }
}
