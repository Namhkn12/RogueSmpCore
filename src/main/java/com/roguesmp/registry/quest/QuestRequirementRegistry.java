package com.roguesmp.registry.quest;

import com.roguesmp.codec.Codec;
import com.roguesmp.quest.QuestRequirement;
import com.roguesmp.quest.requirement.DateRequirement;
import com.roguesmp.registry.Registries;

public class QuestRequirementRegistry {

    public static void bootstrap() {
        register(DateRequirement.TYPE_KEY, DateRequirement.CODEC);
    }

    private static <T extends QuestRequirement> void register(String typeName, Codec<T> reqCodec) {
        Registries.QUEST_REQUIREMENT_CODEC.register(typeName, reqCodec);
    }
}
