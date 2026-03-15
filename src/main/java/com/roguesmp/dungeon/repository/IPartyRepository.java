package com.roguesmp.dungeon.repository;

import com.roguesmp.dungeon.data.Party;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface IPartyRepository {

    /**
     * Lưu 1 party vào file json
     */
    void save(Party party);

    /**
     * Xóa party data file
     */
    void delete(UUID partyId);

    /**
     * Load tất cả các party được lưu file
     */
    Collection<Party> loadAll();
}
