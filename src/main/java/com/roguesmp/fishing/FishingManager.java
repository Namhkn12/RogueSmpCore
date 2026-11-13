package com.roguesmp.fishing;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.event.SmpFishEvent;
import com.roguesmp.event.SmpFishStartEvent;
import com.roguesmp.loot.LootTable;
import com.roguesmp.loot.context.LootContext;
import com.roguesmp.loot.context.LootOrigin;
import com.roguesmp.registry.Holder;
import com.roguesmp.registry.Registries;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.utils.Utils;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class FishingManager {

    private static final double PULL_STRENGTH = 0.1;
    private static final double PULL_LIFT = 0.2;
    private static final Component HOW_TO_PLAY = Component.text()
            .append(Component.text("Cách câu cá", NamedTextColor.GOLD))
            .append(Component.newline())
            .append(Utils.fromString("<gray>Giữ <key:key.sneak> để đẩy con trỏ sang phải,"))
            .append(Component.newline())
            .append(Component.text("thả ra để nó trượt về bên trái.", NamedTextColor.GRAY))
            .append(Component.newline())
            .append(Component.text("Giữ con trỏ trong thanh di chuyển để tăng tiến độ.", NamedTextColor.GRAY))
            .build();

    private static FishingManager INSTANCE;

    private final RogueSmpCore plugin;
    private final Map<UUID, Session> sessions = new HashMap<>();

    private FishingManager(RogueSmpCore plugin) {
        this.plugin = plugin;
    }

    public static void init(RogueSmpCore plugin) {
        INSTANCE = new FishingManager(plugin);
    }

    public static FishingManager getInstance() {
        return INSTANCE;
    }

    public boolean isFishing(Player player) {
        return sessions.containsKey(player.getUniqueId());
    }

    public boolean start(Player player, FishHook hook) {
        if (isFishing(player)) return true;

        FishingHotspotManager.ActiveHotspot active = FishingHotspotManager.getInstance().findAt(hook.getLocation());
        String hotspotId = active == null ? null : active.hotspotId();

        SmpFishStartEvent startEvent = new SmpFishStartEvent(player, hook, hotspotId, FishTier.roll(tier -> active == null ? 1.0 : active.hotspot().tierMultiplier(tier.id())));
        if (!startEvent.callEvent()) return false;

        FishingEffects.bite(player, hook.getLocation(), startEvent.getTier(), hotspotId != null);

        FishingMinigame game = new FishingMinigame(startEvent.getSettings());
        Session session = new Session(new FishingDisplay(player, hook), hook, game, startEvent);
        sessions.put(player.getUniqueId(), session);

        session.task = player.getScheduler().runAtFixedRate(
                plugin,
                task -> tick(player, session),
                () -> dispose(player, session),
                1L,
                1L
        );
        return true;
    }

    public void abort(Player player) {
        Session session = sessions.remove(player.getUniqueId());
        if (session == null) return;

        session.task.cancel();
        session.display.remove();
        removeHook(session.hook);
    }

    private void tick(Player player, Session session) {
        if (!session.hook.isValid()) {
            end(player, session);
            return;
        }

        FishingMinigame.State state = session.game.tick(player.isSneaking());

        if (state == FishingMinigame.State.RUNNING) {
            session.display.update(session.game);
            FishingEffects.progress(player, session.hook.getLocation(), session.game, session.tier, session.hotspotId != null, session.ticks++, session.wasOnTarget);
            session.wasOnTarget = session.game.isOnTarget();
            return;
        }

        end(player, session);

        if (state == FishingMinigame.State.CAUGHT) {
            if (reward(player, session)) FishingEffects.caught(player, session.hook.getLocation(), session.tier, session.hotspotId != null);
        } else {
            notifyEscaped(player, session);
        }
        removeHook(session.hook);
    }

    public void escape(Player player) {
        Session session = sessions.get(player.getUniqueId());
        if (session == null) return;

        end(player, session);
        notifyEscaped(player, session);
        removeHook(session.hook);
    }

    private void notifyEscaped(Player player, Session session) {
        player.sendActionBar(Component.text("Cá đã thoát mất...", NamedTextColor.RED));
        player.sendMessage(Component.text("Cá đã thoát mất... ", NamedTextColor.RED)
                .append(Component.text("[Cách chơi]", NamedTextColor.YELLOW))
                .hoverEvent(HoverEvent.showText(HOW_TO_PLAY)));
        FishingEffects.escaped(player, session.hook.getLocation());
    }

    private void end(Player player, Session session) {
        session.task.cancel();
        dispose(player, session);
    }

    private void dispose(Player player, Session session) {
        sessions.remove(player.getUniqueId(), session);
        session.display.remove();
    }

    private boolean reward(Player player, Session session) {
        Location hookLocation = session.hook.getLocation();
        Vector pull = player.getLocation().toVector().subtract(hookLocation.toVector()).multiply(PULL_STRENGTH).setY(PULL_LIFT);

        SmpFishEvent fishEvent = new SmpFishEvent(player, session.tier, session.hotspotId, rollCatch(player, session));
        if (!fishEvent.callEvent()) return false;
        List<ItemStack> caught = fishEvent.getCaught();

        for (ItemStack stack : caught) {
            hookLocation.getWorld().dropItem(hookLocation, stack, item -> item.setVelocity(pull));
        }
        player.sendActionBar(Component.text("Bạn đã câu được thứ gì đó!", NamedTextColor.GREEN));
        return true;
    }

    private List<ItemStack> rollCatch(Player player, Session session) {
        LootContext context = LootContext.builder(PlayerManager.getInstance().getSmpPlayer(player))
                .origin(LootOrigin.FISHING, session.hook)
                .luck(player.getAttribute(Attribute.LUCK).getValue() / 100.0 + session.luckBonus)
                .build();

        List<ItemStack> results = new ArrayList<>();
        for (Holder<LootTable> table : session.lootTables) {
            if (!table.isBound()) {
                RogueSmpCore.LOGGER.warn("[Fishing] Loot table not found: '{}'", table.getId());
                continue;
            }
            results.addAll(table.value().roll(context));
        }
        return results;
    }

    private void removeHook(FishHook hook) {
        hook.getScheduler().run(plugin, task -> hook.remove(), null);
    }

    private static final class Session {
        private final FishingDisplay display;
        private final FishHook hook;
        private final FishingMinigame game;
        private final FishTier tier;
        private final @Nullable String hotspotId;
        private final List<Holder<LootTable>> lootTables;
        private final double luckBonus;
        private ScheduledTask task;
        private int ticks;
        private boolean wasOnTarget;

        private Session(FishingDisplay display, FishHook hook, FishingMinigame game, SmpFishStartEvent start) {
            this.display = display;
            this.hook = hook;
            this.game = game;
            this.tier = start.getTier();
            this.hotspotId = start.getHotspotId();
            this.lootTables = List.copyOf(start.getLootTables());
            this.luckBonus = start.getLuckBonus();
        }
    }
}
