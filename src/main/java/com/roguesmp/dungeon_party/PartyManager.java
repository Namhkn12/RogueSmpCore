package com.roguesmp.dungeon_party;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

public class PartyManager {

    private static final int DEFAULT_PARTY_SIZE = 4;

    private static PartyManager INSTANCE;

    private final PartyRepository repository;
    private final PartyInviteManager inviteManager;
    private final Map<String, Party> partyMap = new HashMap<>();
    private final Map<String, String> playerIndex = new HashMap<>();

    private PartyManager(RogueSmpCore plugin) {
        this.repository = new PartyRepository(plugin);
        this.inviteManager = new PartyInviteManager(plugin, this);
        loadAll();
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

    private void loadAll() {
        Collection<Party> parties = repository.loadAll();
        for (Party party : parties) {
            partyMap.put(party.getPartyId(), party);
            for (String memberId : party.getMembers()) {
                playerIndex.put(memberId, party.getPartyId());
            }
        }
        RogueSmpCore.LOGGER.info("Loaded {} parties", parties.size());
    }

    public void saveAll() {
        partyMap.values().forEach(repository::save);
        RogueSmpCore.LOGGER.info("Saved {} parties", partyMap.size());
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

        partyMap.remove(party.getPartyId());
        party.getMembers().forEach(playerIndex::remove);
        repository.delete(party.getPartyId());
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
        if (isOwner(player)) {
            player.sendMessage(Utils.fromString("<yellow>Hãy dùng lệnh /party disband để giải tán"));
            return;
        }
        Party party = getPartyByPlayer(player);
        if (party == null) return;

        removeMember(party, player.getUniqueId().toString());
        player.sendMessage(Utils.fromString("<green>Bạn đã rời nhóm"));
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

        String playerId = player.getUniqueId().toString();
        if (party.isOwner(playerId)) {
            String newOwnerId = party.getMembers().stream()
                    .filter(id -> !id.equals(playerId))
                    .findFirst()
                    .orElse(null);

            if (newOwnerId == null) {
                disbandParty(player);
                return;
            }

            party.setOwner(newOwnerId);
            Player newOwner = getPlayer(newOwnerId);
            if (newOwner != null) {
                newOwner.sendMessage(Utils.fromString("<green>Bạn đã trở thành chủ nhóm"));
            }
        }
        removeMember(party, playerId);
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
