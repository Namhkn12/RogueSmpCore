package com.roguesmp.quest.board;

import com.roguesmp.codec.Codec;

/**
 * A quest offered on a player's board for one reset cycle.
 */
public record BoardEntry(String questId, long cycle) {

    public static final Codec<BoardEntry> CODEC = Codec.composite(
            Codec.STRING.fieldOf("quest").forGetter(BoardEntry::questId),
            Codec.LONG.fieldOf("cycle").forGetter(BoardEntry::cycle),
            BoardEntry::new
    );
}
