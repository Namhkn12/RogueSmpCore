package com.roguesmp.quest.reward;

import com.roguesmp.codec.Codec;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.quest.board.QuestBoardManager;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;

import java.util.List;

public class QuestExpReward implements QuestReward {

    public static final String TYPE_KEY = "quest_exp";
    public static final Codec<QuestExpReward> CODEC = Codec.composite(
            Codec.LONG.fieldOf("amount").forGetter(QuestExpReward::getAmount),
            QuestExpReward::new
    );

    private final long amount;

    public QuestExpReward(long amount) {
        this.amount = amount;
    }

    @Override
    public List<Component> getDisplay(SmpPlayer smpPlayer) {
        return List.of(Utils.fromString("<aqua>" + amount + " kinh nghiệm Mạo hiểm"));
    }

    @Override
    public String getTypeId() {
        return TYPE_KEY;
    }

    @Override
    public void giveReward(SmpPlayer smpPlayer) {
        QuestBoardManager.getInstance().giveExp(smpPlayer.getBukkitPlayer(), amount);
    }

    public long getAmount() {
        return amount;
    }
}
