package com.roguesmp.trade.result;

import com.roguesmp.codec.Codec;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.gate.GateDisplay;
import com.roguesmp.item.BaseItem;
import com.roguesmp.utils.ItemRef;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ItemResult implements TradeResult {

    public static final String TYPE_KEY = "item";
    public static final Codec<ItemResult> CODEC = Codec.composite(
            Codec.STRING.fieldOf("item").forGetter(ItemResult::getItem),
            Codec.INT.optionalFieldOf("amount", 1).forGetter(ItemResult::getAmount),
            ItemResult::new
    );

    private final String item;
    private final int amount;

    public ItemResult(String item, int amount) {
        this.item = item;
        this.amount = amount;
    }

    @Override
    public String getTypeId() {
        return TYPE_KEY;
    }

    public String getItem() {
        return item;
    }

    public int getAmount() {
        return amount;
    }

    @Override
    public List<ItemStack> createItems(SmpPlayer player) {
        ItemStack stack = getIcon(player);
        return stack == null ? List.of() : List.of(stack);
    }

    @Override
    public Component getDisplay(SmpPlayer player) {
        return Component.text(amount + "x ", NamedTextColor.YELLOW).append(GateDisplay.itemName(item));
    }

    @Override
    public @Nullable ItemStack getIcon(SmpPlayer player) {
        BaseItem baseItem = ItemRef.baseItem(item);
        return baseItem != null ? baseItem.generateItemStack(player, amount) : ItemRef.toItemStack(item, amount);
    }
}
