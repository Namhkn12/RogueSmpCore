package com.roguesmp.dungeon.room;

import com.roguesmp.codec.Codec;
import com.roguesmp.dungeon.EventMatcher;

// Events with a location only count inside the room.
public record RoomObjective(EventMatcher on, int count, int score, boolean endsDungeon, String display) {

    public static final Codec<RoomObjective> CODEC = Codec.composite(
            EventMatcher.CODEC.fieldOf("on").forGetter(RoomObjective::on),
            Codec.INT.optionalFieldOf("count", 1).forGetter(RoomObjective::count),
            Codec.INT.optionalFieldOf("score", 0).forGetter(RoomObjective::score),
            Codec.BOOLEAN.optionalFieldOf("ends_dungeon", false).forGetter(RoomObjective::endsDungeon),
            Codec.STRING.optionalFieldOf("display", "").forGetter(RoomObjective::display),
            RoomObjective::new
    );
}
