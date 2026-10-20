package com.roguesmp.party;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PartyInviteManager {
    private final Map<UUID, Map<UUID, BukkitTask>> pendingInvites = new HashMap<>();
    private final PartyManager partyManager;
    private final Plugin plugin;

    public PartyInviteManager(Plugin plugin, PartyManager partyManager) {
        this.partyManager = partyManager;
        this.plugin = plugin;
    }

    public boolean sendInvite(Player inviter, Player target) {

        if (!partyManager.isOwner(inviter)) {
            inviter.sendMessage(Component.text("Chỉ chủ party mới có thể mời người khác!", NamedTextColor.RED));
            return false;
        }

        if (partyManager.isInParty(target)) {
            inviter.sendMessage(Component.text("Người chơi này đã ở trong party!", NamedTextColor.RED));
            return false;
        }

        if (inviter.getUniqueId().equals(target.getUniqueId())) {
            inviter.sendMessage(Component.text("Bạn không thể tự mời chính mình.", NamedTextColor.RED));
            return false;
        }

        pendingInvites.putIfAbsent(target.getUniqueId(), new HashMap<>());
        Map<UUID, BukkitTask> invites = pendingInvites.get(target.getUniqueId());

        if (invites.containsKey(inviter.getUniqueId())) {
            inviter.sendMessage(Component.text("Bạn đã gửi lời mời trước đó.", NamedTextColor.RED));
            return false;
        }

        target.sendMessage(Component.text(inviter.getName() + " đã mời bạn vào party.")
                .color(NamedTextColor.YELLOW));

        Component accept = Component.text("[CHẤP NHẬN]")
                .color(NamedTextColor.GREEN)
                .clickEvent(ClickEvent.runCommand("/party accept " + inviter.getName()));

        Component deny = Component.text("[TỪ CHỐI]")
                .color(NamedTextColor.RED)
                .clickEvent(ClickEvent.runCommand("/party deny " + inviter.getName()));

        target.sendMessage(accept.appendSpace().append(deny));
        target.sendMessage(Component.text("Lời mời hết hạn sau 2 phút.").color(NamedTextColor.GRAY));

        BukkitTask expireTask = Bukkit.getScheduler().runTaskLater(
                plugin,
                () -> expireInvite(target.getUniqueId(), inviter.getUniqueId(), target, inviter),
                20L * 120
        );

        invites.put(inviter.getUniqueId(), expireTask);
        inviter.sendMessage(Component.text("Đã gửi lời mời tới " + target.getName() + ".", NamedTextColor.GREEN));
        return true;
    }

    public void acceptInvite(Player target, String inviterName) {
        Player inviter = Bukkit.getPlayerExact(inviterName);

        if (inviter == null) {
            target.sendMessage(Component.text("Người mời không online.", NamedTextColor.RED));
            return;
        }

        if (!hasInviteFrom(target, inviter)) {
            target.sendMessage(Component.text("Bạn không có lời mời từ người này.", NamedTextColor.RED));
            return;
        }

        clearInvites(target.getUniqueId());
        partyManager.joinParty(target, inviter);
        inviter.sendMessage(Component.text(target.getName() + " đã tham gia party!", NamedTextColor.GREEN));
    }

    public void denyInvite(Player target, String inviterName) {
        Player inviter = Bukkit.getPlayerExact(inviterName);

        if (inviter == null) {
            target.sendMessage(Component.text("Người mời không online.", NamedTextColor.RED));
            return;
        }

        if (!hasInviteFrom(target, inviter)) {
            target.sendMessage(Component.text("Bạn không có lời mời từ người này.", NamedTextColor.RED));
            return;
        }

        removeInvite(target.getUniqueId(), inviter.getUniqueId());
        target.sendMessage(Component.text("Bạn đã từ chối lời mời.", NamedTextColor.RED));
        inviter.sendMessage(Component.text(target.getName() + " đã từ chối lời mời.", NamedTextColor.RED));
    }

    public void handlePlayerQuit(Player player) {
        clearInvites(player.getUniqueId());

        for (Map<UUID, BukkitTask> map : pendingInvites.values()) {
            BukkitTask task = map.remove(player.getUniqueId());
            if (task != null) task.cancel();
        }
    }

    private boolean hasInviteFrom(Player target, Player inviter) {
        Map<UUID, BukkitTask> invites = pendingInvites.get(target.getUniqueId());
        return invites != null && invites.containsKey(inviter.getUniqueId());
    }

    private void clearInvites(UUID targetId) {
        Map<UUID, BukkitTask> invites = pendingInvites.remove(targetId);
        if (invites != null) invites.values().forEach(BukkitTask::cancel);
    }

    private boolean removeInvite(UUID targetId, UUID inviterId) {
        Map<UUID, BukkitTask> invites = pendingInvites.get(targetId);
        if (invites == null) return false;

        BukkitTask task = invites.remove(inviterId);
        if (task != null) task.cancel();

        if (invites.isEmpty()) {
            pendingInvites.remove(targetId);
        }
        return task != null;
    }

    private void expireInvite(UUID targetId, UUID inviterId, Player target, Player inviter) {
        if (!removeInvite(targetId, inviterId)) return;
        if (partyManager.isInParty(target)) return;

        if (target.isOnline()) target.sendMessage(Component.text("Lời mời từ " + inviter.getName() + " đã hết hạn.", NamedTextColor.GRAY));
        if (inviter.isOnline()) inviter.sendMessage(Component.text("Lời mời tới " + target.getName() + " đã hết hạn.", NamedTextColor.GRAY));
    }
}