package com.roguesmp.island;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.player.PlayerData;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class IslandInviteManager {

    private static final int INVITE_DURATION_SECONDS = 60;
    private static final long INVITE_DURATION_TICKS = INVITE_DURATION_SECONDS * 20L;

    private final PlayerManager playerManager;
    private final IslandDataManager dataManager;
    private final IslandMembershipService membershipService;
    private final Map<InviteKey, BukkitTask> invites = new HashMap<>();

    public IslandInviteManager(PlayerManager playerManager, IslandDataManager dataManager, IslandMembershipService membershipService) {
        this.playerManager = playerManager;
        this.dataManager = dataManager;
        this.membershipService = membershipService;
    }

    public void sendInvitation(Player sender, Player receiver) {
        UUID senderId = sender.getUniqueId();
        UUID receiverId = receiver.getUniqueId();

        if (senderId.equals(receiverId)) {
            sender.sendMessage(Utils.fromString("<red>Ai lại tự đi mời bản thân...?"));
            return;
        }

        PlayerData senderData = playerManager.getDataManager().getData(senderId);
        if (senderData == null || senderData.getIslandId() == null) {
            sender.sendMessage(Utils.fromString("<red>Bạn cần phải có một hòn đảo trước khi thực hiện mời thành viên!"));
            return;
        }

        PlayerData receiverData = playerManager.getDataManager().getData(receiverId);
        if (receiverData != null && senderData.getIslandId().equals(receiverData.getIslandId())) {
            sender.sendMessage(Utils.fromString("<red>Hai bạn đã sống chung với nhau rồi..."));
            return;
        }

        IslandData islandData = dataManager.getCachedData(senderData.getIslandId());
        if (islandData != null && islandData.getMembers().size() >= IslandMembershipService.TEAM_SIZE) {
            sender.sendMessage(Utils.fromString("<red>Đảo của bạn đã đạt giới hạn tối đa thành viên!"));
            return;
        }

        InviteKey key = new InviteKey(receiverId, senderId);
        if (invites.containsKey(key)) {
            sender.sendMessage(Utils.fromString("<red>Lời mời cũ vẫn còn hiệu lực!"));
            return;
        }

        BukkitTask expiry = Bukkit.getScheduler().runTaskLater(RogueSmpCore.getInstance(), () -> expire(key), INVITE_DURATION_TICKS);
        invites.put(key, expiry);

        sender.sendMessage(Utils.fromString("<green>Đã gửi lời mời tham gia đảo đến <b>" + receiver.getName() + "</b> <yellow>(Hiệu lực trong " + INVITE_DURATION_SECONDS + " giây)<green>!"));
        notifyReceiver(sender, receiver);
    }

    public void accept(Player receiver, Player sender) {
        if (!consume(receiver.getUniqueId(), sender.getUniqueId())) {
            receiver.sendMessage(Utils.fromString("<red>Lời mời này đã hết hạn (quá " + INVITE_DURATION_SECONDS + " giây) hoặc không tồn tại!"));
            return;
        }

        PlayerData senderData = playerManager.getDataManager().getData(sender.getUniqueId());
        if (senderData == null || senderData.getIslandId() == null) {
            receiver.sendMessage(Utils.fromString("<red>Đảo của đối phương không còn tồn tại."));
            return;
        }

        if (membershipService.addMember(senderData.getIslandId(), receiver)) {
            receiver.sendMessage(Utils.fromString("<green>Chào mừng! Bạn đã gia nhập đảo của " + sender.getName()));
            sender.sendMessage(Utils.fromString("<green><b>" + receiver.getName() + "</b> đã chấp nhận lời mời và gia nhập hòn đảo của bạn!"));
        } else {
            receiver.sendMessage(Utils.fromString("<red>Không thể gia nhập đảo. Bạn có thể đã ở trong đảo khác hoặc đảo đã đầy!"));
        }
    }

    public void deny(Player receiver, Player sender) {
        if (!consume(receiver.getUniqueId(), sender.getUniqueId())) {
            receiver.sendMessage(Utils.fromString("<red>Lời mời này đã hết hạn hoặc không khả dụng."));
            return;
        }

        receiver.sendMessage(Utils.fromString("<red>Bạn đã từ chối lời mời của <b>" + sender.getName()));
        sender.sendMessage(Utils.fromString("<red>Người chơi <b>" + receiver.getName() + "</b> đã từ chối lời mời vào đảo."));
    }

    public void cancelInvitesInvolving(UUID playerId) {
        invites.entrySet().removeIf(entry -> {
            InviteKey key = entry.getKey();
            if (!key.receiver().equals(playerId) && !key.sender().equals(playerId)) return false;
            entry.getValue().cancel();
            return true;
        });
    }

    private boolean consume(UUID receiverId, UUID senderId) {
        BukkitTask task = invites.remove(new InviteKey(receiverId, senderId));
        if (task == null) return false;

        task.cancel();
        return true;
    }

    private void expire(InviteKey key) {
        if (invites.remove(key) == null) return;

        Player sender = Bukkit.getPlayer(key.sender());
        Player receiver = Bukkit.getPlayer(key.receiver());
        if (sender != null) sender.sendMessage(Utils.fromString("<red>Lời mời gửi đến <b>" + (receiver != null ? receiver.getName() : "người chơi") + "</b> đã hết hạn."));
        if (receiver != null) receiver.sendMessage(Utils.fromString("<red>Lời mời tham gia đảo từ <b>" + (sender != null ? sender.getName() : "người chơi") + "</b> đã hết hạn."));
    }

    private void notifyReceiver(Player sender, Player receiver) {
        Component choiceButtons = Component.text("   ")
                .append(Component.text("[ CHẤP NHẬN ]", NamedTextColor.GREEN, TextDecoration.BOLD)
                        .clickEvent(ClickEvent.suggestCommand("/is accept " + sender.getName()))
                        .hoverEvent(Component.text("Click để nhập lệnh đồng ý vào ô chat.", NamedTextColor.GRAY)))
                .append(Component.text("    "))
                .append(Component.text("[ TỪ CHỐI ]", NamedTextColor.RED, TextDecoration.BOLD)
                        .clickEvent(ClickEvent.suggestCommand("/is deny " + sender.getName()))
                        .hoverEvent(Component.text("Click để nhập lệnh từ chối vào ô chat.", NamedTextColor.GRAY)));

        receiver.sendMessage(Utils.fromString("<yellow>========================================"));
        receiver.sendMessage(Utils.fromString("<green>Người chơi <b>" + sender.getName() + "</b> muốn mời bạn vào đảo của họ!"));
        receiver.sendMessage(Utils.fromString("<green>Lời mời này sẽ hết hạn sau <yellow>" + INVITE_DURATION_SECONDS + " giây."));
        receiver.sendMessage(choiceButtons);
        receiver.sendMessage(Utils.fromString("<yellow>========================================"));
    }

    private record InviteKey(UUID receiver, UUID sender) {}
}
