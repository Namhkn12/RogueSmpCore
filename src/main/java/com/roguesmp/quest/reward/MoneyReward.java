package com.roguesmp.quest.reward;

import com.roguesmp.codec.Codec;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.quest.QuestReward;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;

import java.util.List;

public class MoneyReward implements QuestReward {

    public static final String TYPE_KEY = "money";
    public static final Codec<MoneyReward> CODEC = Codec.composite(
            Codec.LONG.fieldOf("amount").forGetter(MoneyReward::getAmount),
            MoneyReward::new
    );

    private final long amount;

    public MoneyReward(long amount) {
        this.amount = amount;
    }

    @Override
    public List<Component> getDisplay(SmpPlayer smpPlayer) {
        return List.of(Utils.fromString("<gold>" + Utils.formatMoney(amount) + " xu"));
    }

    @Override
    public String getTypeId() {
        return TYPE_KEY;
    }

    @Override
    public void giveReward(SmpPlayer smpPlayer) {
        smpPlayer.giveMoney(amount);
    }

    public long getAmount() {
        return amount;
    }
}
