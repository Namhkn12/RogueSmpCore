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
            owner.sendMessage(MCStringBuilder.red("Tạo party không thành công"));
            return null;
        }
        owner.sendMessage(MCStringBuilder.green("Tạo thành công"));
        return partyManager.createParty(owner.getUniqueId());
    }

    @Override
    public void disbandParty(Player owner) {
        if (!partyManager.isOwner(owner.getUniqueId())) {
            owner.sendMessage(MCStringBuilder.red("Bạn không phải chủ party"));
            return;
        }

        Party party = partyManager.findByPlayer(owner.getUniqueId()).orElse(null);
        if (party == null) return;

        // Notify tất cả member trước khi disband
        for (UUID memberId : party.getMembers()) {
            Player member = Bukkit.getPlayer(memberId);
            if (member != null) {
                member.sendMessage(MCStringBuilder.yellow("Party đã bị giải tán"));
            }
        }

        partyManager.disbandParty(party.getPartyId());
    }

    @Override
    public void joinParty(Player player, Player owner) {
        if (partyManager.isInParty(player.getUniqueId())) {
            player.sendMessage(MCStringBuilder.yellow("Bạn đang ở trong party khác"));
            return;
        }

        if (!partyManager.isOwner(owner.getUniqueId())) {
            player.sendMessage(MCStringBuilder.yellow("Người này không phải chủ party"));
            return;
        }

        Party party = partyManager.findByPlayer(owner.getUniqueId()).orElse(null);
        if (party == null) return;

        if (party.getMembers().size() >= party.getSize()) {
            player.sendMessage(MCStringBuilder.yellow("Party đã đầy"));
            return;
        }

        partyManager.addMember(party.getPartyId(), player.getUniqueId());
        player.sendMessage(MCStringBuilder.green("Bạn đã tham gia party"));
    }

    @Override
    public void leaveParty(Player player) {
        if (!partyManager.isInParty(player.getUniqueId())) {
            player.sendMessage(MCStringBuilder.yellow("Bạn không ở trong party nào"));
            return;
        }

        if (partyManager.isOwner(player.getUniqueId())) {
            player.sendMessage(MCStringBuilder.yellow("Hãy dùng lệnh disband để giải tán"));
            return;
        }

        Party party = partyManager.findByPlayer(player.getUniqueId()).orElse(null);
        if (party == null) return;

        partyManager.removeMember(party.getPartyId(), player.getUniqueId());
        player.sendMessage(MCStringBuilder.green("Bạn đã rời party"));
    }

    @Override
    public void kickMember(Player owner, Player target) {
        if (!partyManager.isOwner(owner.getUniqueId())) {
            owner.sendMessage(MCStringBuilder.yellow("Bạn không phải chủ party"));
            return;
        }

        if (owner.getUniqueId().equals(target.getUniqueId())) {
            owner.sendMessage(MCStringBuilder.yellow("Bạn không thể tự kick chính mình"));
            return;
        }

        Party party = partyManager.findByPlayer(owner.getUniqueId()).orElse(null);
        if (party == null) return;

        if (!party.getMembers().contains(target.getUniqueId())) {
            owner.sendMessage(MCStringBuilder.yellow("Người này không thuộc party của bạn"));
            return;
        }

        partyManager.removeMember(party.getPartyId(), target.getUniqueId());
        owner.sendMessage(MCStringBuilder.yellow("§aĐã kick " + target.getName() + " khỏi party."));
        target.sendMessage(MCStringBuilder.yellow("Bạn đã bị buộc rời party"));
    }

    @Override
    public void transferOwnership(Player currentOwner, Player newOwner) {
        if (!partyManager.isOwner(currentOwner.getUniqueId())) {
            currentOwner.sendMessage(MCStringBuilder.yellow("Bạn không phải chủ party"));
            return;
        }

        Party party = partyManager.findByPlayer(currentOwner.getUniqueId()).orElse(null);
        if (party == null) return;

        if (!party.getMembers().contains(newOwner.getUniqueId())) {
            currentOwner.sendMessage(MCStringBuilder.yellow("Người này không ở trong party của bạn"));
            return;
        }

        partyManager.transferOwner(party.getPartyId(), newOwner.getUniqueId());
        currentOwner.sendMessage(MCStringBuilder.yellow("Đã chuyển quyền chủ party cho " + newOwner.getName() + "."));
        newOwner.sendMessage(MCStringBuilder.yellow("Bạn đã trở thành chủ party"));
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
            return MCStringBuilder.yellow("Bạn không ở trong party nào.");
        }

        Player leader = Bukkit.getPlayer(party.getOwner());
        String leaderName = leader != null ? leader.getName() : MCStringBuilder.gray("(Offline)");

        MCStringBuilder sb = MCStringBuilder.of()
                .appendGreen("=== Thông tin Party ===").newLine()
                .appendWhite("Leader: ").appendWhite(leaderName).newLine()
                .appendWhite("Thành viên (")
                .appendGold(party.getMembers().size() + "/" + party.getSize())
                .appendWhite("):").newLine();

        for (UUID memberId : party.getMembers()) {
            Player member = Bukkit.getPlayer(memberId);
            String memberName = member != null ? member.getName() : MCStringBuilder.gray("(Offline)");
            sb.appendGray(" - ").appendWhite(memberName).newLine();
        }

        return sb.build();
    }

    @Override
    public Optional<Party> getPartyByPlayer(Player player){
        return partyManager.findByPlayer(player.getUniqueId());
    }

    @Override
    public Optional<Party> getPartyById(UUID partyId) {
        return partyManager.findById(partyId);
    }

    @Override
    public void savePartyToFile() {
        partyManager.saveAll();
    }
}