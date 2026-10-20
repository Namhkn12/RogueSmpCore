package com.roguesmp.gate.cost.impl;

import com.roguesmp.gate.cost.Cost;
import com.roguesmp.codec.Codec;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.gate.GateDisplay;
import net.kyori.adventure.text.Component;

public class MoneyCost implements Cost {

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
        return GateDisplay.money(amount, canAfford(player));
    }
}
