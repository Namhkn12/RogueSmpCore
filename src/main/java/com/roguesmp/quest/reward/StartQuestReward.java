package com.roguesmp.quest.reward;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.codec.Codec;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.quest.Quest;
import com.roguesmp.quest.QuestManager;
import com.roguesmp.quest.holder.PlayerHolder;
import com.roguesmp.registry.Registries;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;

import java.util.List;

/**
 * Starts another quest for the player, used to chain quests. Keeps the quest as a plain id so an inbox entry still
 * loads after that quest is removed.
 */
public class StartQuestReward implements QuestReward {

    public static final String TYPE_KEY = "start_quest";
    public static final Codec<StartQuestReward> CODEC = Codec.composite(
            Codec.STRING.fieldOf("quest").forGetter(StartQuestReward::getQuestId),
            StartQuestReward::new
    );

    private final String questId;

    public StartQuestReward(String questId) {
        this.questId = questId;
    }

    @Override
    public List<Component> getDisplay(SmpPlayer smpPlayer) {
        Quest quest = Registries.QUEST.get(questId);
        String name = quest == null || quest.getName() == null ? questId : quest.getName();
        return List.of(Utils.fromString("<aqua>Mở khóa nhiệm vụ: </aqua>" + name));
    }

    @Override
    public String getTypeId() {
        return TYPE_KEY;
    }

    @Override
    public void giveReward(SmpPlayer smpPlayer) {
        Quest quest = Registries.QUEST.get(questId);
        if (quest == null) {
            RogueSmpCore.LOGGER.warn("start_quest reward points at unknown quest '{}'", questId);
            return;
        }
        QuestManager.getInstance().startQuest(new PlayerHolder(smpPlayer.getBukkitPlayer().getUniqueId()), quest);
    }

    public String getQuestId() {
        return questId;
    }
}
