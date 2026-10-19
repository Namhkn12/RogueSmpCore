package com.roguesmp.trade.cost;

import com.roguesmp.codec.Codec;
import com.roguesmp.item.BaseItem;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.registry.Holder;
import com.roguesmp.registry.Registries;
import com.roguesmp.registry.Registry;
import com.roguesmp.trade.TradeDisplay;
import com.roguesmp.trade.TradeInventory;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.Inventory;

public class ItemCost implements TradeCost {

    public static final String TYPE_KEY = "item";
    public static final Codec<ItemCost> CODEC = Codec.composite(
            Registry.referenceCodec(() -> Registries.ITEM).fieldOf("item_id").forGetter(ItemCost::getItem),
            Codec.INT.optionalFieldOf("amount", 1).forGetter(ItemCost::getAmount),
            ItemCost::new
    );

    private final Holder<BaseItem> item;
    private final int amount;

    public ItemCost(Holder<BaseItem> item, int amount) {
        this.item = item;
        this.amount = amount;
    }

    @Override
    public String getTypeId() {
        return TYPE_KEY;
    }

    public Holder<BaseItem> getItem() {
        return item;
    }

    public int getAmount() {
        return amount;
    }

    @Override
    public boolean canAfford(SmpPlayer player) {
        return TradeInventory.count(player, item) >= amount;
    }

    @Override
    public void takeFrom(Inventory inventory) {
        TradeInventory.remove(inventory, item, amount);
    }

    @Override
    public Component getDisplay(SmpPlayer player) {
        return TradeDisplay.itemProgress(item, amount, TradeInventory.count(player, item));
    }
}
