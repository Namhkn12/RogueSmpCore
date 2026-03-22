package com.roguesmp.dungeon.controller;

import com.roguesmp.dungeon.dto.ActionResult;
import com.roguesmp.dungeon.data.Party;
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

    public ActionResult<Party> getPartyByPlayer(Player player){
        Party party = partyService.getPartyByPlayer(player).orElse(null);
        return ActionResult.ok("Got party info from player successfully", party);
    }

    public ActionResult<Void> createParty(Player player) {
        Party party = partyService.createParty(player);
        if(party == null){
            return ActionResult.failed("Party created fail");
        }
        return ActionResult.ok("Party created successfully");
    }

    public ActionResult<Void> disbandParty(Player player) {
        partyService.disbandParty(player);
        return ActionResult.ok("Party disbanded");
    }

    public void invitePlayer(Player player, Player target) {
        inviteTask.sendInvite(player, target);
    }

    public void acceptInvite(Player player, String inviterName) {
        inviteTask.acceptInvite(player, inviterName);
    }

    public void denyInvite(Player player, String inviterName) {
        inviteTask.denyInvite(player, inviterName);
    }

    public void leaveParty(Player player) {
        partyService.leaveParty(player);
    }

    public void showPartyInfo(Player player) {
        player.sendMessage(partyService.buildPartyInfo(player));
    }
}
