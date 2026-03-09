package com.roguesmp.dungeon.service.impl;

import com.roguesmp.dungeon.data.Party;
import com.roguesmp.dungeon.manager.PartyManager;
import com.roguesmp.dungeon.service.IPartyService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Business logic của Party — gọi IPartyManager để thao tác data,
 * xử lý validation và thông báo tới Player.
 */
public class PartyService implements IPartyService {

    private final PartyManager partyManager;

    public PartyService(PartyManager partyManager) {
        this.partyManager = partyManager;
    }

    @Override
    public void createParty(Player owner) {
        if (partyManager.isInParty(owner.getUniqueId())) {
            owner.sendMessage("§cBạn đã có party!");
            return;
        }

        partyManager.createParty(owner.getUniqueId());
        owner.sendMessage("§aBạn đã tạo party!");
    }

    @Override
    public void disbandParty(Player owner) {
        if (!partyManager.isOwner(owner.getUniqueId())) {
            owner.sendMessage("§cBạn không phải chủ party!");
            return;
        }

        Party party = partyManager.findByPlayer(owner.getUniqueId()).orElse(null);
        if (party == null) return;

        // Notify tất cả member trước khi disband
        for (UUID memberId : party.getMembers()) {
            Player member = Bukkit.getPlayer(memberId);
            if (member != null) {
                member.sendMessage("§cParty đã bị giải tán!");
            }
        }

        partyManager.disbandParty(party.getPartyId());
    }

    @Override
    public void joinParty(Player player, Player owner) {
        if (partyManager.isInParty(player.getUniqueId())) {
            player.sendMessage("§cBạn đang ở trong party khác!");
            return;
        }

        if (!partyManager.isOwner(owner.getUniqueId())) {
            player.sendMessage("§cNgười này không phải chủ party!");
            return;
        }

        Party party = partyManager.findByPlayer(owner.getUniqueId()).orElse(null);
        if (party == null) return;

        if (party.getMembers().size() >= party.getSize()) {
            player.sendMessage("§cParty đã đầy!");
            return;
        }

        partyManager.addMember(party.getPartyId(), player.getUniqueId());
        player.sendMessage("§aBạn đã tham gia party!");
    }

    @Override
    public void leaveParty(Player player) {
        if (!partyManager.isInParty(player.getUniqueId())) {
            player.sendMessage("§cBạn không ở trong party nào!");
            return;
        }

        if (partyManager.isOwner(player.getUniqueId())) {
            player.sendMessage("§cChủ party phải dùng lệnh /party disband!");
            return;
        }

        Party party = partyManager.findByPlayer(player.getUniqueId()).orElse(null);
        if (party == null) return;

        partyManager.removeMember(party.getPartyId(), player.getUniqueId());
        player.sendMessage("§aBạn đã rời party!");
    }

    @Override
    public void kickMember(Player owner, Player target) {
        if (!partyManager.isOwner(owner.getUniqueId())) {
            owner.sendMessage("§cBạn không phải chủ party!");
            return;
        }

        if (owner.getUniqueId().equals(target.getUniqueId())) {
            owner.sendMessage("§cBạn không thể tự kick chính mình. Dùng /party disband.");
            return;
        }

        Party party = partyManager.findByPlayer(owner.getUniqueId()).orElse(null);
        if (party == null) return;

        if (!party.getMembers().contains(target.getUniqueId())) {
            owner.sendMessage("§cNgười này không ở trong party của bạn!");
            return;
        }

        partyManager.removeMember(party.getPartyId(), target.getUniqueId());
        owner.sendMessage("§aĐã kick " + target.getName() + " khỏi party.");
        target.sendMessage("§cBạn đã bị kick khỏi party.");
    }

    @Override
    public void transferOwnership(Player currentOwner, Player newOwner) {
        if (!partyManager.isOwner(currentOwner.getUniqueId())) {
            currentOwner.sendMessage("§cBạn không phải chủ party!");
            return;
        }

        Party party = partyManager.findByPlayer(currentOwner.getUniqueId()).orElse(null);
        if (party == null) return;

        if (!party.getMembers().contains(newOwner.getUniqueId())) {
            currentOwner.sendMessage("§cNgười này không ở trong party của bạn!");
            return;
        }

        partyManager.transferOwner(party.getPartyId(), newOwner.getUniqueId());
        currentOwner.sendMessage("§aĐã chuyển quyền chủ party cho " + newOwner.getName() + ".");
        newOwner.sendMessage("§aBạn đã trở thành chủ party!");
    }

    @Override
    public boolean isInParty(Player player) {
        return partyManager.isInParty(player.getUniqueId());
    }

    @Override
    public boolean isOwner(Player player) {
        return partyManager.isOwner(player.getUniqueId());
    }

    @Override
    public String buildPartyInfo(Player player) {
        Party party = partyManager.findByPlayer(player.getUniqueId()).orElse(null);

        if (party == null) {
            return "§cBạn không ở trong party nào.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("§a=== Thông tin Party ===\n");

        Player leader = Bukkit.getPlayer(party.getOwner());
        sb.append("§fLeader: ")
                .append(leader != null ? leader.getName() : "§7(Offline)")
                .append("\n");

        sb.append("§fThành viên (")
                .append(party.getMembers().size())
                .append("/")
                .append(party.getSize())
                .append("):\n");

        for (UUID memberId : party.getMembers()) {
            Player member = Bukkit.getPlayer(memberId);
            sb.append(" §7- §f")
                    .append(member != null ? member.getName() : "§7(Offline)")
                    .append("\n");
        }

        return sb.toString();
    }
}