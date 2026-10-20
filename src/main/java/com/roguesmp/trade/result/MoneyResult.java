package com.roguesmp.trade.result;

import com.roguesmp.codec.Codec;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.gate.GateDisplay;
import net.kyori.adventure.text.Component;

public class MoneyResult implements TradeResult {

    public static final String TYPE_KEY = "money";
    public static final Codec<MoneyResult> CODEC = Codec.composite(
            Codec.LONG.fieldOf("amount").forGetter(MoneyResult::getAmount),
            MoneyResult::new
    );

    private final long amount;

    public MoneyResult(long amount) {
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
    public void give(SmpPlayer player) {
        player.giveMoney(amount);
    }

    @Override
    public Component getDisplay(SmpPlayer player) {
        return GateDisplay.money(amount, true);
    }
}
