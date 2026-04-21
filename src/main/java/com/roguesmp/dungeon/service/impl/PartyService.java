package com.roguesmp.dungeon.service.impl;

import com.roguesmp.dungeon.data.runtime.Party;
import com.roguesmp.dungeon.manager.PartyManager;
import com.roguesmp.dungeon.service.IPartyService;
import com.roguesmp.dungeon.utils.DungeonEcho;
import com.roguesmp.dungeon.utils.MCStringBuilder;
import com.roguesmp.dungeon.utils.UuidUtil;
import org.bukkit.entity.Player;

import java.util.List;

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
            Player member = UuidUtil.getPlayerById(memberId);
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

        if (!party.getMembers().contains(target.getUniqueId().toString())) {
            DungeonEcho.error(owner, "Người này không thuộc nhóm của bạn");
            return;
        }

        partyManager.removeMember(party.getPartyId(), target.getUniqueId().toString());
        DungeonEcho.success(owner, "Đã đuổi " + target.getName() + " khỏi party.");
        DungeonEcho.warn(target, "Bạn đã bị buộc rời nhóm");
    }

    @Override
    public void forceKick(Player player) {
        if (!partyManager.isInParty(player.getUniqueId())) {
            return;
        }

        Party party = partyManager.findByPlayer(player.getUniqueId());
        if (party == null) return;

        String playerId = player.getUniqueId().toString();

        if (partyManager.checkIsOwner(player.getUniqueId())) {

            String newOwnerId = party.getMembers().stream()
                    .filter(id -> !id.equals(playerId))
                    .findFirst()
                    .orElse(null);

            if (newOwnerId == null) {
                disbandParty(player);
                return;
            }

            partyManager.transferOwner(party.getPartyId(), newOwnerId);

            Player newOwner = UuidUtil.getPlayerById(newOwnerId);
            if (newOwner != null) {
                DungeonEcho.success(newOwner, "Bạn đã trở thành chủ nhóm");
            }
        }
        partyManager.removeMember(party.getPartyId(), playerId);
        DungeonEcho.warn(player, "Bạn đã rời khỏi party (force)");
    }

    @Override
    public void transferOwnership(Player currentOwner, Player newOwner) {
        if (!partyManager.checkIsOwner(currentOwner.getUniqueId())) {
            DungeonEcho.error(currentOwner,"Bạn không phải chủ nhóm");
            return;
        }

        Party party = partyManager.findByPlayer(currentOwner.getUniqueId());
        if (party == null) return;

        if (!party.getMembers().contains(newOwner.getUniqueId().toString())) {
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

        Player leader = UuidUtil.getPlayerById(party.getOwner());
        String leaderName = leader != null ? leader.getName() : MCStringBuilder.gray("(Offline)");

        MCStringBuilder sb = MCStringBuilder.of()
                .appendGreen("=== Thông tin Party ===").newLine()
                .appendWhite("Leader: ").appendWhite(leaderName).newLine()
                .appendWhite("Thành viên (")
                .appendGold(party.getMembers().size() + "/" + party.getMaxSize())
                .appendWhite("):").newLine();

        for (String memberId : party.getMembers()) {
            Player member = UuidUtil.getPlayerById(memberId);
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
                .map(UuidUtil::getPlayerById)
                .filter(p -> p != null && p.isOnline())
                .toList();
    }
}
