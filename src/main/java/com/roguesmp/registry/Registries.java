package com.roguesmp.registry;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.codec.Codec;
import com.roguesmp.effect.SmpEffect;
import com.roguesmp.item.BaseItem;
import com.roguesmp.item.component.ItemComponent;
import com.roguesmp.item.modifier.ItemModifier;
import com.roguesmp.npc.BaseNpc;
import com.roguesmp.npc.SmpNpc;
import com.roguesmp.npc.action.NpcAction;
import com.roguesmp.quest.*;
import com.roguesmp.registry.npc.GuiOpenActionRegistry;
import com.roguesmp.registry.npc.NpcActionRegistry;
import com.roguesmp.registry.quest.QuestObjectiveRegistry;
import com.roguesmp.registry.quest.QuestRequirementRegistry;
import com.roguesmp.registry.quest.QuestRewardRegistry;

public class Registries {

    public static final Registry<Codec<? extends ItemComponent>> ITEM_COMPONENT_CODEC = new Registry<>();
    public static final Registry<Codec<? extends SmpEffect>> EFFECT_CODEC = new Registry<>();

    public static final Registry<Codec<? extends QuestObjective>> QUEST_OBJECTIVE_CODEC = new Registry<>();
    public static final Registry<Codec<? extends QuestRequirement>> QUEST_REQUIREMENT_CODEC = new Registry<>();
    public static final Registry<Codec<? extends QuestReward>> QUEST_REWARD_CODEC = new Registry<>();
    public static final Registry<Codec<? extends ObjectiveProgress>> OBJECTIVE_PROGRESS_CODEC = new Registry<>();

    public static final Registry<Codec<? extends NpcAction>> NPC_ACTION_CODEC = new Registry<>();
    public static final Registry<GuiOpenActionRegistry.OpenAction> NPC_GUI_OPEN_ACTION = new Registry<>();

    public static final Registry<BaseItem> ITEM = new Registry<>("items", BaseItem.CODEC);
    public static final Registry<Quest> QUEST = new Registry<>("quests", Quest.CODEC);
    public static final Registry<BaseNpc> NPC = new Registry<>("npcs", BaseNpc.CODEC);

    public static final Registry<SkinRegistry.SkinData> SKIN_DATA = new Registry<>("skins", SkinRegistry.SkinData.CODEC);

    public static void loadAllData(RogueSmpCore plugin) {
        Registry.loadAll(plugin);
    }

    // Create in-memory data here
    public static void boostrap(RogueSmpCore plugin) {
        //ITEM_COMPONENT_CODEC use ComponentKeys for bootstrapping
        EffectCodecRegistry.bootstrap();

        QuestRequirementRegistry.bootstrap();
        QuestRewardRegistry.bootstrap();
        QuestObjectiveRegistry.bootstrap();

        NpcActionRegistry.bootstrap();
        GuiOpenActionRegistry.bootstrap();
    }

}
