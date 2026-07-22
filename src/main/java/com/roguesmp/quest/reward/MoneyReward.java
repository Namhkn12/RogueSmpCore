package com.roguesmp.quest.reward;

import com.roguesmp.player.SmpPlayer;
import com.roguesmp.quest.QuestReward;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;

import java.util.List;

public class MoneyReward implements QuestReward {

    private final long amount;

    public MoneyReward(long amount) {
        this.amount = amount;
    }

    @Override
    public List<Component> getDisplay(SmpPlayer smpPlayer) {
        return List.of(Utils.fromString("<gold>" + Utils.formatMoney(amount) + " xu"));
    }

    @Override
    public void giveReward(SmpPlayer smpPlayer) {
        smpPlayer.giveMoney(amount);
    }


}
