package com.roguesmp.dungeon.feature;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.codec.Codec;
import com.roguesmp.dungeon.DungeonFeature;
import com.roguesmp.dungeon.DungeonInstance;
import com.roguesmp.dungeon.DungeonEvent;
import com.roguesmp.dungeon.FeatureContext;
import com.roguesmp.dungeon.NoState;
import com.roguesmp.quest.Quest;
import com.roguesmp.quest.QuestManager;
import com.roguesmp.quest.holder.GroupHolder;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public record QuestsFeature(List<Quest> quests, boolean eligibleOnEnter) implements DungeonFeature<NoState> {

    public static final String TYPE_KEY = "quests";
    public static final String OWNER = "dungeon";
    public static final String KILL_SIGNAL_PREFIX = "kill:";

    public static final Codec<QuestsFeature> CODEC = Codec.composite(
            Codec.listOf(Quest.REFERENCE_CODEC).fieldOf("quests").forGetter(QuestsFeature::quests),
            Codec.BOOLEAN.optionalFieldOf("eligible_on_enter", true).forGetter(QuestsFeature::eligibleOnEnter),
            QuestsFeature::new
    );

    public static void sweep(Set<UUID> liveRunIds) {
        int ended = QuestManager.getInstance().sweepGroups(OWNER, liveRunIds);
        if (ended > 0) RogueSmpCore.LOGGER.info("Ended {} quest group(s) of dungeon runs that no longer exist", ended);
    }

    @Override
    public String getTypeId() {
        return TYPE_KEY;
    }

    @Override
    public NoState createState() {
        return NoState.INSTANCE;
    }

    @Override
    public Codec<NoState> stateCodec() {
        return NoState.CODEC;
    }

    @Override
    public void onStart(FeatureContext<NoState> context) {
        GroupHolder holder = holder(context.instance());
        QuestManager questManager = QuestManager.getInstance();
        questManager.bindGroup(holder).thenRun(() -> {
            questManager.startQuests(holder, quests);
            context.instance().members().forEach(member -> questManager.addEligible(holder, member));
        });
    }

    @Override
    public void onRestore(FeatureContext<NoState> context) {
        QuestManager.getInstance().bindGroup(holder(context.instance()));
    }

    @Override
    public void onEvent(FeatureContext<NoState> context, DungeonEvent event) {
        QuestManager questManager = QuestManager.getInstance();
        GroupHolder holder = holder(context.instance());
        switch (event) {
            case DungeonEvent.PlayerEntered entered when eligibleOnEnter -> questManager.addEligible(holder, entered.player().getUniqueId());
            case DungeonEvent.Custom custom -> questManager.signal(holder, custom.id(), custom.amount());
            case DungeonEvent.MobKilled killed -> questManager.signal(holder, KILL_SIGNAL_PREFIX + killed.mobId(), 1);
            default -> {
            }
        }
    }

    @Override
    public void onEnd(FeatureContext<NoState> context) {
        QuestManager.getInstance().endGroup(holder(context.instance()));
    }

    public static GroupHolder holder(DungeonInstance instance) {
        return new GroupHolder(instance.id(), OWNER, instance::members);
    }
}
