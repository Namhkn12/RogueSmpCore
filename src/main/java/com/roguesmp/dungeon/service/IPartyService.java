package com.roguesmp.dungeon.service;

import org.bukkit.entity.Player;

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
}