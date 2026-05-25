package com.roguesmp.island;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.player.PlayerData;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class IslandInviteManager {

    private static final long INVITE_DURATION_TICKS = 1200L;
    // key: "ReceiverUUID:SenderUUID"
    private static final Map<String, InviteRecord> inviteCache = new HashMap<>();

    /**
     * Dispatches an island team invite notification sequence to the targeted recipient.
     */
    public static void sendInvitation(Player sender, Player receiver) {

        UUID receiverUuid = receiver.getUniqueId();
        UUID senderUuid = sender.getUniqueId();

        PlayerData senderData = IslandManager.getInstance().getPlayerManager().getDataManager().getData(senderUuid);
        PlayerData receiverData = IslandManager.getInstance().getPlayerManager().getDataManager().getData(receiverUuid);
        if (senderData == null || senderData.getIslandId() == null) {
            sender.sendMessage(Utils.fromString("<red>Bạn cần phải có một hòn đảo trước khi thực hiện mời thành viên!"));
            return;
        }

        if (receiverUuid.equals(senderUuid)) {
            sender.sendMessage(Utils.fromString("<red>Ai lại tự đi mời bản thân...?"));
            return;
        }

        if (receiverData != null && senderData.getIslandId().equals(receiverData.getIslandId())) {
            sender.sendMessage(Utils.fromString("<red>Hai bạn đã sống chung với nhau rồi..."));
            return;
        }

        String cacheKey = receiverUuid + ":" + senderUuid;

        InviteRecord oldInvite = inviteCache.get(cacheKey);
        if (oldInvite != null) {
            sender.sendMessage(Utils.fromString("<red>Lời mời cũ vẫn còn hiệu lực!"));
            return;
        }

        // 2. Schedule the expiration task
        BukkitTask selfDestructTask = Bukkit.getScheduler().runTaskLater(RogueSmpCore.getInstance(), () -> {
            InviteRecord activeRecord = inviteCache.remove(cacheKey);
            if (activeRecord != null) {
                if (sender.isOnline()) sender.sendMessage(Utils.fromString("<red>Lời mời gửi đến <b>" + receiver.getName() + "</b> đã hết hạn."));
                if (receiver.isOnline()) receiver.sendMessage(Utils.fromString("<red>Lời mời tham gia đảo từ <b>" + sender.getName() + "</b> đã hết hạn."));
            }
        }, INVITE_DURATION_TICKS);

        // 3. Save using the unique compound key
        inviteCache.put(cacheKey, new InviteRecord(selfDestructTask.getTaskId()));

        sender.sendMessage(Utils.fromString("<green>Đã gửi lời mời tham gia đảo đến <b>" + receiver.getName() + "</b> <yellow>(Hiệu lực trong 60 giây)<green>!"));

        receiver.sendMessage(Utils.fromString("<yellow>========================================"));
        receiver.sendMessage(Utils.fromString("<green>Người chơi <b>" + sender.getName() + "</b> muốn mời bạn vào đảo của họ!"));
        receiver.sendMessage(Utils.fromString("<green>Lời mời này sẽ hết hạn sau <yellow>60 giây."));

        // Use suggestCommand to paste the text into their chat box instead of forcing execution
        Component choiceButtons = Component.text("   ")
                .append(Component.text("[ CHẤP NHẬN ]", NamedTextColor.GREEN, TextDecoration.BOLD)
                        .clickEvent(net.kyori.adventure.text.event.ClickEvent.suggestCommand("/is accept " + sender.getName()))
                        .hoverEvent(Component.text("Click để nhập lệnh đồng ý vào ô chat.", NamedTextColor.GRAY)))
                .append(Component.text("    "))
                .append(Component.text("[ TỪ CHỐI ]", NamedTextColor.RED, TextDecoration.BOLD)
                        .clickEvent(net.kyori.adventure.text.event.ClickEvent.suggestCommand("/is deny " + sender.getName()))
                        .hoverEvent(Component.text("Click để nhập lệnh từ chối vào ô chat.", NamedTextColor.GRAY)));

        receiver.sendMessage(choiceButtons);
        receiver.sendMessage(Utils.fromString("<yellow>========================================"));
    }

    /**
     * Verifies the invite's state and clears it from cache upon extraction.
     */
    public static boolean validateAndConsumeInvite(Player receiver, Player sender) {
        String cacheKey = receiver.getUniqueId() + ":" + sender.getUniqueId();
        InviteRecord record = inviteCache.remove(cacheKey);

        if (record == null) {
            return false;
        }

        Bukkit.getScheduler().cancelTask(record.taskId());
        return true;
    }

    private record InviteRecord(int taskId) {}
}
