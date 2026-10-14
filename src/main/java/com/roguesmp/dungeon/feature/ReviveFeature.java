package com.roguesmp.dungeon.feature;

import com.roguesmp.codec.Codec;
import com.roguesmp.constant.Keys;
import com.roguesmp.dungeon.DungeonFeature;
import com.roguesmp.dungeon.DungeonEvent;
import com.roguesmp.dungeon.FeatureContext;
import com.roguesmp.utils.Utils;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public record ReviveFeature(double radius, int reviveSeconds, double reviveHealth, boolean reviveOnRoomClear,
                            boolean failOnWipe, int wipeDelaySeconds, double spectatorRadius) implements DungeonFeature<ReviveFeature.State> {

    public static final String TYPE_KEY = "revive";

    public record Downed(double x, double y, double z, float yaw, int progress, @Nullable UUID marker) {
        public static final Codec<Downed> CODEC = Codec.composite(
                Codec.DOUBLE.fieldOf("x").forGetter(Downed::x),
                Codec.DOUBLE.fieldOf("y").forGetter(Downed::y),
                Codec.DOUBLE.fieldOf("z").forGetter(Downed::z),
                Codec.FLOAT.optionalFieldOf("yaw", 0f).forGetter(Downed::yaw),
                Codec.INT.optionalFieldOf("progress", 0).forGetter(Downed::progress),
                Codec.UUID.optionalFieldOf("marker").forGetter(downed -> Optional.ofNullable(downed.marker())),
                (x, y, z, yaw, progress, marker) -> new Downed(x, y, z, yaw, progress, marker.orElse(null))
        );

        Location location(World world) {
            return new Location(world, x, y, z, yaw, 0);
        }

        Downed withProgress(int progress) {
            return new Downed(x, y, z, yaw, progress, marker);
        }

        Downed withMarker(@Nullable UUID marker) {
            return new Downed(x, y, z, yaw, progress, marker);
        }
    }

    public static final class State {
        public static final Codec<State> CODEC = Codec.composite(
                Codec.unboundedMap(Codec.UUID, Downed.CODEC).optionalFieldOf("downed", Map.of()).forGetter(state -> state.downed),
                Codec.LONG.optionalFieldOf("wipe_at", 0L).forGetter(state -> state.wipeAt),
                State::new
        );

        private final Map<UUID, Downed> downed = new HashMap<>();
        private long wipeAt;
        private final Map<UUID, BossBar> bars = new HashMap<>();

        public State(Map<UUID, Downed> downed, long wipeAt) {
            this.downed.putAll(downed);
            this.wipeAt = wipeAt;
        }

        public boolean isDowned(UUID playerId) {
            return downed.containsKey(playerId);
        }
    }

    public static final Codec<ReviveFeature> CODEC = Codec.composite(
            Codec.DOUBLE.optionalFieldOf("radius", 3.0).forGetter(ReviveFeature::radius),
            Codec.INT.optionalFieldOf("revive_seconds", 12).forGetter(ReviveFeature::reviveSeconds),
            Codec.DOUBLE.optionalFieldOf("revive_health", 5.0).forGetter(ReviveFeature::reviveHealth),
            Codec.BOOLEAN.optionalFieldOf("revive_on_room_clear", true).forGetter(ReviveFeature::reviveOnRoomClear),
            Codec.BOOLEAN.optionalFieldOf("fail_on_wipe", true).forGetter(ReviveFeature::failOnWipe),
            Codec.INT.optionalFieldOf("wipe_delay_seconds", 10).forGetter(ReviveFeature::wipeDelaySeconds),
            Codec.DOUBLE.optionalFieldOf("spectator_radius", 16.0).forGetter(ReviveFeature::spectatorRadius),
            ReviveFeature::new
    );

    @Override
    public String getTypeId() {
        return TYPE_KEY;
    }

    @Override
    public State createState() {
        return new State(Map.of(), 0);
    }

    @Override
    public Codec<State> stateCodec() {
        return State.CODEC;
    }

    @Override
    public void onEvent(FeatureContext<State> context, DungeonEvent event) {
        switch (event) {
            case DungeonEvent.PlayerDied died -> down(context, died);
            case DungeonEvent.PlayerEntered entered -> restoreDowned(context, entered.player());
            case DungeonEvent.PlayerLeft left -> context.state().bars.values().forEach(bar -> left.player().hideBossBar(bar));
            case DungeonEvent.Custom custom when reviveOnRoomClear && custom.id().equals(RoomsFeature.ROOM_CLEARED) -> reviveAll(context);
            default -> {
            }
        }
    }

    @Override
    public void onTick(FeatureContext<State> context) {
        State state = context.state();
        World world = context.world();
        if (world == null) return;

        for (Map.Entry<UUID, Downed> entry : List.copyOf(state.downed.entrySet())) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null || !player.getWorld().equals(world)) continue;
            Downed downed = entry.getValue();
            Location spot = downed.location(world);

            if (player.getLocation().distanceSquared(spot) > spectatorRadius * spectatorRadius) player.teleport(spot);
            if (downed.marker() == null || world.getEntity(downed.marker()) == null) {
                downed = downed.withMarker(spawnMarker(player, spot));
            }

            List<Player> rescuers = world.getPlayers().stream()
                    .filter(rescuer -> !rescuer.equals(player) && !state.isDowned(rescuer.getUniqueId()))
                    .filter(rescuer -> rescuer.getGameMode() != GameMode.SPECTATOR && rescuer.isSneaking())
                    .filter(rescuer -> rescuer.getLocation().distanceSquared(spot) <= radius * radius)
                    .toList();
            int progress = rescuers.isEmpty() ? Math.max(0, downed.progress() - 1) : downed.progress() + 1;
            state.downed.put(entry.getKey(), downed.withProgress(progress));

            if (progress >= reviveSeconds) {
                revive(context, player.getUniqueId());
                continue;
            }
            updateBar(context, player, rescuers, progress);
        }

        if (state.wipeAt > 0 && System.currentTimeMillis() >= state.wipeAt) context.end("đã sụp đổ");
    }

    @Override
    public void onEnd(FeatureContext<State> context) {
        State state = context.state();
        for (UUID playerId : List.copyOf(state.downed.keySet())) {
            removeMarker(context, state.downed.get(playerId));
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) player.setGameMode(GameMode.SURVIVAL);
        }
        state.bars.values().forEach(bar -> Bukkit.getOnlinePlayers().forEach(player -> player.hideBossBar(bar)));
        state.bars.clear();
        state.downed.clear();
    }

    private void down(FeatureContext<State> context, DungeonEvent.PlayerDied died) {
        Player player = died.player();
        State state = context.state();
        died.event().setCancelled(true);
        if (state.isDowned(player.getUniqueId())) return;

        Location spot = player.getLocation();
        double maxHealth = player.getAttribute(Attribute.MAX_HEALTH) != null ? player.getAttribute(Attribute.MAX_HEALTH).getValue() : 20;
        player.setHealth(maxHealth);
        player.setFireTicks(0);
        player.setGameMode(GameMode.SPECTATOR);
        state.downed.put(player.getUniqueId(), new Downed(spot.getX(), spot.getY(), spot.getZ(), spot.getYaw(), 0, spawnMarker(player, spot)));

        player.showTitle(Title.title(Utils.fromString("<red><bold>BẠN ĐÃ GỤC NGÃ"), Utils.fromString("<gray>Chờ đồng đội hồi sinh..."),
                Title.Times.times(Duration.ofMillis(250), Duration.ofSeconds(3), Duration.ofMillis(500))));
        spot.getWorld().spawnParticle(Particle.SCULK_SOUL, spot, 30, 0.4, 0.6, 0.4);
        spot.getWorld().playSound(spot, Sound.ENTITY_WARDEN_DEATH, 1f, 0.8f);
        String coords = "[" + spot.getBlockX() + ", " + spot.getBlockY() + ", " + spot.getBlockZ() + "]";
        for (Player teammate : context.onlinePlayers()) {
            if (teammate.equals(player)) continue;
            teammate.sendMessage(Utils.fromString("<red>" + player.getName() + " đã gục ngã tại " + coords + ". Ngồi xuống cạnh họ để hồi sinh!"));
            teammate.playSound(teammate.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1f, 1f);
        }

        checkWipe(context);
        context.save();
    }

    private void restoreDowned(FeatureContext<State> context, Player player) {
        Downed downed = context.state().downed.get(player.getUniqueId());
        World world = context.world();
        if (downed == null || world == null) return;
        player.setGameMode(GameMode.SPECTATOR);
        player.teleport(downed.location(world));
    }

    private void checkWipe(FeatureContext<State> context) {
        State state = context.state();
        List<Player> inside = context.onlinePlayers();
        if (!failOnWipe || inside.isEmpty() || state.wipeAt > 0) return;
        if (!inside.stream().allMatch(player -> state.isDowned(player.getUniqueId()))) return;

        state.wipeAt = System.currentTimeMillis() + wipeDelaySeconds * 1000L;
        inside.forEach(player -> player.sendMessage(Utils.fromString("<red>Cả đội đã gục ngã! Phó bản sẽ sụp đổ sau " + wipeDelaySeconds + " giây.")));
    }

    private void reviveAll(FeatureContext<State> context) {
        List.copyOf(context.state().downed.keySet()).forEach(playerId -> revive(context, playerId));
    }

    private void revive(FeatureContext<State> context, UUID playerId) {
        State state = context.state();
        Downed downed = state.downed.remove(playerId);
        if (downed == null) return;
        state.wipeAt = 0;
        removeMarker(context, downed);
        BossBar bar = state.bars.remove(playerId);
        if (bar != null) Bukkit.getOnlinePlayers().forEach(viewer -> viewer.hideBossBar(bar));
        context.save();

        Player player = Bukkit.getPlayer(playerId);
        World world = context.world();
        if (player == null || world == null || !player.getWorld().equals(world)) return;
        Location spot = downed.location(world);
        player.teleport(spot);
        player.setGameMode(GameMode.SURVIVAL);
        player.setHealth(Math.min(reviveHealth, player.getHealth()));
        player.showTitle(Title.title(Utils.fromString("<gold>Đã hồi sinh"), Utils.fromString("<yellow>Linh hồn bạn đã trở lại")));
        world.spawnParticle(Particle.TOTEM_OF_UNDYING, spot, 80, 0.5, 1, 0.5);
        world.playSound(spot, Sound.ITEM_TOTEM_USE, 1f, 0.8f);
    }

    private void updateBar(FeatureContext<State> context, Player downedPlayer, List<Player> rescuers, int progress) {
        BossBar bar = context.state().bars.computeIfAbsent(downedPlayer.getUniqueId(),
                id -> BossBar.bossBar(Utils.fromString("<red>Chờ hồi sinh..."), 0f, BossBar.Color.RED, BossBar.Overlay.PROGRESS));
        bar.progress(Math.clamp((float) progress / reviveSeconds, 0f, 1f));
        if (rescuers.isEmpty()) {
            bar.name(Utils.fromString("<red>Chờ hồi sinh..."));
            bar.color(BossBar.Color.RED);
        } else {
            bar.name(Utils.fromString("<green>Đang hồi sinh " + downedPlayer.getName() + " — " + rescuers.size() + " người"));
            bar.color(BossBar.Color.GREEN);
        }

        List<Player> viewers = new ArrayList<>(rescuers);
        viewers.add(downedPlayer);
        for (Player player : context.onlinePlayers()) {
            if (viewers.contains(player)) player.showBossBar(bar);
            else player.hideBossBar(bar);
        }
    }

    private static @Nullable UUID spawnMarker(Player player, Location spot) {
        EntityEquipment equipment = player.getEquipment();
        Mannequin marker = spot.getWorld().spawn(spot.clone().add(0, 0.2, 0), Mannequin.class, mannequin -> {
            mannequin.setProfile(ResolvableProfile.resolvableProfile(player.getPlayerProfile()));
            mannequin.setPose(Pose.STANDING);
            mannequin.setGlowing(true);
            mannequin.setGravity(false);
            mannequin.setSilent(true);
            mannequin.setInvulnerable(true);
            mannequin.setCollidable(false);
            mannequin.setAI(false);
            mannequin.setNoPhysics(true);
            mannequin.setImmovable(true);
            mannequin.setRemoveWhenFarAway(false);
            mannequin.setRotation(spot.getYaw(), 0);
            EntityEquipment copy = mannequin.getEquipment();
            copy.setHelmet(clone(equipment.getHelmet()));
            copy.setChestplate(clone(equipment.getChestplate()));
            copy.setLeggings(clone(equipment.getLeggings()));
            copy.setBoots(clone(equipment.getBoots()));
            copy.setItemInMainHand(clone(equipment.getItemInMainHand()));
            copy.setItemInOffHand(clone(equipment.getItemInOffHand()));
            mannequin.getPersistentDataContainer().set(Keys.REVIVE_POINT, PersistentDataType.BYTE, (byte) 1);
        });
        return marker.getUniqueId();
    }

    private static void removeMarker(FeatureContext<State> context, @Nullable Downed downed) {
        World world = context.world();
        if (downed == null || downed.marker() == null || world == null) return;
        Entity marker = world.getEntity(downed.marker());
        if (marker != null) marker.remove();
    }

    private static @Nullable ItemStack clone(@Nullable ItemStack item) {
        return item == null || item.getType().isAir() ? null : item.clone();
    }
}
