package com.roguesmp.player.trade;

import com.roguesmp.RogueSmpCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerTradeManager {
    private static PlayerTradeManager INSTANCE = null;

    private static final long REQUEST_EXPIRE_TICKS = 20L * 60;
    private static final double MAX_TRADE_DISTANCE = 10;

    private final RogueSmpCore plugin;
    private final Map<UUID, Map<UUID, BukkitTask>> pendingRequests = new HashMap<>();
    private final Map<UUID, PlayerTradeSession> activeSessions = new HashMap<>();

    private PlayerTradeManager(RogueSmpCore plugin) {
        this.plugin = plugin;
    }

    public void handleShiftInteract(Player player, Player target) {
        if (hasRequestFrom(player, target)) {
            acceptRequest(player, target);
        } else {
            sendRequest(player, target);
        }
    }

    public void sendRequest(Player requester, Player target) {
        if (requester.getUniqueId().equals(target.getUniqueId())) {
            requester.sendMessage(Component.text("Bạn không thể giao dịch với chính mình.", NamedTextColor.RED));
            return;
        }
        if (!isInRange(requester, target)) {
            requester.sendMessage(Component.text("Bạn phải ở gần " + target.getName() + " để giao dịch.", NamedTextColor.RED));
            return;
        }
        if (isTrading(requester)) {
            requester.sendMessage(Component.text("Bạn đang trong một giao dịch khác.", NamedTextColor.RED));
            return;
        }
        if (isTrading(target)) {
            requester.sendMessage(Component.text(target.getName() + " đang trong một giao dịch khác.", NamedTextColor.RED));
            return;
        }

        Map<UUID, BukkitTask> requests = pendingRequests.computeIfAbsent(target.getUniqueId(), k -> new HashMap<>());
        if (requests.containsKey(requester.getUniqueId())) {
            requester.sendMessage(Component.text("Bạn đã gửi lời mời giao dịch tới người này rồi.", NamedTextColor.RED));
            return;
        }

        UUID targetId = target.getUniqueId();
        UUID requesterId = requester.getUniqueId();
        requests.put(requesterId, Bukkit.getScheduler().runTaskLater(plugin, () -> expireRequest(targetId, requesterId), REQUEST_EXPIRE_TICKS));

        Component accept = Component.text("[CHẤP NHẬN]", NamedTextColor.GREEN)
                .clickEvent(ClickEvent.runCommand("/trade accept " + requester.getName()));
        Component deny = Component.text("[TỪ CHỐI]", NamedTextColor.RED)
                .clickEvent(ClickEvent.runCommand("/trade deny " + requester.getName()));

        target.sendMessage(Component.text(requester.getName() + " muốn giao dịch với bạn.", NamedTextColor.YELLOW));
        target.sendMessage(accept.appendSpace().append(deny));
        target.sendMessage(Component.text("Shift + chuột phải vào " + requester.getName() + " để chấp nhận. Lời mời hết hạn sau 60 giây.", NamedTextColor.GRAY));
        requester.sendMessage(Component.text("Đã gửi lời mời giao dịch tới " + target.getName() + ".", NamedTextColor.GREEN));
    }

    public void acceptRequest(Player target, @Nullable Player requester) {
        if (requester == null) {
            target.sendMessage(Component.text("Người chơi này không online.", NamedTextColor.RED));
            return;
        }
        if (!hasRequestFrom(target, requester)) {
            target.sendMessage(Component.text("Bạn không có lời mời giao dịch từ người này.", NamedTextColor.RED));
            return;
        }
        if (!isInRange(target, requester)) {
            target.sendMessage(Component.text("Bạn phải ở gần " + requester.getName() + " để giao dịch.", NamedTextColor.RED));
            return;
        }
        removeRequest(target.getUniqueId(), requester.getUniqueId());
        if (isTrading(target) || isTrading(requester)) {
            target.sendMessage(Component.text("Một trong hai người đang trong giao dịch khác.", NamedTextColor.RED));
            return;
        }

        clearRequestsInvolving(target.getUniqueId());
        clearRequestsInvolving(requester.getUniqueId());

        PlayerTradeSession session = new PlayerTradeSession(this, requester, target);
        activeSessions.put(requester.getUniqueId(), session);
        activeSessions.put(target.getUniqueId(), session);
        session.open();
    }

    public void denyRequest(Player target, @Nullable Player requester) {
        if (requester == null || !removeRequest(target.getUniqueId(), requester.getUniqueId())) {
            target.sendMessage(Component.text("Bạn không có lời mời giao dịch từ người này.", NamedTextColor.RED));
            return;
        }
        target.sendMessage(Component.text("Bạn đã từ chối lời mời giao dịch.", NamedTextColor.RED));
        requester.sendMessage(Component.text(target.getName() + " đã từ chối lời mời giao dịch.", NamedTextColor.RED));
    }

    public void handleQuit(Player player) {
        clearRequestsInvolving(player.getUniqueId());
        PlayerTradeSession session = activeSessions.get(player.getUniqueId());
        if (session != null) session.cancel(player.getName() + " đã rời khỏi máy chủ.");
    }

    public void cancelAll() {
        Map.copyOf(activeSessions).values().forEach(session -> session.cancel("Máy chủ đang tắt."));
    }

    void endSession(PlayerTradeSession session) {
        activeSessions.values().removeIf(s -> s == session);
    }

    public boolean isTrading(Player player) {
        return activeSessions.containsKey(player.getUniqueId());
    }

    private boolean isInRange(Player player, Player other) {
        return player.getWorld().equals(other.getWorld())
                && player.getLocation().distanceSquared(other.getLocation()) <= MAX_TRADE_DISTANCE * MAX_TRADE_DISTANCE;
    }

    private boolean hasRequestFrom(Player target, Player requester) {
        Map<UUID, BukkitTask> requests = pendingRequests.get(target.getUniqueId());
        return requests != null && requests.containsKey(requester.getUniqueId());
    }

    private boolean removeRequest(UUID targetId, UUID requesterId) {
        Map<UUID, BukkitTask> requests = pendingRequests.get(targetId);
        if (requests == null) return false;

        BukkitTask task = requests.remove(requesterId);
        if (task != null) task.cancel();
        if (requests.isEmpty()) pendingRequests.remove(targetId);
        return task != null;
    }

    private void clearRequestsInvolving(UUID playerId) {
        Map<UUID, BukkitTask> received = pendingRequests.remove(playerId);
        if (received != null) received.values().forEach(BukkitTask::cancel);

        pendingRequests.values().forEach(requests -> {
            BukkitTask task = requests.remove(playerId);
            if (task != null) task.cancel();
        });
        pendingRequests.values().removeIf(Map::isEmpty);
    }

    private void expireRequest(UUID targetId, UUID requesterId) {
        if (!removeRequest(targetId, requesterId)) return;

        Player target = Bukkit.getPlayer(targetId);
        Player requester = Bukkit.getPlayer(requesterId);
        if (target != null && requester != null) {
            target.sendMessage(Component.text("Lời mời giao dịch từ " + requester.getName() + " đã hết hạn.", NamedTextColor.GRAY));
            requester.sendMessage(Component.text("Lời mời giao dịch tới " + target.getName() + " đã hết hạn.", NamedTextColor.GRAY));
        }
    }

    public static PlayerTradeManager getInstance() {
        return INSTANCE;
    }

    public static void init(RogueSmpCore plugin) {
        if (INSTANCE == null) INSTANCE = new PlayerTradeManager(plugin);
    }
}
