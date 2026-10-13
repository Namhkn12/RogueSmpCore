package com.roguesmp.dungeon.service.impl;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.Component;
import com.roguesmp.dungeon.data.runtime.Party;
import com.roguesmp.dungeon.manager.PartyManager;
import com.roguesmp.dungeon.service.IPartyService;
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
            owner.sendMessage(Component.text("Không thể tạo nhóm khi đang trong 1 nhóm khác", NamedTextColor.YELLOW));
            return null;
        }
        owner.sendMessage(Component.text("Tạo nhóm thành công", NamedTextColor.GREEN));
        return partyManager.createParty(owner.getUniqueId());
    }

    @Override
    public void disbandParty(Player owner) {
        if (!partyManager.checkIsOwner(owner.getUniqueId())) {
            owner.sendMessage(Component.text("Bạn không phải chủ nhóm", NamedTextColor.RED));
            return;
        }

        Party party = partyManager.findByPlayer(owner.getUniqueId());
        if (party == null) return;

        for (String memberId : party.getMembers()) {
            Player member = UuidUtil.getPlayerById(memberId);
            if (member != null) {
                member.sendMessage(Component.text("Nhóm đã bị giải tán", NamedTextColor.YELLOW));
            }
        }

        partyManager.disbandParty(party.getPartyId());
    }

    @Override
    public void joinParty(Player player, Player owner) {
        if (partyManager.isInParty(player.getUniqueId())) {
            player.sendMessage(Component.text("Bạn đang ở trong nhóm khác", NamedTextColor.YELLOW));
            return;
        }

        if (!partyManager.checkIsOwner(owner.getUniqueId())) {
            player.sendMessage(Component.text("Người này không phải chủ nhóm", NamedTextColor.YELLOW));
            return;
        }

        Party party = partyManager.findByPlayer(owner.getUniqueId());
        if (party == null) return;

        if (party.getMembers().size() >= party.getMaxSize()) {
            player.sendMessage(Component.text("Nhóm đã đầy", NamedTextColor.YELLOW));
            return;
        }

        partyManager.addMember(party.getPartyId(), player.getUniqueId().toString());
        player.sendMessage(Component.text("Bạn đã tham gia nhóm", NamedTextColor.GREEN));
    }

    @Override
    public void leaveParty(Player player) {
        if (!partyManager.isInParty(player.getUniqueId())) {
            player.sendMessage(Component.text("Bạn không ở trong nhóm nào", NamedTextColor.YELLOW));
            return;
        }

        if (partyManager.checkIsOwner(player.getUniqueId())) {
            player.sendMessage(Component.text("Hãy dùng lệnh /disband để giải tán", NamedTextColor.YELLOW));
            return;
        }

        Party party = partyManager.findByPlayer(player.getUniqueId());
        if (party == null) return;

        partyManager.removeMember(party.getPartyId(), player.getUniqueId().toString());
        player.sendMessage(Component.text("Bạn đã rời nhóm", NamedTextColor.GREEN));
    }

    @Override
    public void kickMember(Player owner, Player target) {
        if (!partyManager.checkIsOwner(owner.getUniqueId())) {
            owner.sendMessage(Component.text("Bạn không phải chủ nhóm", NamedTextColor.RED));
            return;
        }

        if (owner.getUniqueId().equals(target.getUniqueId())) {
            owner.sendMessage(Component.text("Bạn không thể tự đuổi chính mình", NamedTextColor.RED));
            return;
        }

        Party party = partyManager.findByPlayer(owner.getUniqueId());
        if (party == null) return;

        if (!party.getMembers().contains(target.getUniqueId().toString())) {
            owner.sendMessage(Component.text("Người này không thuộc nhóm của bạn", NamedTextColor.RED));
            return;
        }

        partyManager.removeMember(party.getPartyId(), target.getUniqueId().toString());
        owner.sendMessage(Component.text("Đã đuổi " + target.getName() + " khỏi party.", NamedTextColor.GREEN));
        target.sendMessage(Component.text("Bạn đã bị buộc rời nhóm", NamedTextColor.YELLOW));
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
                newOwner.sendMessage(Component.text("Bạn đã trở thành chủ nhóm", NamedTextColor.GREEN));
            }
        }
        partyManager.removeMember(party.getPartyId(), playerId);
        player.sendMessage(Component.text("Bạn đã rời khỏi party (force)", NamedTextColor.YELLOW));
    }

    @Override
    public void transferOwnership(Player currentOwner, Player newOwner) {
        if (!partyManager.checkIsOwner(currentOwner.getUniqueId())) {
            currentOwner.sendMessage(Component.text("Bạn không phải chủ nhóm", NamedTextColor.RED));
            return;
        }

        Party party = partyManager.findByPlayer(currentOwner.getUniqueId());
        if (party == null) return;

        if (!party.getMembers().contains(newOwner.getUniqueId().toString())) {
            currentOwner.sendMessage(Component.text("Người này không ở trong nhóm của bạn", NamedTextColor.RED));
            return;
        }
        partyManager.transferOwner(party.getPartyId(), newOwner.getUniqueId().toString());

        currentOwner.sendMessage(Component.text("Đã chuyển quyền chủ nhóm cho " + newOwner.getName() + ".", NamedTextColor.GREEN));
        newOwner.sendMessage(Component.text("Bạn đã trở thành chủ nhóm", NamedTextColor.GREEN));
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
