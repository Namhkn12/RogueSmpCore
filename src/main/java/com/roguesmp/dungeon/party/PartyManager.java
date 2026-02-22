package com.roguesmp.dungeon.party;

import com.roguesmp.RogueSmpCore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.logging.Logger;

public class PartyManager {
    private final Map<UUID, Party> partyManager = new HashMap<>();

    private static final Logger logger = RogueSmpCore.getInstance().getLogger();

    public void register(){
        //refresh

    }

    public void refreshPSDPartyId(Player player) {
        String partyId = player.getPersistentDataContainer().get(NamespacedKeys.PARTY_KEY, PersistentDataType.STRING);

        if (partyId != null) {
            logger.warning("Found player " + player.getName() + " with psd partyId: " + partyId);
            player.getPersistentDataContainer().remove(NamespacedKeys.PARTY_KEY);
        } else {
            logger.info("Player " + player.getName() + " has no partyId");
        }
    }


    public void createParty(Player sender){
        if(isInParty(sender)){
            sender.sendMessage("Bạn đã có party");
            return;
        }
        Party party = new Party(sender.getUniqueId(), sender.getName(), new ArrayList<>(), 3, true);
        sender.getPersistentDataContainer().set(NamespacedKeys.PARTY_KEY, PersistentDataType.STRING, party.getPartyId().toString());
        partyManager.put(party.getPartyId(), party);
        sender.sendMessage("Bạn đã tạo 1 party! Hãy mời đồng đội cùng tham gia");
    }

    public void removeParty(Player sender){
        if(!isInParty(sender)){
            sender.sendMessage("Bạn không thuộc party nào!");
            return;
        }
        if(!isOwner(sender)){
            sender.sendMessage("Bạn không có quyền xóa party!");
            return;
        }

        partyManager.get(sender.getUniqueId()).getMembers().forEach(uuid -> {
            Player member = Bukkit.getPlayer(uuid);
            if(member != null) member.getPersistentDataContainer().remove(NamespacedKeys.PARTY_KEY);
        });
        sender.getPersistentDataContainer().remove(NamespacedKeys.PARTY_KEY);
        partyManager.remove(sender.getUniqueId());
        sender.sendMessage("Bạn đã giải tán party!");
    }

    public Party getParty(UUID partyId){
        return partyManager.get(partyId);
    }

    public void joinParty(Player sender, Player inviter){
        if(isInParty(sender)){
            sender.sendMessage("Bạn đang trong 1 party! Hãy rời party cũ trước khi tham gia!");
            return;
        }
        if(!isOwner(inviter)){
            sender.sendMessage("Người này không phải chủ team!");
            return;
        }
        //check : have inviter

        partyManager.get(inviter.getUniqueId()).getMembers().add(sender.getUniqueId());
        sender.getPersistentDataContainer().set(NamespacedKeys.PARTY_KEY, PersistentDataType.STRING, inviter.getUniqueId().toString());
        sender.sendMessage("Bạn đã tham gia party của " + inviter.getName());

    }

    public void leaveParty(Player sender){
        if(!isInParty(sender)){
            sender.sendMessage("Bạn không thuộc party nào!");
            return;
        }
        if(isOwner(sender)){
            sender.sendMessage("Hãy dùng /team disband để bỏ team");
            return;
        }
        UUID partyId = UUID.fromString(Objects.requireNonNull(sender.getPersistentDataContainer().get(NamespacedKeys.PARTY_KEY, PersistentDataType.STRING)));
        partyManager.get(partyId).getMembers().remove(sender.getUniqueId());
        sender.getPersistentDataContainer().remove(NamespacedKeys.PARTY_KEY);
        sender.sendMessage("Bạn đã rời party của " + getParty(partyId).getOwner());
    }

    public Party getPartyFromPlayer(Player player){
        String psdPartyId = player.getPersistentDataContainer().get(NamespacedKeys.PARTY_KEY, PersistentDataType.STRING);
        if(psdPartyId != null){
            UUID partyId = UUID.fromString(psdPartyId);
            Party party = partyManager.get(partyId);
            if(party == null){
                player.getPersistentDataContainer().remove(NamespacedKeys.PARTY_KEY);
                return null;
            }
            return party;
        }
        return null;
    }

    public boolean isInParty(Player player){
        String psdPartyId = player.getPersistentDataContainer().get(NamespacedKeys.PARTY_KEY, PersistentDataType.STRING);
        Bukkit.getLogger().warning("PSD PartyId:" + psdPartyId);
        if(psdPartyId == null){
            player.getPersistentDataContainer().remove(NamespacedKeys.PARTY_KEY);
            return false;
        }
        return partyManager.containsKey(UUID.fromString(psdPartyId));
    }

    public boolean isOwner(Player player){
        return partyManager.containsKey(player.getUniqueId());
    }

    public boolean isPartyOwner(Player player){
        return partyManager.containsKey(player.getUniqueId());
    }

    public String getPartyInfo(Player player) {
        Party party = getPartyFromPlayer(player);

        if (party == null) {
            return "Bạn không ở trong party nào.";
        }

        StringBuilder sb = new StringBuilder();

        sb.append("§a=== Thông tin Party ===\n");
        sb.append("§fLeader: ").append(Bukkit.getPlayer(party.getOwner())).append("\n");
        sb.append("§fThành viên:\n");

        for (UUID memberId : party.getMembers()) {
            Player member = Bukkit.getPlayer(memberId);
            sb.append(" - ").append(member != null ? member.getName() : "Offline player").append("\n");
        }

        return sb.toString();
    }
}
