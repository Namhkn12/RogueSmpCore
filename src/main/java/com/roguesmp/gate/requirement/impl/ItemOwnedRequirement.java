package com.roguesmp.gate.requirement.impl;

import com.roguesmp.gate.requirement.Requirement;
import com.roguesmp.codec.Codec;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.gate.GateDisplay;
import com.roguesmp.gate.GateInventoryUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class ItemOwnedRequirement implements Requirement {

    public static final String TYPE_KEY = "item";
    public static final Codec<ItemOwnedRequirement> CODEC = Codec.composite(
            Codec.STRING.fieldOf("item").forGetter(ItemOwnedRequirement::getItem),
            Codec.INT.optionalFieldOf("amount", 1).forGetter(ItemOwnedRequirement::getAmount),
            ItemOwnedRequirement::new
    );

    private final String item;
    private final int amount;

    public ItemOwnedRequirement(String item, int amount) {
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
    public boolean test(SmpPlayer player) {
        return GateInventoryUtils.count(player, item) >= amount;
    }

    @Override
    public Component getDisplay(SmpPlayer player) {
        return Component.text("Sở hữu ", NamedTextColor.GRAY)
                .append(GateDisplay.itemProgress(item, amount, GateInventoryUtils.count(player, item)));
    }
}
