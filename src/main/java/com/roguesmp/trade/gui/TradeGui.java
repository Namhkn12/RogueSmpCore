package com.roguesmp.trade.gui;

import com.roguesmp.gui.BaseGui;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.gate.GateDisplay;
import com.roguesmp.trade.TradeLayout;
import com.roguesmp.trade.TradeOffer;
import com.roguesmp.trade.TradeShop;
import com.roguesmp.gate.cost.Cost;
import com.roguesmp.gate.requirement.Requirement;
import com.roguesmp.trade.result.TradeResult;
import com.roguesmp.utils.Utils;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class TradeGui extends BaseGui {

    private final Player player;
    private final TradeShop shop;
    private int page;

    public TradeGui(Player player, TradeShop shop) {
        super(Utils.fromString(shop.getTitle()), shop.getLayout().getRows());
        this.player = player;
        this.shop = shop;
    }

    @Override
    public void setup() {
        clearUi();
        SmpPlayer smpPlayer = PlayerManager.getInstance().getSmpPlayer(player);
        if (smpPlayer == null) return;

        TradeLayout layout = shop.getLayout();
        for (int slot = 0; slot < layout.getRows() * 9; slot++) {
            Material decoration = layout.getKeys().get(layout.charAt(slot));
            if (decoration != null) addButton(slot, createDecoration(decoration), ClickHandler.noAction());
        }

        List<TradeOffer> offers = shop.getOffers();
        List<Integer> offerSlots = layout.getOfferSlots();
        int start = page * offerSlots.size();
        for (int i = 0; i < offerSlots.size() && start + i < offers.size(); i++) {
            TradeOffer offer = offers.get(start + i);
            addButton(offerSlots.get(i), buildOfferItem(offer, smpPlayer), event -> {
                event.setCancelled(true);
                switch (offer.trade(smpPlayer)) {
                    case SUCCESS -> player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f);
                    case NOT_ELIGIBLE -> deny("Bạn không đủ điều kiện để giao dịch!");
                    case INVENTORY_FULL -> deny("Túi đồ không đủ chỗ trống!");
                }
                setup();
            });
        }

        if (page > 0 && layout.getPrevPageSlot() >= 0) {
            addButton(layout.getPrevPageSlot(), PREV_PAGE_BUTTON, event -> {
                event.setCancelled(true);
                page--;
                setup();
            });
        }
        if (start + offerSlots.size() < offers.size() && layout.getNextPageSlot() >= 0) {
            addButton(layout.getNextPageSlot(), NEXT_PAGE_BUTTON, event -> {
                event.setCancelled(true);
                page++;
                setup();
            });
        }

        ItemStack fill = createDecoration(layout.getFill());
        for (int slot = 0; slot < layout.getRows() * 9; slot++) {
            if (getInventory().getItem(slot) == null) addButton(slot, fill, ClickHandler.noAction());
        }
    }

    private ItemStack buildOfferItem(TradeOffer offer, SmpPlayer smpPlayer) {
        ItemStack icon = offer.getResults().isEmpty() ? null : offer.getResults().getFirst().getIcon(smpPlayer);
        if (icon == null) icon = ItemStack.of(Material.CHEST);
        if (offer.getName() != null) icon.setData(DataComponentTypes.CUSTOM_NAME, Utils.fromString(offer.getName()).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));

        List<Component> lore = new ArrayList<>();
        lore.add(header("Mua:", NamedTextColor.GREEN));
        for (TradeResult result : offer.getResults()) lore.add(bullet(result.getDisplay(smpPlayer)));

        if (!offer.getCosts().isEmpty()) {
            lore.add(Component.empty());
            lore.add(header("Giá:", NamedTextColor.GOLD));
            for (Cost cost : offer.getCosts()) lore.add(bullet(GateDisplay.check(cost.getDisplay(smpPlayer), cost.canAfford(smpPlayer))));
        }

        if (!offer.getRequirements().isEmpty()) {
            lore.add(Component.empty());
            lore.add(header("Yêu cầu:", NamedTextColor.AQUA));
            for (Requirement requirement : offer.getRequirements()) lore.add(bullet(GateDisplay.check(requirement.getDisplay(smpPlayer), requirement.test(smpPlayer))));
        }

        lore.add(Component.empty());
        boolean available = offer.meetsRequirements(smpPlayer) && offer.canAfford(smpPlayer);
        lore.add(available
                ? Utils.fromString("<!i><yellow>▶ Click để giao dịch!")
                : Utils.fromString("<!i><red>✘ Không đủ điều kiện"));
        icon.setData(DataComponentTypes.LORE, ItemLore.lore(lore));
        return icon;
    }

    @Override
    public void onClickBottomInventory(InventoryClickEvent event) {
        if (event.isShiftClick()) event.setCancelled(true);
    }

    @Override
    public void onDragInventory(InventoryDragEvent event) {
        if (event.getRawSlots().stream().anyMatch(slot -> slot < getInventory().getSize())) event.setCancelled(true);
    }

    private void deny(String message) {
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
        player.sendMessage(Component.text(message, NamedTextColor.RED));
    }

    private static Component header(String text, NamedTextColor color) {
        return Component.text(text, color, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false);
    }

    private static Component bullet(Component line) {
        return Component.text(" • ", NamedTextColor.DARK_GRAY).append(line).decoration(TextDecoration.ITALIC, false);
    }
}
