package com.roguesmp.gate.requirement;

import com.roguesmp.codec.Codec;
import com.roguesmp.gate.requirement.impl.AbilityRequirement;
import com.roguesmp.gate.requirement.impl.AdventureLevelRequirement;
import com.roguesmp.gate.requirement.impl.ClassRequirement;
import com.roguesmp.gate.requirement.impl.CompoundRequirement;
import com.roguesmp.gate.requirement.impl.ExpLevelRequirement;
import com.roguesmp.gate.requirement.impl.HasIslandRequirement;
import com.roguesmp.gate.requirement.impl.ItemOwnedRequirement;
import com.roguesmp.gate.requirement.impl.IslandLevelRequirement;
import com.roguesmp.gate.requirement.impl.MoneyBalanceRequirement;
import com.roguesmp.gate.requirement.impl.NotRequirement;
import com.roguesmp.gate.requirement.impl.PermissionRequirement;
import com.roguesmp.gate.requirement.impl.PlaceholderRequirement;
import com.roguesmp.gate.requirement.impl.QuestActiveRequirement;
import com.roguesmp.gate.requirement.impl.QuestCompletedRequirement;
import com.roguesmp.registry.Registries;

public class Requirements {

    public static final Codec<IslandLevelRequirement> ISLAND_LEVEL = register(IslandLevelRequirement.TYPE_KEY, IslandLevelRequirement.CODEC);
    public static final Codec<QuestCompletedRequirement> QUEST_COMPLETED = register(QuestCompletedRequirement.TYPE_KEY, QuestCompletedRequirement.CODEC);
    public static final Codec<PermissionRequirement> PERMISSION = register(PermissionRequirement.TYPE_KEY, PermissionRequirement.CODEC);
    public static final Codec<MoneyBalanceRequirement> MONEY = register(MoneyBalanceRequirement.TYPE_KEY, MoneyBalanceRequirement.CODEC);
    public static final Codec<ItemOwnedRequirement> ITEM = register(ItemOwnedRequirement.TYPE_KEY, ItemOwnedRequirement.CODEC);
    public static final Codec<ExpLevelRequirement> EXP_LEVEL = register(ExpLevelRequirement.TYPE_KEY, ExpLevelRequirement.CODEC);
    public static final Codec<AdventureLevelRequirement> ADVENTURE_LEVEL = register(AdventureLevelRequirement.TYPE_KEY, AdventureLevelRequirement.CODEC);
    public static final Codec<ClassRequirement> CLASS = register(ClassRequirement.TYPE_KEY, ClassRequirement.CODEC);
    public static final Codec<AbilityRequirement> ABILITY = register(AbilityRequirement.TYPE_KEY, AbilityRequirement.CODEC);
    public static final Codec<HasIslandRequirement> HAS_ISLAND = register(HasIslandRequirement.TYPE_KEY, HasIslandRequirement.CODEC);
    public static final Codec<QuestActiveRequirement> QUEST_ACTIVE = register(QuestActiveRequirement.TYPE_KEY, QuestActiveRequirement.CODEC);
    public static final Codec<PlaceholderRequirement> PLACEHOLDER = register(PlaceholderRequirement.TYPE_KEY, PlaceholderRequirement.CODEC);
    public static final Codec<NotRequirement> NOT = register(NotRequirement.TYPE_KEY, NotRequirement.CODEC);
    public static final Codec<CompoundRequirement> ANY_OF = register(CompoundRequirement.ANY_OF_KEY, CompoundRequirement.ANY_OF_CODEC);
    public static final Codec<CompoundRequirement> ALL_OF = register(CompoundRequirement.ALL_OF_KEY, CompoundRequirement.ALL_OF_CODEC);

    public static void loadClass() {

    }

    private static <T extends Requirement> Codec<T> register(String key, Codec<T> codec) {
        return Registries.REQUIREMENT_CODEC.register(key, codec);
    }
}
