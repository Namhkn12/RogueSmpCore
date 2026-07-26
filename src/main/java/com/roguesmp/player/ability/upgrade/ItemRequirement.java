package com.roguesmp.player.ability.upgrade;

import com.roguesmp.codec.Codec;
import com.roguesmp.constant.ComponentKeys;
import com.roguesmp.item.BaseItem;
import com.roguesmp.item.component.impl.NameComponent;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.ItemStackUtils;
import com.roguesmp.utils.SmpItemUtils;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class ItemRequirement implements UpgradeRequirement {

    public static final String TYPE_KEY = "item";

    public static final Codec<ItemRequirement> CODEC = Codec.composite(
            BaseItem.REFERENCE_CODEC.fieldOf("item_id").forGetter(ItemRequirement::getRequiredItem),
            Codec.INT.fieldOf("amount").forGetter(ItemRequirement::getAmount),
            ItemRequirement::new
    );

    private final BaseItem requiredItem;
    private final int amount;

    public ItemRequirement(BaseItem requiredItem, int amount) {
        this.requiredItem = requiredItem;
        this.amount = amount;
    }

    @Override
    public String getTypeId() {
        return TYPE_KEY;
    }

    public BaseItem getRequiredItem() {
        return requiredItem;
    }

    public int getAmount() {
        return amount;
    }

    @Override
    public boolean canFulfill(SmpPlayer player) {
        return getCurrentAmount(player) >= amount;
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

            if (requiredItem != null && requiredItem.equals(SmpItemUtils.getBaseItem(stack))) {
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
        if (requiredItem == null) return Component.text("Item Error", NamedTextColor.RED);

        // 1. Get the item name
        Component itemName;
        NameComponent nameComponent = requiredItem.getComponent(ComponentKeys.ITEM_NAME);
        if (nameComponent == null) {
            itemName = Component.text(requiredItem.getId(), NamedTextColor.GRAY);
        } else {
            itemName = Utils.fromString(nameComponent.value()).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
        }

        // 2. Calculate progress
        int current = getCurrentAmount(player);
        boolean hasEnough = current >= amount;

        // 3. Build adaptive component
        // Example: - 5x Steel Ingot (2/5)
        return Component.text(" • ", NamedTextColor.DARK_GRAY)
                .append(Component.text(amount + "x ", NamedTextColor.GREEN))
                .append(itemName)
                .append(Component.text(" (", NamedTextColor.GRAY))
                .append(Component.text(current, hasEnough ? NamedTextColor.GREEN : NamedTextColor.RED))
                .append(Component.text("/", NamedTextColor.GRAY))
                .append(Component.text(amount, NamedTextColor.GRAY))
                .append(Component.text(")", NamedTextColor.GRAY));
    }

    private int getCurrentAmount(SmpPlayer player) {
        Player bukkitPlayer = player.getBukkitPlayer();
        if (bukkitPlayer == null) return 0;

        int count = 0;
        for (ItemStack stack : bukkitPlayer.getInventory().getContents()) {
            if (!ItemStackUtils.isValidItem(stack)) continue;

            if (requiredItem != null && requiredItem.equals(SmpItemUtils.getBaseItem(stack))) {
                count += stack.getAmount();
            }
        }
        return count;
    }
}
