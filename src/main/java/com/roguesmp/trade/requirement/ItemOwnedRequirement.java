package com.roguesmp.trade.requirement;

import com.roguesmp.codec.Codec;
import com.roguesmp.item.BaseItem;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.registry.Holder;
import com.roguesmp.registry.Registries;
import com.roguesmp.registry.Registry;
import com.roguesmp.trade.TradeDisplay;
import com.roguesmp.trade.TradeInventory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class ItemOwnedRequirement implements TradeRequirement {

    public static final String TYPE_KEY = "item";
    public static final Codec<ItemOwnedRequirement> CODEC = Codec.composite(
            Registry.referenceCodec(() -> Registries.ITEM).fieldOf("item_id").forGetter(ItemOwnedRequirement::getItem),
            Codec.INT.optionalFieldOf("amount", 1).forGetter(ItemOwnedRequirement::getAmount),
            ItemOwnedRequirement::new
    );

    private final Holder<BaseItem> item;
    private final int amount;

    public ItemOwnedRequirement(Holder<BaseItem> item, int amount) {
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
    public boolean test(SmpPlayer player) {
        return TradeInventory.count(player, item) >= amount;
    }

    @Override
    public Component getDisplay(SmpPlayer player) {
        return Component.text("Sở hữu ", NamedTextColor.GRAY)
                .append(TradeDisplay.itemProgress(item, amount, TradeInventory.count(player, item)));
    }
}
