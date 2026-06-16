package com.roguesmp.utils;

import com.roguesmp.constant.ComponentKeys;
import com.roguesmp.constant.Keys;
import com.roguesmp.item.BaseItem;
import com.roguesmp.item.component.impl.WalletComponent;
import com.roguesmp.registry.ItemRegistry;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class WalletUtils {

    public static final NamespacedKey DATA_ID = Keys.of("stored_money");

    // Centralized Currency Value Mappings
    public static final int VALUE_NUGGET = 1;
    public static final int VALUE_INGOT = 64;
    public static final int VALUE_BLOCK = 64 * 64; // 4096

    // Registry Identifiers
    public static final String BLOCK_ID = "copper_block_currency";
    public static final String INGOT_ID = "copper_ingot_currency";
    public static final String NUGGET_ID = "copper_nugget_currency";
    public static final String WALLET_ID_PREFIX = "wallet";

    public static boolean isCurrency(ItemStack itemStack) {
        String id = ItemStackUtils.getId(itemStack);
        if (id == null) return false;
        return id.equals(BLOCK_ID) || id.equals(INGOT_ID) || id.equals(NUGGET_ID);
    }

    public static boolean isWalletItem(ItemStack itemStack) {
        String id = ItemStackUtils.getId(itemStack);
        if (id == null) return false;
        return id.startsWith(WALLET_ID_PREFIX);
    }

    public static int calculateMoneyValue(ItemStack item) {
        String id = ItemStackUtils.getId(item);
        if (id == null) return 0;

        return switch (id) {
            case BLOCK_ID -> item.getAmount() * VALUE_BLOCK;
            case INGOT_ID -> item.getAmount() * VALUE_INGOT;
            case NUGGET_ID -> item.getAmount() * VALUE_NUGGET;
            default -> 0;
        };
    }

    /**
     * Converts a raw value of nuggets into optimized, chunked physical stacks (max size 64).
     */
    public static List<ItemStack> convertMoneyToPhysicalItems(int totalNuggets) {
        List<ItemStack> items = new ArrayList<>();

        int blocks = totalNuggets / VALUE_BLOCK;
        int remaining = totalNuggets % VALUE_BLOCK;
        int ingots = remaining / VALUE_INGOT;
        int nuggets = remaining % VALUE_INGOT;

        BaseItem blockBase = ItemRegistry.getInstance().getBaseItem(BLOCK_ID);
        BaseItem ingotBase = ItemRegistry.getInstance().getBaseItem(INGOT_ID);
        BaseItem nuggetBase = ItemRegistry.getInstance().getBaseItem(NUGGET_ID);

        if (blockBase != null && blocks > 0) {
            ItemStack blockItem = blockBase.generateItemStack(1);
            while (blocks > 0) {
                int count = Math.min(64, blocks);
                ItemStack clone = blockItem.clone();
                clone.setAmount(count);
                items.add(clone);
                blocks -= count;
            }
        }

        if (ingotBase != null && ingots > 0) {
            items.add(ingotBase.generateItemStack(ingots));
        }

        if (nuggetBase != null && nuggets > 0) {
            items.add(nuggetBase.generateItemStack(nuggets));
        }

        return items;
    }

    public static List<ItemStack> findAllWallets(Player player) {
        List<ItemStack> wallets = new ArrayList<>();
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && isWalletItem(item)) {
                wallets.add(item);
            }
        }
        return wallets;
    }

    /**
     * Get how much currency this item can hold
     */
    public static int getCapacity(ItemStack itemStack) {
        return getSlotCapacity(itemStack) * VALUE_BLOCK * 64;
    }

    /**
     * Get how much slot this wallet has
     */
    public static int getSlotCapacity(ItemStack itemStack) {
        if (!isWalletItem(itemStack)) return 0;
        BaseItem walletBase = SmpItemUtils.getBaseItem(itemStack);
        if (walletBase == null) return 0;
        WalletComponent walletComponent = walletBase.getComponent(ComponentKeys.WALLET);
        if (walletComponent == null) return 0;
        return walletComponent.getMaxSlot();
    }

    /**
     * Get how much money this itemstack might have stored.
     */
    public static int getBalance(ItemStack itemStack) {
        if (!isWalletItem(itemStack)) return 0;
        return itemStack.getPersistentDataContainer().getOrDefault(DATA_ID, PersistentDataType.INTEGER, 0);
    }

    /**
     * Directly set the balance of a specific wallet item stack.
     * Automatically constrains value between 0 and MAX_CURRENCY.
     */
    public static void setBalance(ItemStack itemStack, int amount) {
        if (!isWalletItem(itemStack)) return;
        int boundedAmount = Math.max(0, Math.min(amount, getCapacity(itemStack)));
        itemStack.editPersistentDataContainer(pdc -> pdc.set(DATA_ID, PersistentDataType.INTEGER, boundedAmount));
    }

    /**
     * Add a specified amount to a wallet item stack.
     *
     * @param amount Amount to deposit (must be positive).
     * @param itemStack Target wallet item.
     * @return The leftover amount that could not fit due to wallet capacity caps.
     */
    public static int addBalance(ItemStack itemStack, int amount) {
        if (!isWalletItem(itemStack) || amount <= 0) return amount;

        int currentBalance = getBalance(itemStack);
        int maxRoomLeft = getCapacity(itemStack) - currentBalance;

        if (maxRoomLeft <= 0) return amount;

        int amountToDeposit = Math.min(amount, maxRoomLeft);
        setBalance(itemStack, currentBalance + amountToDeposit);

        return amount - amountToDeposit;
    }

    /**
     * Deduct a specified amount from a wallet item stack.
     *
     * @param amount Amount to withdraw (must be positive).
     * @param itemStack Target wallet item.
     * @return The leftover amount that couldn't be deducted because the wallet hit 0.
     */
    public static int removeBalance(ItemStack itemStack, int amount) {
        if (!isWalletItem(itemStack) || amount <= 0) return amount;

        int currentBalance = getBalance(itemStack);
        if (currentBalance <= 0) return amount;

        int amountToRemove = Math.min(amount, currentBalance);
        setBalance(itemStack, currentBalance - amountToRemove);

        return amount - amountToRemove;
    }

    /* ========================================================================= */
    /*                    PLAYER DIRECT CONVENIENCE METHODS                      */
    /* ========================================================================= */

    /**
     * Sums up the total balance across ALL wallets present inside a player's inventory layout.
     */
    public static int getPlayerTotalBalance(Player player) {
        int total = 0;
        for (ItemStack wallet : findAllWallets(player)) {
            total += getBalance(wallet);
        }
        return total;
    }

    /**
     * Checks if a player has a specific total balance distributed across their wallets.
     */
    public static boolean hasPlayerBalance(Player player, int requiredAmount) {
        return getPlayerTotalBalance(player) >= requiredAmount;
    }

    /**
     * Distributes an incoming financial deposit across all wallets in a player's inventory chain.
     *
     * @return The leftover amount that couldn't fit anywhere because all wallets are completely full.
     */
    public static int addPlayerBalance(Player player, int amount) {
        int leftOver = amount;
        for (ItemStack wallet : findAllWallets(player)) {
            if (leftOver <= 0) break;
            leftOver = addBalance(wallet, leftOver);
        }
        return leftOver;
    }

    /**
     * Deducts funds cleanly across a player's wallet collection.
     * Prioritizes draining wallets sequentially until the charge request is satisfied.
     *
     * @return The leftover bill amount that couldn't be satisfied because the player went completely broke.
     */
    public static int removePlayerBalance(Player player, int amount) {
        int billRemaining = amount;
        for (ItemStack wallet : findAllWallets(player)) {
            if (billRemaining <= 0) break;
            billRemaining = removeBalance(wallet, billRemaining);
        }
        return billRemaining;
    }
}
