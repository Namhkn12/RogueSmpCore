package com.roguesmp.dungeon.repository;

import com.roguesmp.dungeon.data.Party;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface IPartyRepository {

    void save(Party party);

    void delete(UUID partyId);

    Optional<Party> findById(UUID partyId);

    Optional<Party> findByPlayer(UUID playerId);

    boolean existsByPlayer(UUID playerId);

    Collection<Party> findAll();

    void indexPlayer(UUID playerId, UUID partyId);

    void removePlayerIndex(UUID playerId);
}
