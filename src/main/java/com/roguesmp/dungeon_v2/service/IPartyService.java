package com.roguesmp.dungeon_v2.service;

import com.roguesmp.dungeon_v2.data.runtime.Party;
import org.bukkit.entity.Player;

import java.util.List;

public interface IPartyService {

    Party createParty(Player owner);

    void disbandParty(Player owner);

    void joinParty(Player player, Player owner);

    void leaveParty(Player player);

    void kickMember(Player owner, Player target);

    void transferOwnership(Player currentOwner, Player newOwner);

    String buildPartyInfo(Player player);

    boolean isInParty(Player player);

    boolean isOwner(Player player);

    Party getPartyByPlayer(Player player);

    Party getPartyById(String partyId);

    void savePartyToFile();

    List<Player> getOnlineMembers(Party party);

}
