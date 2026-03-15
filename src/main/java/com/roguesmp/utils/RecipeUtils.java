package com.roguesmp.utils;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;

public class RecipeUtils {
    public static boolean matches(Inventory inv, int[] slots, List<ItemStack> inputs){
        for(ItemStack requiredItems: inputs){
            int amountNeeded = requiredItems.getAmount();
            int amountFound = 0;

            for(int slot: slots){
                ItemStack itemInSlot = inv.getItem(slot);
                if(itemInSlot!=null && itemInSlot.isSimilar(requiredItems)){
                    int amount = itemInSlot.getAmount();
                    amountFound+=amount;
                }
            }

            if(amountFound < amountNeeded) return false;
        }

        return true;
    }

    public static boolean canCraft(Inventory inv, List<ItemStack> inputs){
        for(ItemStack requiredItems: inputs){
            int amountNeeded = requiredItems.getAmount();
            int amountFound = countItems(inv, requiredItems);

            if(amountFound < amountNeeded) return false;
        }

        return true;
    }

    private static int countItems(Inventory inv, ItemStack similar){
        int count = 0;
        for(ItemStack item: inv.getContents()){
            if(item!=null && item.isSimilar(similar)){
                count+=item.getAmount();
            }
        }

        return count;
    }

    public static void addToOutputSlot(Inventory inv, int[] outputSlots, List<ItemStack> outputs) {
        for (ItemStack result : outputs) {
            addToSpecificSlots(inv, outputSlots, result);
        }
    }

    private static void addToSpecificSlots(Inventory inv, int[] slots, ItemStack itemToAdd) {
        int amountLeft = itemToAdd.getAmount();

        // Ưu tiên stack vào item có sẵn trước
        for (int slot : slots) {
            if (amountLeft <= 0) break;
            ItemStack current = inv.getItem(slot);

            if (current != null && current.isSimilar(itemToAdd)) {
                int space = current.getMaxStackSize() - current.getAmount();
                int toAdd = Math.min(amountLeft, space);

                current.setAmount(current.getAmount() + toAdd);
                amountLeft -= toAdd;
            }
        }

        // Sau đó mới cho vào slot trống
        if (amountLeft > 0) {
            for (int slot : slots) {
                if (amountLeft <= 0) break;
                ItemStack current = inv.getItem(slot);

                if (current == null || current.getType() == Material.AIR) {
                    ItemStack newItem = itemToAdd.clone();
                    int toAdd = Math.min(amountLeft, newItem.getMaxStackSize());
                    newItem.setAmount(toAdd);

                    inv.setItem(slot, newItem);
                    amountLeft -= toAdd;
                }
            }
        }
    }

    public static boolean canFitInSlots(Inventory inv, int[] outputSlots, List<ItemStack> outputs) {
        ItemStack[] simulatedContents = new ItemStack[outputSlots.length];

        for(int i=0; i<outputSlots.length; i++){
            ItemStack item = inv.getItem(outputSlots[i]);
            simulatedContents[i] = (item!=null) ? item.clone() : null;
        }

        for(ItemStack output: outputs){
            boolean fit = tryToAddToSimulatedSlots(simulatedContents, output);
            if(!fit) return false;
        }

        return true;
    }

    private static boolean tryToAddToSimulatedSlots(ItemStack[] contents, ItemStack itemToAdd){
        int amountLeft = itemToAdd.getAmount();
        int maxStack = itemToAdd.getMaxStackSize();

        for(int i=0; i<contents.length; i++){
            if(amountLeft<=0) break;

            ItemStack current = contents[i];
            if(current != null && current.isSimilar(itemToAdd)){
                int space = maxStack - current.getAmount();
                int add = Math.min(amountLeft, space);

                current.setAmount(current.getAmount() + add);
                amountLeft -= add;
            }
        }

        if(amountLeft > 0){
            for(int i=0; i<contents.length; i++){
                if(amountLeft <=0) break;

                if(contents[i] == null || contents[i].getType() == Material.AIR){
                    ItemStack newItem = itemToAdd.clone();
                    int add = Math.min(amountLeft, maxStack);
                    newItem.setAmount(add);

                    contents[i] = newItem;
                    amountLeft -= add;
                }
            }
        }

        return amountLeft == 0;
    }

    public static void givePlayerItem(Player player, List<ItemStack> outputs){
        Inventory inv = player.getInventory();

        for (ItemStack item : outputs) {
            HashMap<Integer, ItemStack> leftOver = inv.addItem(item);

            if (!leftOver.isEmpty()) {
                for (ItemStack drop : leftOver.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                }
            }
        }
    }

    public static void consumeInputs(Player player, List<ItemStack> inputs){}

    public static void consumeInputs(Inventory inv, int[] inputSlots, List<ItemStack> inputs){
        for(ItemStack item: inputs){
            int amount = item.getAmount();

            for(int i=0; i<inputSlots.length; i++){
                ItemStack itemInSlot = inv.getItem(inputSlots[i]);
                if(itemInSlot != null && itemInSlot.isSimilar(item)){
                    int amountLeft = itemInSlot.getAmount() - amount;
                    itemInSlot.setAmount(amountLeft);
                    inv.setItem(inputSlots[i], amountLeft > 0 ? itemInSlot : null);
                }
            }
        }
    }
}
