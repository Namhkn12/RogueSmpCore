package com.roguesmp.dungeon.controller;

import com.roguesmp.dungeon.controller.response.ControllerResponse;
import com.roguesmp.dungeon.data.Party;
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

    public ControllerResponse<Party> getPartyByPlayer(Player player){
        Party party = partyService.getPartyByPlayer(player).orElse(null);
        return ControllerResponse.success("Lấy party từ người chơi thành công", party);
    }

    public ControllerResponse<Void> createParty(Player player) {
        Party party = partyService.createParty(player);
        if(party == null){
            return ControllerResponse.failure("Không thể tạo party");
        }
        return ControllerResponse.success("Đã tạo party thành công");
    }

    public ControllerResponse<Void> disbandParty(Player player) {
        partyService.disbandParty(player);
        return ControllerResponse.success("Đã giải tán party");
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
