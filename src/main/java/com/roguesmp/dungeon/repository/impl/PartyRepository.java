package com.roguesmp.dungeon.repository.impl;

import com.roguesmp.dungeon.data.Party;
import com.roguesmp.dungeon.repository.IPartyRepository;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class PartyRepository implements IPartyRepository {

    // partyId → Party
    private final Map<UUID, Party> partyMap = new HashMap<>();

    // playerId → partyId (lookup index)
    private final Map<UUID, UUID> playerIndex = new HashMap<>();

    @Override
    public void save(Party party) {
        partyMap.put(party.getPartyId(), party);
    }

    @Override
    public void delete(UUID partyId) {
        partyMap.remove(partyId);
    }

    @Override
    public Optional<Party> findById(UUID partyId) {
        return Optional.ofNullable(partyMap.get(partyId));
    }

    @Override
    public Optional<Party> findByPlayer(UUID playerId) {
        UUID partyId = playerIndex.get(playerId);
        if (partyId == null) return Optional.empty();
        return findById(partyId);
    }

    @Override
    public boolean existsByPlayer(UUID playerId) {
        return playerIndex.containsKey(playerId);
    }

    @Override
    public Collection<Party> findAll() {
        return Collections.unmodifiableCollection(partyMap.values());
    }

    @Override
    public void indexPlayer(UUID playerId, UUID partyId) {
        playerIndex.put(playerId, partyId);
    }

    @Override
    public void removePlayerIndex(UUID playerId) {
        playerIndex.remove(playerId);
    }
}
