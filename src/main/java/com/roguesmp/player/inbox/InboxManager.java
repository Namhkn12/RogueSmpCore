package com.roguesmp.player.inbox;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.inbox.gui.InboxGui;
import com.roguesmp.storage.JsonSqliteStore;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InboxManager {

    private static final long EXPIRY_INTERVAL_TICKS = 20L * 60 * 60;
    private static InboxManager INSTANCE;

    private final Map<UUID, Inbox> inboxes = new HashMap<>();
    private final Map<UUID, Inbox> pendingJoins = new ConcurrentHashMap<>();
    private final Map<UUID, List<InboxEntry>> undelivered = new HashMap<>();
    private final JsonSqliteStore<Inbox> store;

    private InboxManager(RogueSmpCore plugin) {
        this.store = new JsonSqliteStore<>(plugin, "player_inbox.db", "player_inbox", "uuid", Inbox.CODEC);
    }

    public @Blocking void stagePendingJoin(UUID uuid) {
        Inbox loaded = store.load(uuid);
        pendingJoins.put(uuid, loaded != null ? loaded : new Inbox(uuid));
    }

    /**
     * @return false if nothing was staged for the player
     */
    public boolean cachePendingJoin(UUID uuid) {
        Inbox inbox = pendingJoins.remove(uuid);
        if (inbox == null) return false;
        List<InboxEntry> waiting = undelivered.remove(uuid);
        if (waiting != null) waiting.forEach(inbox::add);
        inbox.expire(System.currentTimeMillis());
        inboxes.put(uuid, inbox);
        return true;
    }

    public void saveAndUncache(UUID uuid) {
        Inbox inbox = inboxes.remove(uuid);
        if (inbox != null) saveAsync(inbox);
    }

    public @Nullable Inbox getInbox(UUID uuid) {
        return inboxes.get(uuid);
    }

    public int count(UUID uuid) {
        Inbox inbox = inboxes.get(uuid);
        return inbox == null ? 0 : inbox.getEntries().size();
    }

    /**
     * Puts the entry in the player's inbox, online or not. An offline player's row is changed in place on the store;
     * the entry is also kept in memory until they join, in case their inbox was being loaded at the same time.
     */
    public void send(UUID playerId, InboxEntry entry) {
        Inbox cached = inboxes.get(playerId);
        if (cached == null) {
            undelivered.computeIfAbsent(playerId, id -> new ArrayList<>()).add(entry);
            store.updateAsync(playerId, () -> new Inbox(playerId), inbox -> inbox.add(entry));
            return;
        }
        cached.add(entry);
        saveAsync(cached);
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            player.sendMessage(Utils.fromString("<gold>Phần thưởng từ </gold>" + entry.source().name() + " <gold>đã được gửi vào hộp thư. ").append(openButton()));
        }
    }

    public boolean claim(Player player, UUID entryId) {
        Inbox inbox = inboxes.get(player.getUniqueId());
        SmpPlayer smpPlayer = PlayerManager.getInstance().getSmpPlayer(player);
        if (inbox == null || smpPlayer == null) return false;
        boolean claimed = claim(player, smpPlayer, inbox, entryId);
        if (inbox.isDirty()) saveAsync(inbox);
        return claimed;
    }

    public int claimAll(Player player) {
        Inbox inbox = inboxes.get(player.getUniqueId());
        SmpPlayer smpPlayer = PlayerManager.getInstance().getSmpPlayer(player);
        if (inbox == null || smpPlayer == null) return 0;
        int claimed = 0;
        for (InboxEntry entry : List.copyOf(inbox.getEntries())) {
            if (claim(player, smpPlayer, inbox, entry.id())) claimed++;
        }
        if (inbox.isDirty()) saveAsync(inbox);
        return claimed;
    }

    private boolean claim(Player player, SmpPlayer smpPlayer, Inbox inbox, UUID entryId) {
        InboxEntry entry = inbox.remove(entryId);
        if (entry == null) return false;
        if (entry.isExpired(System.currentTimeMillis())) {
            player.sendMessage(Utils.fromString("<red>Phần thưởng này đã hết hạn!"));
            return false;
        }
        entry.rewards().forEach(reward -> reward.giveReward(smpPlayer));
        return true;
    }

    public void notifyPending(Player player) {
        int pending = count(player.getUniqueId());
        if (pending == 0) return;
        player.sendMessage(Utils.fromString("<yellow>Bạn có <gold>" + pending + "</gold> phần thưởng chưa nhận trong hộp thư. ").append(openButton()));
    }

    private Component openButton() {
        return Utils.fromString("<gold>[Mở hộp thư]</gold>").clickEvent(ClickEvent.callback(audience -> {
            if (audience instanceof Player viewer) new InboxGui(viewer, null).showInventory(viewer);
        }));
    }

    private void expireAll() {
        long now = System.currentTimeMillis();
        inboxes.values().forEach(inbox -> inbox.expire(now));
        undelivered.values().removeIf(entries -> {
            entries.removeIf(entry -> entry.isExpired(now));
            return entries.isEmpty();
        });
    }

    private void saveAsync(Inbox inbox) {
        String json = store.encode(new Inbox(inbox));
        if (json == null) return;
        inbox.setDirty(false);
        store.writeAsync(inbox.getUuid(), json).thenAccept(written -> {
            if (!written) inbox.setDirty(true);
        });
    }

    public @Blocking void onDisable() {
        for (Inbox inbox : inboxes.values()) {
            if (!inbox.isDirty()) continue;
            String json = store.encode(new Inbox(inbox));
            if (json != null && store.write(inbox.getUuid(), json)) inbox.setDirty(false);
        }
        store.close();
    }

    public static void init(RogueSmpCore plugin) {
        INSTANCE = new InboxManager(plugin);
        Bukkit.getScheduler().runTaskTimer(plugin, INSTANCE::expireAll, EXPIRY_INTERVAL_TICKS, EXPIRY_INTERVAL_TICKS);
    }

    public static InboxManager getInstance() {
        if (INSTANCE == null) throw new IllegalStateException("InboxManager is null!");
        return INSTANCE;
    }
}
