package com.roguesmp.dungeon.service.impl;

import com.roguesmp.dungeon.data.Party;
import com.roguesmp.dungeon.manager.PartyManager;
import com.roguesmp.dungeon.service.IPartyService;
import com.roguesmp.dungeon.ultis.MCStringBuilder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Optional;
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
    public Party createParty(Player owner) {
        if (partyManager.isInParty(owner.getUniqueId())) {
            owner.sendMessage(MCStringBuilder.of().red("Tạo party không thành công").build());
            return null;
        }
        owner.sendMessage(MCStringBuilder.of().green("Tạo thành công").build());
        return partyManager.createParty(owner.getUniqueId());
    }

    @Override
    public void disbandParty(Player owner) {
        if (!partyManager.isOwner(owner.getUniqueId())) {
            owner.sendMessage(MCStringBuilder.of().red("Bạn không phải chủ party").build());
            return;
        }

        Party party = partyManager.findByPlayer(owner.getUniqueId()).orElse(null);
        if (party == null) return;

        // Notify tất cả member trước khi disband
        for (UUID memberId : party.getMembers()) {
            Player member = Bukkit.getPlayer(memberId);
            if (member != null) {
                member.sendMessage(MCStringBuilder.of().yellow("Party đã bị giải tán").build());
            }
        }

        partyManager.disbandParty(party.getPartyId());
    }

    @Override
    public void joinParty(Player player, Player owner) {
        if (partyManager.isInParty(player.getUniqueId())) {
            player.sendMessage(MCStringBuilder.of().yellow("Bạn đang ở trong party khác").build());
            return;
        }

        if (!partyManager.isOwner(owner.getUniqueId())) {
            player.sendMessage(MCStringBuilder.of().yellow("Người này không phải chủ party").build());
            return;
        }

        Party party = partyManager.findByPlayer(owner.getUniqueId()).orElse(null);
        if (party == null) return;

        if (party.getMembers().size() >= party.getSize()) {
            player.sendMessage(MCStringBuilder.of().yellow("Party đã đầy").build());
            return;
        }

        partyManager.addMember(party.getPartyId(), player.getUniqueId());
        player.sendMessage(MCStringBuilder.of().green("Bạn đã tham gia party").build());
    }

    @Override
    public void leaveParty(Player player) {
        if (!partyManager.isInParty(player.getUniqueId())) {
            player.sendMessage(MCStringBuilder.of().yellow("Bạn không ở trong party nào").build());
            return;
        }

        if (partyManager.isOwner(player.getUniqueId())) {
            player.sendMessage(MCStringBuilder.of().yellow("Hãy dùng lệnh disband để giải tán").build());
            return;
        }

        Party party = partyManager.findByPlayer(player.getUniqueId()).orElse(null);
        if (party == null) return;

        partyManager.removeMember(party.getPartyId(), player.getUniqueId());
        player.sendMessage(MCStringBuilder.of().green("Bạn đã rời party").build());
    }

    @Override
    public void kickMember(Player owner, Player target) {
        if (!partyManager.isOwner(owner.getUniqueId())) {
            owner.sendMessage(MCStringBuilder.of().yellow("Bạn không phải chủ party").build());
            return;
        }

        if (owner.getUniqueId().equals(target.getUniqueId())) {
            owner.sendMessage(MCStringBuilder.of().yellow("Bạn không thể tự kick chính mình").build());
            return;
        }

        Party party = partyManager.findByPlayer(owner.getUniqueId()).orElse(null);
        if (party == null) return;

        if (!party.getMembers().contains(target.getUniqueId())) {
            owner.sendMessage(MCStringBuilder.of().yellow("Người này không thuộc party của bạn").build());
            return;
        }

        partyManager.removeMember(party.getPartyId(), target.getUniqueId());
        owner.sendMessage(MCStringBuilder.of().yellow("§aĐã kick " + target.getName() + " khỏi party.").build());
        target.sendMessage(MCStringBuilder.of().yellow("Bạn đã bị buộc rời party").build());
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

    @Override
    public Optional<Party> getPartyByPlayer(Player player){
        return partyManager.findByPlayer(player.getUniqueId());
    }

    @Override
    public Optional<Party> getPartyById(UUID partyId) {
        return partyManager.findById(partyId);
    }
}