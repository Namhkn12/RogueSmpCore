package com.roguesmp.trade.gui;

import com.roguesmp.gui.BaseGui;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.trade.TradeOffer;
import com.roguesmp.trade.result.TradeResult;
import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class TradeOfferPreviewGui extends BaseGui {

    private static final int RESULTS_PER_ROW = 7;

    private final SmpPlayer player;
    private final TradeOffer offer;

    public TradeOfferPreviewGui(SmpPlayer player, TradeOffer offer, Component title) {
        super(title, rowsFor(offer));
        this.player = player;
        this.offer = offer;
    }

    private static int rowsFor(TradeOffer offer) {
        int resultRows = Math.max(1, (offer.getResults().size() + RESULTS_PER_ROW - 1) / RESULTS_PER_ROW);
        return Math.min(6, resultRows + 2);
    }

    @Override
    public void setup() {
        clearUi();
        List<TradeResult> results = offer.getResults();
        int capacity = (getInventory().getSize() / 9 - 2) * RESULTS_PER_ROW;
        for (int i = 0; i < results.size() && i < capacity; i++) {
            addButton(1 + i / RESULTS_PER_ROW, 1 + i % RESULTS_PER_ROW, createIcon(results.get(i)), ClickHandler.noAction());
        }
        addBackButton(getSlot(getInventory().getSize() / 9 - 1, 4));
        fillEmpty(FILLER_BLACK);
    }

    private ItemStack createIcon(TradeResult result) {
        ItemStack icon = result.getIcon(player);
        if (icon != null) return icon;
        ItemStack fallback = ItemStack.of(Material.PAPER);
        fallback.setData(DataComponentTypes.CUSTOM_NAME, result.getDisplay(player).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
        return fallback;
    }

    @Override
    public void onClickBottomInventory(InventoryClickEvent event) {
        event.setCancelled(true);
    }
}
