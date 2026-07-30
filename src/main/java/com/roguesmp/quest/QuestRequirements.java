package com.roguesmp.quest;

import com.roguesmp.codec.Codec;
import com.roguesmp.quest.requirement.DateRequirement;
import com.roguesmp.registry.Registries;

/**
 * Every {@link QuestRequirement} codec. Each constant registers itself into
 * {@link Registries#QUEST_REQUIREMENT_CODEC} as it's initialized - call {@link #loadClass()} to
 * force that to happen.
 */
public class QuestRequirements {

    public static final Codec<DateRequirement> DATE = register(DateRequirement.TYPE_KEY, DateRequirement.CODEC);

    public static void loadClass() {

    }

    private static <T extends QuestRequirement> Codec<T> register(String typeName, Codec<T> codec) {
        return Registries.QUEST_REQUIREMENT_CODEC.register(typeName, codec);
    }
}
