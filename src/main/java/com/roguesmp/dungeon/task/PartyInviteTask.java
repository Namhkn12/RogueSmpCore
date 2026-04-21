package com.roguesmp.dungeon.task;

import com.roguesmp.dungeon.service.IPartyService;
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

public class PartyInviteTask {

    // targetUUID → (inviterUUID → expireTask)
    private final Map<UUID, Map<UUID, BukkitTask>> pendingInvites = new HashMap<>();
    private final IPartyService partyService;
    private final Plugin plugin;

    public PartyInviteTask(Plugin plugin, IPartyService partyService) {
        this.partyService = partyService;
        this.plugin = plugin;
    }

    public boolean sendInvite(Player inviter, Player target) {

        if (!partyService.isOwner(inviter)) {
            inviter.sendMessage("§cChỉ chủ party mới có thể mời người khác!");
            return false;
        }

        if (partyService.isInParty(target)) {
            inviter.sendMessage("§cNgười chơi này đã ở trong party!");
            return false;
        }

        if (inviter.getUniqueId().equals(target.getUniqueId())) {
            inviter.sendMessage("§cBạn không thể tự mời chính mình.");
            return false;
        }

        pendingInvites.putIfAbsent(target.getUniqueId(), new HashMap<>());
        Map<UUID, BukkitTask> invites = pendingInvites.get(target.getUniqueId());

        if (invites.containsKey(inviter.getUniqueId())) {
            inviter.sendMessage("§cBạn đã gửi lời mời trước đó.");
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
        inviter.sendMessage("§aĐã gửi lời mời tới " + target.getName() + ".");
        return true;
    }

    public void acceptInvite(Player target, String inviterName) {
        Player inviter = Bukkit.getPlayerExact(inviterName);

        if (inviter == null) {
            target.sendMessage("§cNgười mời không online.");
            return;
        }

        if (!hasInviteFrom(target, inviter)) {
            target.sendMessage("§cBạn không có lời mời từ người này.");
            return;
        }

        removeInvite(target.getUniqueId(), inviter.getUniqueId());
        partyService.joinParty(target, inviter);
        inviter.sendMessage("§a" + target.getName() + " đã tham gia party!");
    }

    public void denyInvite(Player target, String inviterName) {
        Player inviter = Bukkit.getPlayerExact(inviterName);

        if (inviter == null) {
            target.sendMessage("§cNgười mời không online.");
            return;
        }

        if (!hasInviteFrom(target, inviter)) {
            target.sendMessage("§cBạn không có lời mời từ người này.");
            return;
        }

        removeInvite(target.getUniqueId(), inviter.getUniqueId());
        target.sendMessage("§cBạn đã từ chối lời mời.");
        inviter.sendMessage("§c" + target.getName() + " đã từ chối lời mời.");
    }

    public void handlePlayerQuit(Player player) {
        Map<UUID, BukkitTask> invites = pendingInvites.remove(player.getUniqueId());
        if (invites != null) {
            invites.values().forEach(BukkitTask::cancel);
        }

        for (Map<UUID, BukkitTask> map : pendingInvites.values()) {
            BukkitTask task = map.remove(player.getUniqueId());
            if (task != null) task.cancel();
        }
    }

    // ── Internal ─────────────────────────────────────────────

    private boolean hasInviteFrom(Player target, Player inviter) {
        Map<UUID, BukkitTask> invites = pendingInvites.get(target.getUniqueId());
        return invites != null && invites.containsKey(inviter.getUniqueId());
    }

    private void removeInvite(UUID targetId, UUID inviterId) {
        Map<UUID, BukkitTask> invites = pendingInvites.get(targetId);
        if (invites == null) return;

        BukkitTask task = invites.remove(inviterId);
        if (task != null) task.cancel();

        if (invites.isEmpty()) {
            pendingInvites.remove(targetId);
        }
    }

    private void expireInvite(UUID targetId, UUID inviterId, Player target, Player inviter) {
        removeInvite(targetId, inviterId);

        if (target.isOnline()) target.sendMessage("§7Lời mời từ " + inviter.getName() + " đã hết hạn.");
        if (inviter.isOnline()) inviter.sendMessage("§7Lời mời tới " + target.getName() + " đã hết hạn.");
    }
}