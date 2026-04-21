package com.roguesmp.dungeon.controller;

import com.roguesmp.dungeon.data.runtime.Party;
import com.roguesmp.dungeon.service.IPartyService;
import com.roguesmp.dungeon.task.PartyInviteTask;
import org.bukkit.entity.Player;

public class PartyController {

    private final IPartyService partyService;
    private final PartyInviteTask inviteTask;

    public PartyController(IPartyService partyService, PartyInviteTask inviteTask) {
        this.partyService = partyService;
        this.inviteTask = inviteTask;
    }

    public void handleCreateParty(Player player) {
        Party party = partyService.createParty(player);
        if(party == null){
            return;
        }
        return;
    }

    public void handleDisbandParty(Player player) {
        partyService.disbandParty(player);
        return;
    }

    public void handleInvitePlayer(Player player, Player target) {
        inviteTask.sendInvite(player, target);
    }

    public void handleAcceptInvite(Player player, String inviterName) {
        inviteTask.acceptInvite(player, inviterName);
    }

    public void handleDenyInvite(Player player, String inviterName) {
        inviteTask.denyInvite(player, inviterName);
    }

    public void handleLeaveParty(Player player) {
        partyService.leaveParty(player);
    }

    public void handeShowPartyInfo(Player player) {
        player.sendMessage(partyService.buildPartyInfo(player));
    }

}
