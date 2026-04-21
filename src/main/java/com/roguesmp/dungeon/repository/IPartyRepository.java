package com.roguesmp.dungeon.repository;


import com.roguesmp.dungeon.data.runtime.Party;
import java.util.Collection;

public interface IPartyRepository {

    /**
     * Lưu 1 party vào file json
     */
    void save(Party party);

    /**
     * Xóa party data file
     */
    void delete(String partyId);

    /**
     * Load tất cả các party được lưu file
     */
    Collection<Party> loadAll();
}
