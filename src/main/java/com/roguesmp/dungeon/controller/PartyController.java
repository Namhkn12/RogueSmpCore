package com.roguesmp.dungeon.controller;

import com.roguesmp.dungeon.service.IPartyService;
import com.roguesmp.dungeon.task.PartyInviteTask;
import org.bukkit.entity.Player;

/**
 * Nhận request từ PartyCommand, orchestrate các service liên quan.
 * Không chứa business logic — chỉ delegate xuống service.
 *
 * Khi cần gọi thêm service khác (vd: DungeonService, StatService...),
 * inject vào đây thay vì vào command.
 */
public class PartyController {

    private final IPartyService partyService;
    private final PartyInviteTask inviteTask;

    public PartyController(IPartyService partyService, PartyInviteTask inviteTask) {
        this.partyService = partyService;
        this.inviteTask = inviteTask;
    }

    public void createParty(Player player) {
        partyService.createParty(player);
    }

    public void disbandParty(Player player) {
        partyService.disbandParty(player);
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
