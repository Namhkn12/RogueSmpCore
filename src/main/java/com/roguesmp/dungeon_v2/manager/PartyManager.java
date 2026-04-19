package com.roguesmp.dungeon_v2.manager;

import com.roguesmp.dungeon_v2.data.runtime.Party;
import com.roguesmp.dungeon_v2.repository.IPartyRepository;
import com.roguesmp.dungeon_v2.utils.Log4Craft_;

import java.util.*;

/**
 * Quản lý data của Party — gọi repository để đọc/ghi,
 * không chứa business logic hay Bukkit API.
 */
public class PartyManager {

    private static final int DEFAULT_PARTY_SIZE = 4;

    // In-memory cache — chuyển từ repo sang đây
    private final Map<String, Party> partyMap = new HashMap<>();
    private final Map<String, String> playerIndex = new HashMap<>();

    private final IPartyRepository repository;
    private final Log4Craft_ logger;

    public PartyManager(IPartyRepository repository, Log4Craft_ logger) {
        this.repository = repository;
        this.logger = logger;

        loadAll();
    }

    public void loadAll() {
        Collection<Party> parties = repository.loadAll();
        for (Party party : parties) {
            partyMap.put(party.getPartyId(), party);
            for (String memberId : party.getMembers()) {
                playerIndex.put(memberId, party.getPartyId());
            }
        }
        logger.info(this.getClass(), "Loaded party to cache: " + parties.size() + " party");
    }

    // Gọi khi server stop — save từng party một
    public void saveAll() {
        for (Party party : partyMap.values()) {
            repository.save(party);
        }
        logger.info(this.getClass(),"Saved party to file: " + partyMap.size() + " party");
    }

    public Party createParty(UUID ownerId) {
        UUID partyId = UUID.randomUUID();
        Party party = new Party(partyId.toString(), ownerId.toString(), DEFAULT_PARTY_SIZE);
        partyMap.put(partyId.toString(), party);
        playerIndex.put(ownerId.toString(), partyId.toString());
        return party;
    }

    public void disbandParty(String partyId) {
        Party party = partyMap.remove(partyId);
        if (party == null) return;
        for (String memberId : party.getMembers()) {
            playerIndex.remove(memberId);
        }
        repository.delete(partyId);
    }

    public void addMember(String partyId, String playerId) {
        Party party = partyMap.get(partyId);
        if (party == null) return;
        party.addMember(playerId);
        playerIndex.put(playerId, partyId);
    }

    public void removeMember(String partyId, String playerId) {
        Party party = partyMap.get(partyId);
        if (party == null) return;
        party.removeMember(playerId);
        playerIndex.remove(playerId);
    }

    public void transferOwner(String partyId, String newOwnerId) {
        Party party = partyMap.get(partyId);
        if (party == null) return;
        party.setOwner(newOwnerId);
    }

    public Party findByPlayer(UUID playerId) {
        String partyId = playerIndex.get(playerId.toString());
        if (partyId == null) return null;
        return partyMap.get(partyId);
    }

    public Party findById(String partyId) {
        return partyMap.get(partyId);
    }

    public boolean isInParty(UUID playerId) {
        return playerId != null && playerIndex.containsKey(playerId.toString());
    }

    public boolean checkIsOwner(UUID playerId) {
        Party party = findByPlayer(playerId);
        return party != null && party.isOwner(playerId.toString());
    }
}
