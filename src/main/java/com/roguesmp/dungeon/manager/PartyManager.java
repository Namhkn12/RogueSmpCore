package com.roguesmp.dungeon.manager;

import com.roguesmp.dungeon.constant.PrefixConfig;
import com.roguesmp.dungeon.data.Party;
import com.roguesmp.dungeon.repository.IPartyRepository;
import com.roguesmp.dungeon.utils.ConsoleLogger;
import com.roguesmp.dungeon.utils.Log4Craft;

import java.util.*;

/**
 * Quản lý data của Party — gọi repository để đọc/ghi,
 * không chứa business logic hay Bukkit API.
 */
public class PartyManager {

    private static final int DEFAULT_PARTY_SIZE = 3;

    // In-memory cache — chuyển từ repo sang đây
    private final Map<UUID, Party> partyMap = new HashMap<>();
    private final Map<UUID, UUID> playerIndex = new HashMap<>();

    private final IPartyRepository repository;

    public PartyManager(IPartyRepository repository) {
        this.repository = repository;

        loadAll();
    }

    public void loadAll() {
        Collection<Party> parties = repository.loadAll();
        for (Party party : parties) {
            partyMap.put(party.getPartyId(), party);
            for (UUID memberId : party.getMembers()) {
                playerIndex.put(memberId, party.getPartyId());
            }
        }
        Log4Craft.success("Loaded party to cache: " + parties.size() + " party");
    }

    // Gọi khi server stop — save từng party một
    public void saveAll() {
        for (Party party : partyMap.values()) {
            repository.save(party);
        }
        Log4Craft.success("Saved party to file: " + partyMap.size() + " party");
    }

    public Party createParty(UUID ownerId) {
        UUID partyId = UUID.randomUUID();
        List<UUID> members = new ArrayList<>();
        members.add(ownerId);

        Party party = new Party(partyId, ownerId, members, DEFAULT_PARTY_SIZE, true);
        partyMap.put(partyId, party);
        playerIndex.put(ownerId, partyId);
        return party;
    }

    public void disbandParty(UUID partyId) {
        Party party = partyMap.remove(partyId);
        if (party == null) return;
        for (UUID memberId : party.getMembers()) {
            playerIndex.remove(memberId);
        }
        repository.delete(partyId);
    }

    public void addMember(UUID partyId, UUID playerId) {
        Party party = partyMap.get(partyId);
        if (party == null) return;
        party.getMembers().add(playerId);
        playerIndex.put(playerId, partyId);
    }

    public void removeMember(UUID partyId, UUID playerId) {
        Party party = partyMap.get(partyId);
        if (party == null) return;
        party.getMembers().remove(playerId);
        playerIndex.remove(playerId);
    }

    public void transferOwner(UUID partyId, UUID newOwnerId) {
        Party party = partyMap.get(partyId);
        if (party == null) return;
        party.setOwner(newOwnerId);
    }

    public Optional<Party> findByPlayer(UUID playerId) {
        UUID partyId = playerIndex.get(playerId);
        if (partyId == null) return Optional.empty();
        return Optional.ofNullable(partyMap.get(partyId));
    }

    public Optional<Party> findById(UUID partyId) {
        return Optional.ofNullable(partyMap.get(partyId));
    }

    public boolean isInParty(UUID playerId) {
        return playerIndex.containsKey(playerId);
    }

    public boolean isOwner(UUID playerId) {
        return findByPlayer(playerId)
                .map(party -> party.getOwner().equals(playerId))
                .orElse(false);
    }
}
