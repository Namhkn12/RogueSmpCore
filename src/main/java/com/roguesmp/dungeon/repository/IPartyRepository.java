package com.roguesmp.dungeon.repository;

import com.roguesmp.dungeon.data.Party;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface IPartyRepository {
    void save(Party party);
    void delete(UUID partyId);
    Collection<Party> loadAll();
}
