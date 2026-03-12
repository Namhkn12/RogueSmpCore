package com.roguesmp.dungeon.service;

import com.roguesmp.dungeon.data.Party;
import org.bukkit.entity.Player;

import java.util.Optional;

public interface IPartyService {

    void createParty(Player owner);

    void disbandParty(Player owner);

    void joinParty(Player player, Player owner);

    void leaveParty(Player player);

    void kickMember(Player owner, Player target);

    void transferOwnership(Player currentOwner, Player newOwner);

    String buildPartyInfo(Player player);

    boolean isInParty(Player player);

    boolean isOwner(Player player);

    Optional<Party> getPartyByPlayer(Player player);
}