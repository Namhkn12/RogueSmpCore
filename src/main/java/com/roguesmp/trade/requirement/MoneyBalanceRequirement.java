package com.roguesmp.trade.requirement;

import com.roguesmp.codec.Codec;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.trade.TradeDisplay;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class MoneyBalanceRequirement implements TradeRequirement {

    public static final String TYPE_KEY = "money";
    public static final Codec<MoneyBalanceRequirement> CODEC = Codec.composite(
            Codec.LONG.fieldOf("amount").forGetter(MoneyBalanceRequirement::getAmount),
            MoneyBalanceRequirement::new
    );

    private final long amount;

    public MoneyBalanceRequirement(long amount) {
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
    public boolean test(SmpPlayer player) {
        return player.hasMoney(amount);
    }

    @Override
    public Component getDisplay(SmpPlayer player) {
        return Component.text("Sở hữu ", NamedTextColor.GRAY).append(TradeDisplay.money(amount, test(player)));
    }
}
