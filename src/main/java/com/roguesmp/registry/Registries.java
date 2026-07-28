package com.roguesmp.registry;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.codec.Codec;
import com.roguesmp.constant.Enchants;
import com.roguesmp.effect.SmpEffect;
import com.roguesmp.entity.BaseEntity;
import com.roguesmp.entity.EntityFactory;
import com.roguesmp.entity.component.EntityComponent;
import com.roguesmp.entity.spell.SpellFactory;
import com.roguesmp.entity.spell.SpellParams;
import com.roguesmp.item.BaseItem;
import com.roguesmp.item.component.ItemComponent;
import com.roguesmp.npc.BaseNpc;
import com.roguesmp.npc.action.NpcAction;
import com.roguesmp.player.ability.Ability;
import com.roguesmp.player.ability.AbilityInfo;
import com.roguesmp.player.ability.upgrade.UpgradeRequirement;
import com.roguesmp.quest.*;
import com.roguesmp.registry.ability.AbilityInfoRegistry;
import com.roguesmp.registry.ability.UpgradeRequirementRegistry;
import com.roguesmp.registry.entity.EntityFactoryRegistry;
import com.roguesmp.registry.entity.EntitySpellRegistry;
import com.roguesmp.registry.npc.GuiOpenActionRegistry;
import com.roguesmp.registry.npc.NpcActionRegistry;
import com.roguesmp.registry.quest.QuestObjectiveRegistry;
import com.roguesmp.registry.quest.QuestRequirementRegistry;
import com.roguesmp.registry.quest.QuestRewardRegistry;

public class Registries {

    public static final Registry<Codec<? extends ItemComponent>> ITEM_COMPONENT_CODEC = new Registry<>();
    public static final Registry<Codec<? extends EntityComponent>> ENTITY_COMPONENT_CODEC = new Registry<>();
    public static final Registry<SpellFactory<?>> ENTITY_SPELL = new Registry<>();
    public static final Registry<EntityFactory> ENTITY_FACTORY = new Registry<>();
    public static final Registry<Codec<? extends SmpEffect>> EFFECT_CODEC = new Registry<>();

    public static final Registry<Codec<? extends QuestObjective>> QUEST_OBJECTIVE_CODEC = new Registry<>();
    public static final Registry<Codec<? extends QuestRequirement>> QUEST_REQUIREMENT_CODEC = new Registry<>();
    public static final Registry<Codec<? extends QuestReward>> QUEST_REWARD_CODEC = new Registry<>();
    public static final Registry<Codec<? extends ObjectiveProgress>> OBJECTIVE_PROGRESS_CODEC = new Registry<>();

    public static final Registry<Codec<? extends UpgradeRequirement>> ABILITY_UPGRADE_REQUIREMENT_CODEC = new Registry<>();

    // Entries hardcoded via AbilityInfoRegistry.bootstrap(); tunable data (scaling/trigger/upgrades)
    // is overlaid onto them separately by AbilityInfoRegistry.loadAll() from ability_info/*.json.
    public static final Registry<AbilityInfo<? extends Ability>> ABILITY = new Registry<>();

    public static final Registry<Codec<? extends NpcAction>> NPC_ACTION_CODEC = new Registry<>();
    public static final Registry<GuiOpenActionRegistry.OpenAction> NPC_GUI_OPEN_ACTION = new Registry<>();

    public static final Registry<SkinRegistry.SkinData> SKIN_DATA = new Registry<>("skins", SkinRegistry.SkinData.CODEC);

    public static final Registry<BaseItem> ITEM = new Registry<>("items", BaseItem.CODEC);
    public static final Registry<Quest> QUEST = new Registry<>("quests", Quest.CODEC);
    public static final Registry<BaseNpc> NPC = new Registry<>("npcs", BaseNpc.CODEC);
    public static final Registry<BaseEntity> ENTITY = new Registry<>("entities", BaseEntity.CODEC);

    // No bulk JSON entries of its own (Enchants is a hardcoded enum) - only exists so enchants
    // can have a "enchants/tags/*.json" folder like every other registry.
    public static final Registry<Enchants> ENCHANTS = new Registry<>("enchants");

    public static void loadAllData(RogueSmpCore plugin) {
        Registry.loadAll(plugin);
        //tunable data (scaling/trigger/upgrades) is overlaid onto Registries.ABILITY separately
        //Since this runs after Registry.loadAll(), the REFERENCE_CODEC should work fine
        AbilityInfoRegistry.loadAll();
        Registry.loadAllTags(plugin); // must run after loadAll, since tags resolve against already-loaded entries
    }

    // Create in-memory data here
    public static void boostrap(RogueSmpCore plugin) {
        //ITEM_COMPONENT_CODEC use ComponentKeys for bootstrapping
        EffectCodecRegistry.bootstrap();

        QuestRequirementRegistry.bootstrap();
        QuestRewardRegistry.bootstrap();
        QuestObjectiveRegistry.bootstrap();

        UpgradeRequirementRegistry.bootstrap();
        AbilityInfoRegistry.bootstrap();
        EntitySpellRegistry.bootstrap();
        EntityFactoryRegistry.bootstrap();

        NpcActionRegistry.bootstrap();
        GuiOpenActionRegistry.bootstrap();

        for (Enchants enchant : Enchants.values()) {
            ENCHANTS.register(enchant.getEnchant().getId(), enchant);
        }
    }

}
