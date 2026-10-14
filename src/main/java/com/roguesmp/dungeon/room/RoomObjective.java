package com.roguesmp.dungeon.room;

import com.roguesmp.codec.Codec;
import com.roguesmp.dungeon.script.DungeonCondition;
import com.roguesmp.dungeon.script.EventMatcher;

import java.util.List;

// Events with a location only count inside the room.
public record RoomObjective(EventMatcher on, List<DungeonCondition> conditions, int count, int score, boolean endsDungeon, String display) {

    public static final Codec<RoomObjective> CODEC = Codec.composite(
            EventMatcher.CODEC.fieldOf("on").forGetter(RoomObjective::on),
            Codec.listOf(DungeonCondition.CODEC).optionalFieldOf("conditions", List.of()).forGetter(RoomObjective::conditions),
            Codec.INT.optionalFieldOf("count", 1).forGetter(RoomObjective::count),
            Codec.INT.optionalFieldOf("score", 0).forGetter(RoomObjective::score),
            Codec.BOOLEAN.optionalFieldOf("ends_dungeon", false).forGetter(RoomObjective::endsDungeon),
            Codec.STRING.optionalFieldOf("display", "").forGetter(RoomObjective::display),
            RoomObjective::new
    );
}
