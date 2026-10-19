package com.roguesmp.trade.cost;

import com.roguesmp.codec.Codec;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.trade.TradeDisplay;
import net.kyori.adventure.text.Component;

public class MoneyCost implements TradeCost {

    public static final String TYPE_KEY = "money";
    public static final Codec<MoneyCost> CODEC = Codec.composite(
            Codec.LONG.fieldOf("amount").forGetter(MoneyCost::getAmount),
            MoneyCost::new
    );

    private final long amount;

    public MoneyCost(long amount) {
        this.amount = amount;
    }

    @Override
    public String getTypeId() {
        return TYPE_KEY;
    }

    public long getAmount() {
        return amount;
    }

    @Override
    public boolean canAfford(SmpPlayer player) {
        return player.hasMoney(amount);
    }

    @Override
    public void take(SmpPlayer player) {
        player.takeMoney(amount);
    }

    @Override
    public Component getDisplay(SmpPlayer player) {
        return TradeDisplay.money(amount, canAfford(player));
    }
}
