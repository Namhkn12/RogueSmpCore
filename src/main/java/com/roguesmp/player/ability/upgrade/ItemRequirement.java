package com.roguesmp.player.ability.upgrade;

import com.roguesmp.constant.ComponentKeys;
import com.roguesmp.item.BaseItem;
import com.roguesmp.item.component.impl.NameComponent;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.ItemStackUtils;
import com.roguesmp.utils.SmpItemUtils;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class ItemRequirement implements UpgradeRequirement {

    private final BaseItem requiredItem;
    private final int amount;

    public ItemRequirement(BaseItem requiredItem, int amount) {
        this.requiredItem = requiredItem;
        this.amount = amount;
    }

    @Override
    public boolean canFulfill(SmpPlayer player) {
        Player bukkitPlayer = player.getBukkitPlayer();
        if (bukkitPlayer == null) return false;

        int count = 0;
        for (ItemStack stack : bukkitPlayer.getInventory().getContents()) {
            if (!ItemStackUtils.isValidItem(stack)) continue;

            if (requiredItem.equals(SmpItemUtils.getBaseItem(stack))) {
                count += stack.getAmount();
            }

            if (count >= amount) return true;
        }

        return count >= amount;
    }

    @Override
    public void consume(SmpPlayer player) {
        Player bukkitPlayer = player.getBukkitPlayer();
        if (bukkitPlayer == null) return;

        Inventory inv = bukkitPlayer.getInventory();
        int leftToTake = amount;

        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!ItemStackUtils.isValidItem(stack)) continue;

            if (requiredItem.equals(SmpItemUtils.getBaseItem(stack))) {
                int stackAmount = stack.getAmount();

                if (stackAmount <= leftToTake) {
                    leftToTake -= stackAmount;
                    inv.setItem(i, null);
                } else {
                    stack.setAmount(stackAmount - leftToTake);
                    leftToTake = 0;
                }
            }

            if (leftToTake <= 0) break;
        }
    }

    @Override
    public Component getDisplay(SmpPlayer player) {
        Component itemName;

        NameComponent nameComponent = requiredItem.getComponent(ComponentKeys.ITEM_NAME);
        if (nameComponent == null) {
            itemName = Utils.text(requiredItem.getId(), NamedTextColor.GRAY);
        } else {
            itemName = Utils.fromString(nameComponent.value());
        }

        return Component.text("- " + amount + "x ")
                .append(itemName);
    }
}
