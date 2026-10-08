package com.roguesmp.fishing;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.loot.LootTable;
import com.roguesmp.loot.context.LootContext;
import com.roguesmp.loot.context.LootOrigin;
import com.roguesmp.registry.Holder;
import com.roguesmp.registry.Registries;
import com.roguesmp.player.PlayerManager;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
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
import java.util.concurrent.ThreadLocalRandom;

public final class FishingManager {

    private static final double PULL_STRENGTH = 0.1;
    private static final double PULL_LIFT = 0.2;

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

    public void start(Player player, FishHook hook) {
        if (isFishing(player)) return;

        FishingHotspotManager.ActiveHotspot active = FishingHotspotManager.getInstance().findAt(hook.getLocation());
        FishingHotspot hotspot = active == null ? null : active.hotspot();

        FishTier tier = rollTier(hotspot);

        if (hotspot != null) player.sendActionBar(Utils.fromString("<aqua>Hotspot: ").append(Utils.fromString(hotspot.displayName())));

        FishingMinigame game = new FishingMinigame(FishingMinigameSettings.of(tier, hotspot));
        Session session = new Session(new FishingDisplay(player, hook), hook, game, tier, hotspot);
        sessions.put(player.getUniqueId(), session);

        session.task = player.getScheduler().runAtFixedRate(
                plugin,
                task -> tick(player, session),
                () -> dispose(player, session),
                1L,
                1L
        );
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
            return;
        }

        end(player, session);

        if (state == FishingMinigame.State.CAUGHT) {
            reward(player, session);
        } else {
            player.sendActionBar(Component.text("The fish got away...", NamedTextColor.RED));
        }
        removeHook(session.hook);
    }

    private void end(Player player, Session session) {
        session.task.cancel();
        dispose(player, session);
    }

    private void dispose(Player player, Session session) {
        sessions.remove(player.getUniqueId(), session);
        session.display.remove();
    }

    private void reward(Player player, Session session) {
        Location hookLocation = session.hook.getLocation();
        Vector pull = player.getLocation().toVector().subtract(hookLocation.toVector()).multiply(PULL_STRENGTH).setY(PULL_LIFT);

        for (ItemStack stack : rollCatch(player, session)) {
            hookLocation.getWorld().dropItem(hookLocation, stack, item -> item.setVelocity(pull));
        }
        player.sendActionBar(Component.text("You caught something!", NamedTextColor.GREEN));
    }

    private FishTier rollTier(@Nullable FishingHotspot hotspot) {
        FishTier[] tiers = FishTier.values();
        double[] weights = new double[tiers.length];
        double totalWeight = 0;
        for (int i = 0; i < tiers.length; i++) {
            weights[i] = tiers[i].weight() * (hotspot == null ? 1.0 : hotspot.tierMultiplier(tiers[i].id()));
            totalWeight += weights[i];
        }

        double roll = ThreadLocalRandom.current().nextDouble(totalWeight);
        for (int i = 0; i < tiers.length; i++) {
            roll -= weights[i];
            if (roll < 0) return tiers[i];
        }
        return FishTier.COMMON;
    }

    private List<ItemStack> rollCatch(Player player, Session session) {
        FishingHotspot hotspot = session.hotspot;
        double luckBonus = hotspot == null ? 0.0 : hotspot.luckBonus();

        LootContext context = LootContext.builder(PlayerManager.getInstance().getSmpPlayer(player))
                .origin(LootOrigin.FISHING, session.hook)
                .luck(player.getAttribute(Attribute.LUCK).getValue() / 100.0 + luckBonus)
                .build();

        List<Holder<LootTable>> tables = new ArrayList<>();
        boolean replaced = hotspot != null && hotspot.lootMode() == FishingHotspot.LootMode.REPLACE && !hotspot.lootTables().isEmpty();
        if (!replaced) tables.add(session.tier.lootTable());
        if (hotspot != null) tables.addAll(hotspot.lootTables());

        List<ItemStack> results = new ArrayList<>();
        for (Holder<LootTable> table : tables) {
            if (!table.isBound()) {
                RogueSmpCore.LOGGER.warn("[Fishing] Loot table not found: '" + table.getId() + "'");
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
        private final @Nullable FishingHotspot hotspot;
        private ScheduledTask task;

        private Session(FishingDisplay display, FishHook hook, FishingMinigame game, FishTier tier, @Nullable FishingHotspot hotspot) {
            this.tier = tier;
            this.hotspot = hotspot;
            this.display = display;
            this.hook = hook;
            this.game = game;
        }
    }
}
