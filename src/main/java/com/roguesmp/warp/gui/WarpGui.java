package com.roguesmp.warp.gui;

import com.roguesmp.gui.BaseGui;
import com.roguesmp.utils.Utils;
import com.roguesmp.warp.Warp;
import com.roguesmp.warp.WarpService;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import io.papermc.paper.registry.keys.SoundEventKeys;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WarpGui extends BaseGui {

    private static final int[] CARD_COLUMNS = {1, 3, 5, 7};
    private static final int BORDER_TOP_ROW = 1;
    private static final int ICON_ROW = 2;
    private static final int BORDER_BOTTOM_ROW = 3;
    private static final int NAV_ROW = 4;

    private final Player player;
    private int currentPage = 0;

    public WarpGui(Player player) {
        super(Component.text("Dịch Chuyển", NamedTextColor.DARK_PURPLE).decoration(TextDecoration.BOLD, true), 5);
        this.player = player;
    }

    @Override
    public void setup() {
        clearUi();

        List<Map.Entry<String, Warp>> warps = WarpService.getInstance().usableWarps(player);
        int totalPages = Math.max(1, (int) Math.ceil(warps.size() / (double) CARD_COLUMNS.length));
        currentPage = Math.min(currentPage, totalPages - 1);

        int from = currentPage * CARD_COLUMNS.length;
        List<Map.Entry<String, Warp>> pageWarps = warps.subList(from, Math.min(from + CARD_COLUMNS.length, warps.size()));
        ItemStack border = createDecoration(Material.PURPLE_STAINED_GLASS_PANE);
        for (int i = 0; i < pageWarps.size(); i++) {
            String id = pageWarps.get(i).getKey();
            int col = CARD_COLUMNS[i];
            addButton(BORDER_TOP_ROW, col, border, ClickHandler.noAction());
            addButton(BORDER_BOTTOM_ROW, col, border, ClickHandler.noAction());
            addButton(ICON_ROW, col, createWarpIcon(pageWarps.get(i).getValue()), event -> {
                event.setCancelled(true);
                player.closeInventory();
                player.playSound(Sound.sound(SoundEventKeys.ENTITY_ENDERMAN_TELEPORT, Sound.Source.PLAYER, 0.8f, 1.2f));
                WarpService.getInstance().teleport(player, id);
            });
        }

        if (warps.isEmpty()) {
            ItemStack empty = ItemStack.of(Material.STRUCTURE_VOID);
            empty.setData(DataComponentTypes.ITEM_NAME, Component.text("Chưa có điểm dịch chuyển nào", NamedTextColor.GRAY));
            addButton(ICON_ROW, 4, empty, ClickHandler.noAction());
        }

        if (currentPage > 0) {
            addButton(NAV_ROW, 3, PREV_PAGE_BUTTON, event -> {
                event.setCancelled(true);
                changePage(-1);
            });
        }
        if (currentPage < totalPages - 1) {
            addButton(NAV_ROW, 5, NEXT_PAGE_BUTTON, event -> {
                event.setCancelled(true);
                changePage(1);
            });
        }
        ItemStack pageIcon = ItemStack.of(Material.PAPER);
        pageIcon.setData(DataComponentTypes.ITEM_NAME, Component.text("Trang " + (currentPage + 1) + "/" + totalPages, NamedTextColor.YELLOW));
        addButton(NAV_ROW, 4, pageIcon, ClickHandler.noAction());
        addBackButton(getSlot(NAV_ROW, 0));
        fillEmpty(FILLER_BLACK);
    }

    private void changePage(int delta) {
        currentPage += delta;
        player.playSound(Sound.sound(SoundEventKeys.ITEM_BOOK_PAGE_TURN, Sound.Source.PLAYER, 1f, 1f));
        setup();
    }

    private ItemStack createWarpIcon(Warp warp) {
        ItemStack item = warp.createIcon();
        item.setData(DataComponentTypes.ITEM_NAME, Utils.fromString(warp.displayName()).decoration(TextDecoration.BOLD, true));

        List<Component> lore = new ArrayList<>();
        for (Component line : Utils.fromStrings(warp.lore())) {
            lore.add(line.colorIfAbsent(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        }
        if (!lore.isEmpty()) lore.add(Component.empty());
        lore.add(Utils.text("Click để dịch chuyển", NamedTextColor.YELLOW));

        item.setData(DataComponentTypes.LORE, ItemLore.lore(lore));
        return item;
    }

    @Override
    public void onClickBottomInventory(InventoryClickEvent event) {
        event.setCancelled(true);
    }
}
