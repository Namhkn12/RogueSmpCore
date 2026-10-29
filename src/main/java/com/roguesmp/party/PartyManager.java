package com.roguesmp.party;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

public class PartyManager {

    private static final int DEFAULT_PARTY_SIZE = 4;

    private static PartyManager INSTANCE;

    private final PartyInviteManager inviteManager;
    private final Map<String, Party> partyMap = new HashMap<>();
    private final Map<String, String> playerIndex = new HashMap<>();

    private PartyManager(RogueSmpCore plugin) {
        this.inviteManager = new PartyInviteManager(plugin, this);
    }

    public static void init(RogueSmpCore plugin) {
        INSTANCE = new PartyManager(plugin);
    }

    public static PartyManager getInstance() {
        return INSTANCE;
    }

    public PartyInviteManager getInviteManager() {
        return inviteManager;
    }

    public Party createParty(Player owner) {
        if (isInParty(owner)) {
            owner.sendMessage(Utils.fromString("<yellow>Không thể tạo nhóm khi đang trong 1 nhóm khác"));
            return null;
        }
        String partyId = UUID.randomUUID().toString();
        String ownerId = owner.getUniqueId().toString();
        Party party = new Party(partyId, ownerId, DEFAULT_PARTY_SIZE);
        partyMap.put(partyId, party);
        playerIndex.put(ownerId, partyId);
        owner.sendMessage(Utils.fromString("<green>Tạo nhóm thành công"));
        return party;
    }

    public void disbandParty(Player owner) {
        if (!isOwner(owner)) {
            owner.sendMessage(Utils.fromString("<red>Bạn không phải chủ nhóm"));
            return;
        }
        Party party = getPartyByPlayer(owner);
        if (party == null) return;

        getOnlineMembers(party).forEach(member -> member.sendMessage(Utils.fromString("<yellow>Nhóm đã bị giải tán")));

        remove(party);
    }

    public void handleQuit(Player player) {
        Party party = getPartyByPlayer(player);
        if (party != null && getOnlineMembers(party).stream().allMatch(player::equals)) remove(party);
    }

    private void remove(Party party) {
        partyMap.remove(party.getPartyId());
        party.getMembers().forEach(playerIndex::remove);
    }

    public void joinParty(Player player, Player owner) {
        if (isInParty(player)) {
            player.sendMessage(Utils.fromString("<yellow>Bạn đang ở trong nhóm khác"));
            return;
        }
        if (!isOwner(owner)) {
            player.sendMessage(Utils.fromString("<yellow>Người này không phải chủ nhóm"));
            return;
        }
        Party party = getPartyByPlayer(owner);
        if (party == null) return;

        if (party.isFull()) {
            player.sendMessage(Utils.fromString("<yellow>Nhóm đã đầy"));
            return;
        }

        addMember(party, player.getUniqueId().toString());
        player.sendMessage(Utils.fromString("<green>Bạn đã tham gia nhóm"));
    }

    public void leaveParty(Player player) {
        if (!isInParty(player)) {
            player.sendMessage(Utils.fromString("<yellow>Bạn không ở trong nhóm nào"));
            return;
        }
        Party party = getPartyByPlayer(player);
        if (party == null) return;

        if (!removeAndPromote(party, player)) {
            player.sendMessage(Utils.fromString("<yellow>Nhóm đã bị giải tán"));
            return;
        }
        player.sendMessage(Utils.fromString("<green>Bạn đã rời nhóm"));
    }

    private boolean removeAndPromote(Party party, Player player) {
        String playerId = player.getUniqueId().toString();
        if (party.isOwner(playerId)) {
            String newOwnerId = party.getMembers().stream()
                    .filter(id -> !id.equals(playerId))
                    .min(Comparator.comparing(id -> getPlayer(id) == null))
                    .orElse(null);

            if (newOwnerId == null) {
                remove(party);
                return false;
            }

            party.setOwner(newOwnerId);
            String message = "<yellow>" + player.getName() + " đã rời nhóm, " + displayName(newOwnerId) + " trở thành chủ nhóm";
            getOnlineMembers(party).stream()
                    .filter(member -> !member.equals(player))
                    .forEach(member -> member.sendMessage(Utils.fromString(message)));
        }
        removeMember(party, playerId);
        return true;
    }

    public void kickMember(Player owner, Player target) {
        if (!isOwner(owner)) {
            owner.sendMessage(Utils.fromString("<red>Bạn không phải chủ nhóm"));
            return;
        }
        if (owner.getUniqueId().equals(target.getUniqueId())) {
            owner.sendMessage(Utils.fromString("<red>Bạn không thể tự đuổi chính mình"));
            return;
        }
        Party party = getPartyByPlayer(owner);
        if (party == null) return;

        if (!party.isMember(target.getUniqueId().toString())) {
            owner.sendMessage(Utils.fromString("<red>Người này không thuộc nhóm của bạn"));
            return;
        }

        removeMember(party, target.getUniqueId().toString());
        owner.sendMessage(Utils.fromString("<green>Đã đuổi " + target.getName() + " khỏi party."));
        target.sendMessage(Utils.fromString("<yellow>Bạn đã bị buộc rời nhóm"));
    }

    public void forceKick(Player player) {
        Party party = getPartyByPlayer(player);
        if (party == null) return;

        removeAndPromote(party, player);
        player.sendMessage(Utils.fromString("<yellow>Bạn đã rời khỏi party (force)"));
    }

    public void transferOwnership(Player currentOwner, Player newOwner) {
        if (!isOwner(currentOwner)) {
            currentOwner.sendMessage(Utils.fromString("<red>Bạn không phải chủ nhóm"));
            return;
        }
        Party party = getPartyByPlayer(currentOwner);
        if (party == null) return;

        if (!party.isMember(newOwner.getUniqueId().toString())) {
            currentOwner.sendMessage(Utils.fromString("<red>Người này không ở trong nhóm của bạn"));
            return;
        }
        party.setOwner(newOwner.getUniqueId().toString());

        currentOwner.sendMessage(Utils.fromString("<green>Đã chuyển quyền chủ nhóm cho " + newOwner.getName() + "."));
        newOwner.sendMessage(Utils.fromString("<green>Bạn đã trở thành chủ nhóm"));
    }

    public Component buildPartyInfo(Player player) {
        Party party = getPartyByPlayer(player);
        if (party == null) {
            return Utils.fromString("<yellow>Bạn không ở trong nhóm nào.");
        }

        StringBuilder sb = new StringBuilder()
                .append("<green>=== Thông tin Party ===</green>\n")
                .append("<white>Leader: ").append(displayName(party.getOwner())).append("</white>\n")
                .append("<white>Thành viên (<gold>").append(party.currentSize()).append("/").append(party.getMaxSize()).append("</gold>):</white>");
        for (String memberId : party.getMembers()) {
            sb.append("\n<gray> - </gray><white>").append(displayName(memberId)).append("</white>");
        }
        return Utils.fromString(sb.toString());
    }

    public boolean isInParty(Player player) {
        return playerIndex.containsKey(player.getUniqueId().toString());
    }

    public boolean isOwner(Player player) {
        Party party = getPartyByPlayer(player);
        return party != null && party.isOwner(player.getUniqueId().toString());
    }

    public Party getPartyByPlayer(Player player) {
        String partyId = playerIndex.get(player.getUniqueId().toString());
        return partyId == null ? null : partyMap.get(partyId);
    }

    public Party getPartyById(String partyId) {
        return partyMap.get(partyId);
    }

    public List<Player> getOnlineMembers(Party party) {
        return party.getMembers().stream()
                .map(PartyManager::getPlayer)
                .filter(p -> p != null && p.isOnline())
                .toList();
    }

    private void addMember(Party party, String playerId) {
        party.addMember(playerId);
        playerIndex.put(playerId, party.getPartyId());
    }

    private void removeMember(Party party, String playerId) {
        party.removeMember(playerId);
        playerIndex.remove(playerId);
    }

    private static String displayName(String playerId) {
        Player player = getPlayer(playerId);
        return player != null ? player.getName() : "<gray>(Offline)</gray>";
    }

    private static Player getPlayer(String playerId) {
        try {
            return Bukkit.getPlayer(UUID.fromString(playerId));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
