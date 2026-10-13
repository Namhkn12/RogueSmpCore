package com.roguesmp.dungeon.data.definition.room.roomevent;

import com.roguesmp.dungeon_party.Party;
import com.roguesmp.dungeon.data.runtime.RoomInstance;
import com.roguesmp.dungeon_party.PartyManager;

public class RoomEventContext {

    private final RoomInstance roomInstance;
    private final Party party;
    private final PartyManager partyManager;

    public RoomEventContext(RoomInstance roomInstance, Party party, PartyManager partyManager) {
        this.roomInstance = roomInstance;
        this.party = party;
        this.partyManager = partyManager;
    }

    public RoomInstance getRoomInstance() {
        return roomInstance;
    }

    public Party getParty() {
        return party;
    }

    public PartyManager getPartyService() {
        return partyManager;
    }
}
