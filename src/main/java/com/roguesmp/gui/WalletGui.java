package com.roguesmp.gui;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.item.BaseItem;
import com.roguesmp.item.component.impl.WalletComponent;
import com.roguesmp.registry.ItemRegistry;
import com.roguesmp.utils.ItemStackUtils;
import com.roguesmp.utils.Utils;
import com.roguesmp.utils.WalletUtils;
import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class WalletGui extends BaseGui {

    private final ItemStack walletItem;
    private final Player player;

    public WalletGui(ItemStack walletItem, Player player) {
        // Automatically reads the item's custom name via Paper components if it exists
        super(walletItem.hasData(DataComponentTypes.CUSTOM_NAME)
                ? walletItem.getData(DataComponentTypes.CUSTOM_NAME)
                : Component.text("Ví Đồng", NamedTextColor.GOLD), 3);
        this.walletItem = walletItem;
        this.player = player;
    }

    @Override
    public void setup() {
        // Prevent layout ghosting when updating balances dynamically
        clearUi();

        int totalNuggets;
        Integer value = walletItem.getPersistentDataContainer().get(WalletUtils.DATA_ID, PersistentDataType.INTEGER);
        if (value == null) {
            player.sendMessage(Utils.fromString("<red>Vật phẩm này không chưa data của ví? ඞ"));
            RogueSmpCore.LOGGER.warn("Người chơi {} muốn mở WalletGui nhưng Wallet ItemStack không chứa data? Very sus", player.getName());
            return;
        }
        totalNuggets = value;

        List<ItemStack> currencyStacks = WalletUtils.convertMoneyToPhysicalItems(totalNuggets);

        int currentSlot = 0;
        int maxSlots = getInventory().getSize();

        for (ItemStack stack : currencyStacks) {
            if (currentSlot >= maxSlots) break;
            addItem(currentSlot++, stack);
        }
    }

    @Override
    public void onClickTopInventory(InventoryClickEvent event) {
        ItemStack cursorItem = event.getCursor();
        ItemStack clickedItem = event.getCurrentItem();

        // 1. If they are clicking an item INSIDE the GUI slot, it MUST be valid currency
        if (ItemStackUtils.isValidItem(clickedItem)) {
            if (!WalletUtils.isCurrency(clickedItem)) {
                event.setCancelled(true);
                return;
            }
        }

        // 2. If they are trying to PLACE an item from their cursor into the GUI, it MUST be valid currency
        if (ItemStackUtils.isValidItem(cursorItem)) {
            if (!WalletUtils.isCurrency(cursorItem)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @Override
    public void onClickBottomInventory(InventoryClickEvent event) {
        ItemStack clickedItem = event.getCurrentItem(); // The item in the player inventory slot they clicked

        // 1. If they clicked a blank/empty slot in their inventory, let them move things there freely
        if (!ItemStackUtils.isValidItem(clickedItem)) {
            return;
        }

        // 2. If they clicked a non-currency item (like a sword or dirt block)
        if (!WalletUtils.isCurrency(clickedItem)) {
            event.setCancelled(true);
            return;
        }
    }

    @Override
    public void onCloseInventory(InventoryCloseEvent event) {
        saveWalletToNbt();
    }

    @Override
    public void onDragInventory(InventoryDragEvent event) {
        event.setCancelled(true);
    }

    /**
     * Iterates dynamically over every container inventory slot to sum up currency items,
     * compressing them cleanly back into a single total value saved right to the item.
     */
    private void saveWalletToNbt() {

        int totalNuggets = 0;
        for (int i = 0; i < getInventory().getSize(); i++) {
            ItemStack item = getInventory().getItem(i);
            if (ItemStackUtils.isValidItem(item) && WalletUtils.isCurrency(item)) {
                totalNuggets += WalletUtils.calculateMoneyValue(item);
            }
        }
        WalletUtils.setBalance(walletItem, totalNuggets);
    }
}
