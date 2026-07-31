package com.roguesmp.registry;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.codec.Codec;
import com.roguesmp.item.component.ItemComponentKeys;
import com.roguesmp.enchant.Enchants;
import com.roguesmp.entity.component.EntityComponentKeys;
import com.roguesmp.effect.EffectCodecs;
import com.roguesmp.effect.SmpEffect;
import com.roguesmp.entity.BaseEntity;
import com.roguesmp.entity.EntityFactory;
import com.roguesmp.entity.SmpEntity;
import com.roguesmp.entity.component.EntityComponent;
import com.roguesmp.entity.spell.SpellFactory;
import com.roguesmp.entity.spell.SpellParams;
import com.roguesmp.item.BaseItem;
import com.roguesmp.item.component.ItemComponent;
import com.roguesmp.npc.BaseNpc;
import com.roguesmp.npc.action.NpcAction;
import com.roguesmp.npc.action.NpcActions;
import com.roguesmp.player.ability.*;
import com.roguesmp.player.ability.upgrade.UpgradeRequirement;
import com.roguesmp.quest.*;
import com.roguesmp.player.ability.upgrade.UpgradeRequirements;
import com.roguesmp.entity.SpecialEntities;
import com.roguesmp.entity.spell.EntitySpells;
import com.roguesmp.npc.action.GuiOpenActions;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class Registries {
    private static final List<Consumer<RogueSmpCore>> BOOTSTRAPPERS = new ArrayList<>();

    public static final Registry<Codec<? extends ItemComponent>> ITEM_COMPONENT_CODEC = register(registry -> ItemComponentKeys.loadClass());
    public static final Registry<Codec<? extends EntityComponent>> ENTITY_COMPONENT_CODEC = register(registry -> EntityComponentKeys.loadClass());
    public static final Registry<Codec<? extends SmpEffect>> EFFECT_CODEC = register(registry -> EffectCodecs.loadClass());

    public static final Registry<Codec<? extends QuestObjective>> QUEST_OBJECTIVE_CODEC = register(registry -> QuestObjectives.loadClass());
    public static final Registry<Codec<? extends QuestRequirement>> QUEST_REQUIREMENT_CODEC = register(registry -> QuestRequirements.loadClass());
    public static final Registry<Codec<? extends QuestReward>> QUEST_REWARD_CODEC = register(registry -> QuestRewards.loadClass());
    // Populated as a side effect of QUEST_OBJECTIVE_CODEC's own loadClass() (each objective type
    // registers its progress codec alongside its own), not independently bootstrapped.
    public static final Registry<Codec<? extends ObjectiveProgress>> OBJECTIVE_PROGRESS_CODEC = new Registry<>();

    public static final Registry<Codec<? extends UpgradeRequirement>> ABILITY_UPGRADE_REQUIREMENT_CODEC = register(registry -> UpgradeRequirements.loadClass());

    public static final Registry<Codec<? extends NpcAction>> NPC_ACTION_CODEC = register(registry -> NpcActions.loadClass());

    // No bulk JSON entries of its own (Enchants is a hardcoded enum) - only exists so enchants
    // can have a "enchants/tags/*.json" folder like every other registry.
    public static final Registry<Enchants> ENCHANTS = register("enchants", registry -> Enchants.bootstrap());

    public static final Registry<SpellFactory<? extends SpellParams>> ENTITY_SPELL = register(registry -> EntitySpells.loadClass());
    public static final Registry<EntityFactory<? extends SmpEntity>> ENTITY_FACTORY = register(registry -> SpecialEntities.loadClass());

    public static final Registry<AbilityInfo<? extends Ability>> ABILITY = register(registry -> AbilityInfos.loadClass());
    public static final Registry<AbilityConfig> ABILITY_CONFIG = new Registry<>("ability_info", AbilityConfig.CODEC);
    public static final Registry<Predicate<Player>> TRIGGER_OPTION = register(registry -> TriggerOptions.loadClass());

    public static final Registry<GuiOpenActions.OpenAction> NPC_GUI_OPEN_ACTION = register(registry -> GuiOpenActions.loadClass());

    public static final Registry<SkinRegistry.SkinData> SKIN_DATA = new Registry<>("skins", SkinRegistry.SkinData.CODEC);

    public static final Registry<BaseItem> ITEM = new Registry<>("items", BaseItem.CODEC);
    public static final Registry<Quest> QUEST = new Registry<>("quests", Quest.CODEC);
    public static final Registry<BaseNpc> NPC = new Registry<>("npcs", BaseNpc.CODEC);
    public static final Registry<BaseEntity> ENTITY = new Registry<>("entities", BaseEntity.CODEC);

    public static void loadAllData(RogueSmpCore plugin) {
        Registry.loadAll(plugin);
        Registry.loadAllTags(plugin); // must run after loadAll, since tags resolve against already-loaded entries
    }

    // Create in-memory data here
    public static void boostrap(RogueSmpCore plugin) {
        // Every registry declared via register(Bootstrapper<T>) above runs its loadClass() here.
        BOOTSTRAPPERS.forEach(consumer -> consumer.accept(plugin));
    }

    /**
     * For registry that is not data driven, entry loaded entirely from code
     */
    private static <T> Registry<T> register(Bootstrapper<T> bootstrapper) {
        Registry<T> registry = new Registry<>();
        BOOTSTRAPPERS.add(rogueSmpCore -> bootstrapper.run(registry));
        return registry;
    }

    /**
     * For data-driven registry
     */
    private static <T> Registry<T> register(String key, Codec<T> entryCodec, Bootstrapper<T> bootstrapper) {
        Registry<T> registry = new Registry<>(key, entryCodec);
        BOOTSTRAPPERS.add(rogueSmpCore -> bootstrapper.run(registry));
        return registry;
    }

    /**
     * For registry that is not data driven, but still want to have tags
     */
    private static <T> Registry<T> register(String key, Bootstrapper<T> bootstrapper) {
        Registry<T> registry = new Registry<>(key);
        BOOTSTRAPPERS.add(rogueSmpCore -> bootstrapper.run(registry));
        return registry;
    }

    @FunctionalInterface
    public interface Bootstrapper<T> {
        void run(Registry<T> registry);
    }

}
