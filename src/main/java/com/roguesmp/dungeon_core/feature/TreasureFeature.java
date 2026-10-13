package com.roguesmp.dungeon_core.feature;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.codec.Codec;
import com.roguesmp.dungeon_core.DungeonFeature;
import com.roguesmp.dungeon_core.DungeonSignal;
import com.roguesmp.dungeon_core.FeatureContext;
import com.roguesmp.dungeon_core.room.RoomDefinition;
import com.roguesmp.dungeon_core.room.RoomSchematics;
import com.roguesmp.registry.Registries;
import com.roguesmp.utils.Utils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Vault;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public record TreasureFeature(List<String> rooms, int extendSeconds, int spacing) implements DungeonFeature<TreasureFeature.State> {

    public static final String TYPE_KEY = "treasure";

    private static final int TELEPORT_DELAY_TICKS = 20;

    public static final class State {
        public static final Codec<State> CODEC = Codec.composite(
                Codec.STRING.optionalFieldOf("room", "").forGetter(state -> state.room),
                Codec.BOOLEAN.optionalFieldOf("portal_open", false).forGetter(state -> state.portalOpen),
                Codec.listOf(Codec.UUID).<Set<UUID>>xmap(HashSet::new, ArrayList::new).optionalFieldOf("entered", Set.of()).forGetter(state -> state.entered),
                State::new
        );

        private String room;
        private boolean portalOpen;
        private final Set<UUID> entered = new HashSet<>();

        public State(String room, boolean portalOpen, Set<UUID> entered) {
            this.room = room;
            this.portalOpen = portalOpen;
            this.entered.addAll(entered);
        }
    }

    public static final Codec<TreasureFeature> CODEC = Codec.composite(
            Codec.listOf(Codec.STRING).fieldOf("rooms").forGetter(TreasureFeature::rooms),
            Codec.INT.optionalFieldOf("extend_seconds", 300).forGetter(TreasureFeature::extendSeconds),
            Codec.INT.optionalFieldOf("spacing", 32).forGetter(TreasureFeature::spacing),
            TreasureFeature::new
    );

    @Override
    public String getTypeId() {
        return TYPE_KEY;
    }

    @Override
    public State createState() {
        return new State("", false, Set.of());
    }

    @Override
    public Codec<State> stateCodec() {
        return State.CODEC;
    }

    @Override
    public void onStart(FeatureContext<State> context) {
        if (!rooms.isEmpty()) context.state().room = rooms.get(ThreadLocalRandom.current().nextInt(rooms.size()));
    }

    @Override
    public void onSignal(FeatureContext<State> context, DungeonSignal signal) {
        switch (signal) {
            case DungeonSignal.Custom custom when custom.id().equals(RoomsFeature.BOSS_DEFEATED) -> onBossDefeated(context);
            case DungeonSignal.BlockInteracted interacted when interacted.block().getState() instanceof Vault && bossDefeated(context) -> {
                interacted.event().setCancelled(true);
                openPortal(context, interacted.block());
            }
            case DungeonSignal.PortalEntered portal when portal.portal() == Material.END_GATEWAY -> enterTreasure(context, portal.player());
            case DungeonSignal.PortalEntered portal when portal.portal() == Material.NETHER_PORTAL -> exit(context, portal.player());
            default -> {
            }
        }
    }

    private void onBossDefeated(FeatureContext<State> context) {
        FeatureContext<ExpiryFeature.State> expiry = context.manager().feature(context.instance(), ExpiryFeature.class);
        if (expiry != null) ExpiryFeature.setExpiresAt(expiry, System.currentTimeMillis() + extendSeconds * 1000L);
        context.onlinePlayers().forEach(player -> player.sendMessage(Utils.fromString(
                "<green><b>Boss đã bị đánh bại!</b> <yellow>Mở cánh cửa để vào kho báu. Còn " + extendSeconds / 60 + " phút.")));
    }

    private void openPortal(FeatureContext<State> context, Block door) {
        State state = context.state();
        if (state.portalOpen) return;
        state.portalOpen = true;
        context.save();

        World world = door.getWorld();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                world.getBlockAt(door.getX() + dx, door.getY() + dy, door.getZ()).setType(Material.END_GATEWAY);
            }
        }
    }

    private void enterTreasure(FeatureContext<State> context, Player player) {
        State state = context.state();
        World world = context.world();
        RoomDefinition room = state.room.isEmpty() ? null : Registries.DUNGEON_ROOM.get(state.room);
        if (world == null || room == null || !state.entered.add(player.getUniqueId())) return;
        context.save();

        Location origin = world.getSpawnLocation().clone().add(0, (double) spacing * state.entered.size(), 0);
        try {
            RoomSchematics.paste(room.schematic(), origin);
        } catch (IllegalStateException e) {
            RogueSmpCore.LOGGER.error("Failed to paste treasure room {} in dungeon run {}", state.room, context.instance().id(), e);
            return;
        }
        Utils.runLater(() -> {
            if (player.isOnline() && player.getWorld().equals(world)) player.teleport(origin.clone().add(0, 1, 0));
        }, TELEPORT_DELAY_TICKS);
    }

    private void exit(FeatureContext<State> context, Player player) {
        if (!bossDefeated(context)) return;
        context.manager().leave(player);
        boolean empty = context.onlinePlayers().stream().allMatch(inside -> inside.equals(player));
        if (empty) context.end("đã được chinh phục");
    }

    private static boolean bossDefeated(FeatureContext<?> context) {
        FeatureContext<RoomsFeature.State> rooms = context.manager().feature(context.instance(), RoomsFeature.class);
        return rooms != null && rooms.state().bossDefeated();
    }
}
