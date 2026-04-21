package com.roguesmp.dungeon.data.definition.room.roomevent;

import com.roguesmp.dungeon.data.runtime.Party;
import com.roguesmp.dungeon.data.runtime.RoomInstance;
import com.roguesmp.dungeon.service.IPartyService;

public class RoomEventContext {

    private final RoomInstance roomInstance;
    private final Party party;
    private final IPartyService partyService;

    public RoomEventContext(RoomInstance roomInstance, Party party, IPartyService partyService) {
        this.roomInstance = roomInstance;
        this.party = party;
        this.partyService = partyService;
    }

    public RoomInstance getRoomInstance() {
        return roomInstance;
    }

    public Party getParty() {
        return party;
    }

    public IPartyService getPartyService() {
        return partyService;
    }
}
