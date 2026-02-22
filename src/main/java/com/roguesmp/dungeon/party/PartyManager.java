package com.roguesmp.dungeon.party;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

public class PartyManager {
    private static PartyManager INSTANCE = null;
    private final PartyInviteManager inviteManager;
    private final Map<UUID, Party> partyMap = new HashMap<>();
    private final Map<UUID, UUID> playerToPartyMap = new HashMap<>();

    public static void init() {
        if (INSTANCE != null) {
            throw new IllegalStateException("PartyManager already initialized!");
        }
        INSTANCE = new PartyManager();
    }

    public static PartyManager getInstance(){
        if (INSTANCE == null) {
            throw new RuntimeException(PartyManager.class.getSimpleName() + "is null when getInstance() is called.");
        }
        return INSTANCE;
    }

    public PartyInviteManager getInviteManager() {
        return inviteManager;
    }

    public void registerCommands() {
        new PartyCommand(this, inviteManager).register();
    }

    private PartyManager() {
        this.inviteManager = new PartyInviteManager(this);
    }

    /*
    * Create a party
    * */
    public void createParty(Player player) {
        if (isInParty(player)) {
            player.sendMessage("Bạn đã có party!");
            return;
        }

        UUID partyId = UUID.randomUUID();
        List<UUID> members = new ArrayList<>();
        members.add(player.getUniqueId());

        Party party = new Party(partyId, player.getUniqueId(), members, 3, true);

        partyMap.put(partyId, party);
        playerToPartyMap.put(player.getUniqueId(), partyId);

        player.sendMessage("Bạn đã tạo party!");
    }

    /*
     * DisBand a party
     * */
    public void disbandParty(Player player) {
        if (!isOwner(player)) {
            player.sendMessage("Bạn không phải chủ party!");
            return;
        }

        UUID partyId = playerToPartyMap.get(player.getUniqueId());
        Party party = partyMap.get(partyId);

        if (party == null) return;

        for (UUID member : party.getMembers()) {
            playerToPartyMap.remove(member);

            Player online = Bukkit.getPlayer(member);
            if (online != null) {
                online.sendMessage("Party đã bị giải tán!");
            }
        }

        partyMap.remove(partyId);
    }

    /*
     * Join a party
     * */
    public void joinParty(Player player, Player owner) {
        if (isInParty(player)) {
            player.sendMessage("Bạn đang ở trong party khác!");
            return;
        }

        if (!isOwner(owner)) {
            player.sendMessage("Người này không phải chủ party!");
            return;
        }

        UUID partyId = playerToPartyMap.get(owner.getUniqueId());
        Party party = partyMap.get(partyId);

        if (party == null) return;

        if (party.getMembers().size() >= party.getSize()) {
            player.sendMessage("Party đã đầy!");
            return;
        }

        party.getMembers().add(player.getUniqueId());
        playerToPartyMap.put(player.getUniqueId(), partyId);

        player.sendMessage("Bạn đã tham gia party!");
    }

    /*
     * Leave a party
     * */
    public void leaveParty(Player player) {
        if (!isInParty(player)) {
            player.sendMessage("Bạn không ở trong party nào!");
            return;
        }

        if (isOwner(player)) {
            player.sendMessage("Chủ party phải dùng lệnh disband!");
            return;
        }

        UUID partyId = playerToPartyMap.remove(player.getUniqueId());
        Party party = partyMap.get(partyId);

        if (party != null) {
            party.getMembers().remove(player.getUniqueId());
        }

        player.sendMessage("Bạn đã rời party!");
    }

    public Party getParty(UUID partyId) {
        return partyMap.get(partyId);
    }

    public Party getParty(Player player) {
        UUID partyId = playerToPartyMap.get(player.getUniqueId());
        return partyId != null ? partyMap.get(partyId) : null;
    }

    public boolean isInParty(Player player) {
        return playerToPartyMap.containsKey(player.getUniqueId());
    }

    public boolean isOwner(Player player) {
        UUID partyId = playerToPartyMap.get(player.getUniqueId());
        if (partyId == null) return false;

        Party party = partyMap.get(partyId);
        return party != null && party.getOwner().equals(player.getUniqueId());
    }

    /*
     * Show a party
     * */
    public String getPartyInfo(Player player) {
        Party party = getParty(player);

        if (party == null) {
            return "Bạn không ở trong party nào.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("§a=== Thông tin Party ===\n");

        Player leader = Bukkit.getPlayer(party.getOwner());
        sb.append("§fLeader: ")
                .append(leader != null ? leader.getName() : "Offline")
                .append("\n");

        sb.append("§fThành viên:\n");

        for (UUID memberId : party.getMembers()) {
            Player member = Bukkit.getPlayer(memberId);
            sb.append(" - ")
                    .append(member != null ? member.getName() : "Offline")
                    .append("\n");
        }

        return sb.toString();
    }
}
