package com.roguesmp.dungeon.party;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class PartyInviteManager {
    private final Map<UUID, Map<UUID, Long>> pendingInvites = new HashMap<>();

    public void sendInvite(Player inviter, Player target) {

        pendingInvites.putIfAbsent(target.getUniqueId(), new HashMap<>());
        Map<UUID, Long> invites = pendingInvites.get(target.getUniqueId());

        if (invites.containsKey(inviter.getUniqueId())) {
            inviter.sendMessage(Component.text("Bạn đã gửi lời mời cho " + target.getName() + ".").color(NamedTextColor.RED));
            return;
        }

        target.sendMessage(Component.text(inviter.getName() + " đã mời bạn vào party của họ.").color(NamedTextColor.YELLOW));

        Component accept = Component.text("[CHẤP NHẬN]")
                .color(NamedTextColor.GREEN)
                .clickEvent(ClickEvent.runCommand("/party join " + inviter.getName()));

        Component deny = Component.text("[TỪ CHỐI]")
                .color(NamedTextColor.RED)
                .clickEvent(ClickEvent.runCommand("/party deny " + inviter.getName()));

        target.sendMessage(accept.appendSpace().append(deny));
        target.sendMessage(Component.text("Lời mời có hiệu lực trong 1 phút."));

        long expireAt = System.currentTimeMillis() + 60_000;
        invites.put(inviter.getUniqueId(), expireAt);
    }


    public void acceptInvite(Player target, UUID inviterId) {

        Map<UUID, Long> invites = pendingInvites.get(target.getUniqueId());

        if (invites == null || !invites.containsKey(inviterId)) {
            target.sendMessage("§cLời mời đã hết hạn hoặc không tồn tại.");
            return;
        }

        long expireAt = invites.get(inviterId);

        if (System.currentTimeMillis() > expireAt) {
            invites.remove(inviterId);
            if (invites.isEmpty()) pendingInvites.remove(target.getUniqueId());
            target.sendMessage("§cLời mời đã hết hạn!");
            return;
        }

        invites.remove(inviterId);
        if (invites.isEmpty()) pendingInvites.remove(target.getUniqueId());

        Player inviter = Bukkit.getPlayer(inviterId);
        target.sendMessage("§aBạn đã chấp nhận lời mời từ " + (inviter != null ? inviter.getName() : "người chơi"));
    }


    public void denyInvite(Player target, UUID inviterId) {

        Map<UUID, Long> invites = pendingInvites.get(target.getUniqueId());

        if (invites == null || !invites.containsKey(inviterId)) {
            target.sendMessage("§cLời mời đã hết hạn hoặc không tồn tại!");
            return;
        }

        invites.remove(inviterId);
        if (invites.isEmpty()) pendingInvites.remove(target.getUniqueId());

        Player inviter = Bukkit.getPlayer(inviterId);
        target.sendMessage("§cBạn đã từ chối lời mời từ " + (inviter != null ? inviter.getName() : "người chơi"));
    }


    public String[] getInviters(Player target) {
        Map<UUID, Long> invites = pendingInvites.get(target.getUniqueId());
        if (invites == null || invites.isEmpty()) return new String[0];

        long now = System.currentTimeMillis();

        invites.entrySet().removeIf(entry -> entry.getValue() < now);

        if (invites.isEmpty()) {
            pendingInvites.remove(target.getUniqueId());
            return new String[0];
        }

        return invites.keySet().stream()
                .map(uuid -> {
                    Player p = Bukkit.getPlayer(uuid);
                    return p != null ? p.getName() : null;
                })
                .filter(Objects::nonNull)
                .toArray(String[]::new);
    }


}
