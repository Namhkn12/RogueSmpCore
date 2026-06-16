package com.roguesmp.listener;

import com.roguesmp.gui.WalletGui;
import com.roguesmp.item.SmpItem;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.Utils;
import com.roguesmp.utils.WalletUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;

public class WalletPickupListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onWalletItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        ItemStack pickedItem = event.getItem().getItemStack();
        if (!WalletUtils.isCurrency(pickedItem)) return;
        // If player has a wallet open, the items won't go into wallet
        if (player.getOpenInventory().getTopInventory().getHolder() instanceof WalletGui) {
            return;
        }

        int pickupMoneyValue = WalletUtils.calculateMoneyValue(pickedItem);
        if (pickupMoneyValue <= 0) return;

        // Track if we successfully found a home for this money stack
        boolean deposited = false;

        // Loop through inventory slots directly so we can update the item stack later
        ItemStack[] contents = player.getInventory().getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack walletItem = contents[slot];
            if (walletItem == null || !WalletUtils.isWalletItem(walletItem)) continue;

            int currentBalance = WalletUtils.getBalance(walletItem);
            int spaceLeft = WalletUtils.getCapacity(walletItem) - currentBalance;

            // The moment we find ONE wallet that can completely absorb the stack, we take it!
            if (spaceLeft >= pickupMoneyValue) {
                WalletUtils.setBalance(walletItem, currentBalance + pickupMoneyValue);
                deposited = true;

                SmpItem smpItem = new SmpItem(walletItem);
                SmpPlayer smpPlayer = PlayerManager.getInstance().getSmpPlayer(player);
                if (smpPlayer != null) {
                    ItemStack updated = smpItem.generateItemStack(smpPlayer, walletItem.getAmount());
                    // Update the slot with the new item
                    player.getInventory().setItem(slot, updated);
                }

                break;
            }
        }

        // If a single wallet swallowed the entire stack, clear it from the ground
        if (deposited) {
            event.setCancelled(true);
            event.getItem().remove();
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.6F, 1.2F);
            player.sendActionBar(Component.text("+" + Utils.formatMoney(pickupMoneyValue) + " đồng", NamedTextColor.GOLD));
        }
    }
}
