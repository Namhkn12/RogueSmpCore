package com.roguesmp.dungeon.manager;

import com.roguesmp.dungeon.data.Party;
import com.roguesmp.dungeon.repository.IPartyRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Quản lý data của Party — gọi repository để đọc/ghi,
 * không chứa business logic hay Bukkit API.
 */
public class PartyManager {

    private static final int DEFAULT_PARTY_SIZE = 3;

    private final IPartyRepository repository;

    public PartyManager(IPartyRepository repository) {
        this.repository = repository;
    }


    public Party createParty(UUID ownerId) {
        UUID partyId = UUID.randomUUID();
        List<UUID> members = new ArrayList<>();
        members.add(ownerId);

        Party party = new Party(partyId, ownerId, members, DEFAULT_PARTY_SIZE, true);

        repository.save(party);
        repository.indexPlayer(ownerId, partyId);

        return party;
    }

    public void disbandParty(UUID partyId) {
        Party party = repository.findById(partyId).orElse(null);
        if (party == null) return;

        for (UUID memberId : party.getMembers()) {
            repository.removePlayerIndex(memberId);
        }

        repository.delete(partyId);
    }

    public void addMember(UUID partyId, UUID playerId) {
        Party party = repository.findById(partyId).orElse(null);
        if (party == null) return;

        party.getMembers().add(playerId);
        repository.save(party);
        repository.indexPlayer(playerId, partyId);
    }

    public void removeMember(UUID partyId, UUID playerId) {
        Party party = repository.findById(partyId).orElse(null);
        if (party == null) return;

        party.getMembers().remove(playerId);
        repository.save(party);
        repository.removePlayerIndex(playerId);
    }

    public void transferOwner(UUID partyId, UUID newOwnerId) {
        Party party = repository.findById(partyId).orElse(null);
        if (party == null) return;

        party.setOwner(newOwnerId);
        repository.save(party);
    }


    public Optional<Party> findByPlayer(UUID playerId) {
        return repository.findByPlayer(playerId);
    }

    public Optional<Party> findById(UUID partyId) {
        return repository.findById(partyId);
    }

    public boolean isInParty(UUID playerId) {
        return repository.existsByPlayer(playerId);
    }

    public boolean isOwner(UUID playerId) {
        return repository.findByPlayer(playerId)
                .map(party -> party.getOwner().equals(playerId))
                .orElse(false);
    }
}
