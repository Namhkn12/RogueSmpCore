package com.roguesmp.dungeon_v2.service.impl;

import com.roguesmp.dungeon_v2.data.runtime.Party;
import com.roguesmp.dungeon_v2.manager.PartyManager;
import com.roguesmp.dungeon_v2.service.IPartyService;
import com.roguesmp.dungeon_v2.utils.DungeonEcho;
import com.roguesmp.dungeon_v2.utils.MCStringBuilder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public class PartyService implements IPartyService {

    private final PartyManager partyManager;

    public PartyService(PartyManager partyManager) {
        this.partyManager = partyManager;
    }

    @Override
    public Party createParty(Player owner) {
        if (partyManager.isInParty(owner.getUniqueId())) {
            DungeonEcho.warn(owner, "Không thể tạo nhóm khi đang trong 1 nhóm khác");
            return null;
        }
        DungeonEcho.success(owner, "Tạo nhóm thành công");
        return partyManager.createParty(owner.getUniqueId());
    }

    @Override
    public void disbandParty(Player owner) {
        if (!partyManager.checkIsOwner(owner.getUniqueId())) {
            DungeonEcho.error(owner, "Bạn không phải chủ nhóm");
            return;
        }

        Party party = partyManager.findByPlayer(owner.getUniqueId());
        if (party == null) return;

        for (String memberId : party.getMembers()) {
            Player member = Bukkit.getPlayer(memberId);
            if (member != null) {
                DungeonEcho.warn(member, "Nhóm đã bị giải tán");
            }
        }

        partyManager.disbandParty(party.getPartyId());
    }

    @Override
    public void joinParty(Player player, Player owner) {
        if (partyManager.isInParty(player.getUniqueId())) {
            DungeonEcho.warn(player, "Bạn đang ở trong nhóm khác");
            return;
        }

        if (!partyManager.checkIsOwner(owner.getUniqueId())) {
            DungeonEcho.warn(player, "Người này không phải chủ nhóm");
            return;
        }

        Party party = partyManager.findByPlayer(owner.getUniqueId());
        if (party == null) return;

        if (party.getMembers().size() >= party.getMaxSize()) {
            DungeonEcho.warn(player, "Nhóm đã đầy");
            return;
        }

        partyManager.addMember(party.getPartyId(), player.getUniqueId().toString());
        DungeonEcho.success(player, "Bạn đã tham gia nhóm");
    }

    @Override
    public void leaveParty(Player player) {
        if (!partyManager.isInParty(player.getUniqueId())) {
            DungeonEcho.warn(player, "Bạn không ở trong nhóm nào");
            return;
        }

        if (partyManager.checkIsOwner(player.getUniqueId())) {
            DungeonEcho.warn(player, "Hãy dùng lệnh /disband để giải tán");
            return;
        }

        Party party = partyManager.findByPlayer(player.getUniqueId());
        if (party == null) return;

        partyManager.removeMember(party.getPartyId(), player.getUniqueId().toString());
        DungeonEcho.success(player, "Bạn đã rời nhóm");
    }

    @Override
    public void kickMember(Player owner, Player target) {
        if (!partyManager.checkIsOwner(owner.getUniqueId())) {
            DungeonEcho.error(owner, "Bạn không phải chủ nhóm");
            return;
        }

        if (owner.getUniqueId().equals(target.getUniqueId())) {
            DungeonEcho.error(owner, "Bạn không thể tự đuổi chính mình");
            return;
        }

        Party party = partyManager.findByPlayer(owner.getUniqueId());
        if (party == null) return;

        if (!party.getMembers().contains(target.getUniqueId())) {
            DungeonEcho.error(owner, "Người này không thuộc nhóm của bạn");
            return;
        }

        partyManager.removeMember(party.getPartyId(), target.getUniqueId().toString());
        DungeonEcho.success(owner, "Đã đuổi " + target.getName() + " khỏi party.");
        DungeonEcho.warn(owner, "Bạn đã bị buộc rời nhóm");
    }

    @Override
    public void transferOwnership(Player currentOwner, Player newOwner) {
        if (!partyManager.checkIsOwner(currentOwner.getUniqueId())) {
            DungeonEcho.error(currentOwner,"Bạn không phải chủ nhóm");
            return;
        }

        Party party = partyManager.findByPlayer(currentOwner.getUniqueId());
        if (party == null) return;

        if (!party.getMembers().contains(newOwner.getUniqueId())) {
            DungeonEcho.error(currentOwner,"Người này không ở trong nhóm của bạn");
            return;
        }
        partyManager.transferOwner(party.getPartyId(), newOwner.getUniqueId().toString());

        DungeonEcho.success(currentOwner,"Đã chuyển quyền chủ nhóm cho " + newOwner.getName() + ".");
        DungeonEcho.success(newOwner, "Bạn đã trở thành chủ nhóm");
    }

    @Override
    public boolean isInParty(Player player) {
        return partyManager.isInParty(player.getUniqueId());
    }

    @Override
    public boolean isOwner(Player player) {
        return partyManager.checkIsOwner(player.getUniqueId());
    }

    @Override
    public String buildPartyInfo(Player player) {
        Party party = partyManager.findByPlayer(player.getUniqueId());

        if (party == null) {
            return MCStringBuilder.yellow("Bạn không ở trong nhóm nào.");
        }

        Player leader = Bukkit.getPlayer(UUID.fromString(party.getOwner()));
        String leaderName = leader != null ? leader.getName() : MCStringBuilder.gray("(Offline)");

        MCStringBuilder sb = MCStringBuilder.of()
                .appendGreen("=== Thông tin Party ===").newLine()
                .appendWhite("Leader: ").appendWhite(leaderName).newLine()
                .appendWhite("Thành viên (")
                .appendGold(party.getMembers().size() + "/" + party.getMaxSize())
                .appendWhite("):").newLine();

        for (String memberId : party.getMembers()) {
            Player member = Bukkit.getPlayer(UUID.fromString(memberId));
            String memberName = member != null ? member.getName() : MCStringBuilder.gray("(Offline)");
            sb.appendGray(" - ").appendWhite(memberName).newLine();
        }

        return sb.build();
    }

    @Override
    public Party getPartyByPlayer(Player player){
        return partyManager.findByPlayer(player.getUniqueId());
    }

    @Override
    public Party getPartyById(String partyId) {
        return partyManager.findById(partyId);
    }

    @Override
    public void savePartyToFile() {
        partyManager.saveAll();
    }

    @Override
    public List<Player> getOnlineMembers(Party party) {
        return party.getMembers().stream()
                .map(UUID::fromString)
                .map(Bukkit::getPlayer)
                .filter(p -> p != null && p.isOnline())
                .toList();
    }
}