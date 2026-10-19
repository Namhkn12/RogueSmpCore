package com.roguesmp.trade.result;

import com.roguesmp.codec.Codec;
import com.roguesmp.item.BaseItem;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.registry.Holder;
import com.roguesmp.registry.Registries;
import com.roguesmp.registry.Registry;
import com.roguesmp.trade.TradeDisplay;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ItemResult implements TradeResult {

    public static final String TYPE_KEY = "item";
    public static final Codec<ItemResult> CODEC = Codec.composite(
            Registry.referenceCodec(() -> Registries.ITEM).fieldOf("item_id").forGetter(ItemResult::getItem),
            Codec.INT.optionalFieldOf("amount", 1).forGetter(ItemResult::getAmount),
            ItemResult::new
    );

    private final Holder<BaseItem> item;
    private final int amount;

    public ItemResult(Holder<BaseItem> item, int amount) {
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
    public List<ItemStack> createItems(SmpPlayer player) {
        return item.isBound() ? List.of(item.value().generateItemStack(player, amount)) : List.of();
    }

    @Override
    public Component getDisplay(SmpPlayer player) {
        return Component.text(amount + "x ", NamedTextColor.YELLOW).append(TradeDisplay.itemName(item));
    }

    @Override
    public @Nullable ItemStack getIcon(SmpPlayer player) {
        return item.isBound() ? item.value().generateItemStack(player, amount) : null;
    }
}
