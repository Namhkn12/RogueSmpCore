package com.roguesmp.gate.requirement;

import com.roguesmp.codec.Codec;
import com.roguesmp.gate.requirement.impl.DateRequirement;
import com.roguesmp.gate.requirement.impl.ItemOwnedRequirement;
import com.roguesmp.gate.requirement.impl.LevelRequirement;
import com.roguesmp.gate.requirement.impl.MoneyBalanceRequirement;
import com.roguesmp.gate.requirement.impl.PermissionRequirement;
import com.roguesmp.gate.requirement.impl.QuestCompletedRequirement;
import com.roguesmp.registry.Registries;

public class Requirements {

    public static final Codec<LevelRequirement> LEVEL = register(LevelRequirement.TYPE_KEY, LevelRequirement.CODEC);
    public static final Codec<QuestCompletedRequirement> QUEST_COMPLETED = register(QuestCompletedRequirement.TYPE_KEY, QuestCompletedRequirement.CODEC);
    public static final Codec<PermissionRequirement> PERMISSION = register(PermissionRequirement.TYPE_KEY, PermissionRequirement.CODEC);
    public static final Codec<MoneyBalanceRequirement> MONEY = register(MoneyBalanceRequirement.TYPE_KEY, MoneyBalanceRequirement.CODEC);
    public static final Codec<ItemOwnedRequirement> ITEM = register(ItemOwnedRequirement.TYPE_KEY, ItemOwnedRequirement.CODEC);
    public static final Codec<DateRequirement> DATE = register(DateRequirement.TYPE_KEY, DateRequirement.CODEC);

    public static void loadClass() {

    }

    private static <T extends Requirement> Codec<T> register(String key, Codec<T> codec) {
        return Registries.REQUIREMENT_CODEC.register(key, codec);
    }
}
