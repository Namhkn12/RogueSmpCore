package com.roguesmp.dungeon.feature;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.codec.Codec;
import com.roguesmp.constant.Keys;
import com.roguesmp.dungeon.DungeonFeature;
import com.roguesmp.dungeon.DungeonManager;
import com.roguesmp.dungeon.DungeonEvent;
import com.roguesmp.dungeon.FeatureContext;
import com.roguesmp.dungeon.room.RoomDefinition;
import com.roguesmp.dungeon.room.RoomEffect;
import com.roguesmp.dungeon.room.RoomEffectContext;
import com.roguesmp.dungeon.room.RoomObjective;
import com.roguesmp.dungeon.room.RoomSchematics;
import com.roguesmp.dungeon.room.RoomType;
import com.roguesmp.spawner.SpawnerSystem;
import com.roguesmp.entity.EntityManager;
import com.roguesmp.gui.dungeon.BossTriggerGui;
import com.roguesmp.gui.dungeon.RoomChoiceGui;
import com.roguesmp.registry.Registries;
import com.roguesmp.utils.Utils;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.TrialSpawner;
import org.bukkit.block.Vault;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public record RoomsFeature(List<Pool> pools, int minimumRooms, int maxChoices) implements DungeonFeature<RoomsFeature.State> {

    public static final String TYPE_KEY = "rooms";
    public static final String ROOM_STARTED = "room_started";
    public static final String ROOM_CLEARED = "room_cleared";
    public static final String BOSS_DEFEATED = "boss_defeated";

    private static final long DOOR_CLOSE_DELAY_TICKS = 100L;

    public record Entry(String room, double weight) {
        public static final Codec<Entry> CODEC = Codec.composite(
                Codec.STRING.fieldOf("room").forGetter(Entry::room),
                Codec.DOUBLE.optionalFieldOf("weight", 1.0).forGetter(Entry::weight),
                Entry::new
        );
    }

    public record Pool(int min, int max, List<Entry> rooms) {
        public static final Codec<Pool> CODEC = Codec.composite(
                Codec.INT.optionalFieldOf("min", 1).forGetter(Pool::min),
                Codec.INT.optionalFieldOf("max", 1).forGetter(Pool::max),
                Codec.listOf(Entry.CODEC).fieldOf("rooms").forGetter(Pool::rooms),
                Pool::new
        );
    }

    public static final class State {
        private static final Codec<List<Double>> DOUBLES = Codec.listOf(Codec.DOUBLE);
        private static final Codec<List<Integer>> INTS = Codec.listOf(Codec.INT);

        public static final Codec<State> CODEC = Codec.composite(
                Codec.listOf(Codec.STRING).optionalFieldOf("pool", List.of()).forGetter(state -> state.pool),
                Codec.INT.optionalFieldOf("cleared", 0).forGetter(state -> state.cleared),
                Codec.INT.optionalFieldOf("score", 0).forGetter(state -> state.score),
                Codec.BOOLEAN.optionalFieldOf("boss_defeated", false).forGetter(state -> state.bossDefeated),
                Codec.STRING.optionalFieldOf("room", "").forGetter(state -> state.room),
                DOUBLES.optionalFieldOf("bounds", List.of()).forGetter(state -> state.bounds),
                INTS.optionalFieldOf("door", List.of()).forGetter(state -> state.door),
                Codec.BOOLEAN.optionalFieldOf("room_completed", true).forGetter(state -> state.roomCompleted),
                Codec.unboundedMap(Codec.INT).optionalFieldOf("progress", Map.of()).forGetter(state -> state.progress),
                Codec.BOOLEAN.optionalFieldOf("boss_spawned", false).forGetter(state -> state.bossSpawned),
                Codec.listOf(Codec.STRING).optionalFieldOf("next_rooms", List.of()).forGetter(state -> state.nextRooms),
                State::new
        );

        private final List<String> pool = new ArrayList<>();
        private int cleared;
        private int score;
        private boolean bossDefeated;
        private String room;
        private List<Double> bounds;
        private List<Integer> door;
        private boolean roomCompleted;
        private final Map<String, Integer> progress = new HashMap<>();
        private boolean bossSpawned;
        private final List<String> nextRooms = new ArrayList<>();
        private @Nullable List<Object> effectStates;
        private int roomSeconds;

        public State(List<String> pool, int cleared, int score, boolean bossDefeated, String room, List<Double> bounds, List<Integer> door,
                     boolean roomCompleted, Map<String, Integer> progress, boolean bossSpawned, List<String> nextRooms) {
            this.pool.addAll(pool);
            this.cleared = cleared;
            this.score = score;
            this.bossDefeated = bossDefeated;
            this.room = room;
            this.bounds = List.copyOf(bounds);
            this.door = List.copyOf(door);
            this.roomCompleted = roomCompleted;
            this.progress.putAll(progress);
            this.bossSpawned = bossSpawned;
            this.nextRooms.addAll(nextRooms);
        }

        public int cleared() {
            return cleared;
        }

        public int score() {
            return score;
        }

        public boolean bossDefeated() {
            return bossDefeated;
        }

        public @Nullable String room() {
            return room.isEmpty() ? null : room;
        }

        public boolean roomCompleted() {
            return roomCompleted;
        }

        public int progress(int objectiveIndex) {
            return progress.getOrDefault(String.valueOf(objectiveIndex), 0);
        }

        private @Nullable BoundingBox boundingBox() {
            return bounds.size() == 6 ? new BoundingBox(bounds.get(0), bounds.get(1), bounds.get(2), bounds.get(3), bounds.get(4), bounds.get(5)) : null;
        }
    }

    public static final Codec<RoomsFeature> CODEC = Codec.composite(
            Codec.listOf(Pool.CODEC).fieldOf("pools").forGetter(RoomsFeature::pools),
            Codec.INT.optionalFieldOf("minimum_rooms", 3).forGetter(RoomsFeature::minimumRooms),
            Codec.INT.optionalFieldOf("max_choices", 3).forGetter(RoomsFeature::maxChoices),
            RoomsFeature::new
    );

    public static List<String> progressLines(State state) {
        List<String> lines = new ArrayList<>();
        RoomDefinition room = state.room() == null ? null : Registries.DUNGEON_ROOM.get(state.room);
        if (state.bossDefeated) {
            lines.add("<green>✔ Boss đã bị đánh bại");
        } else if (room == null) {
            lines.add("<gray>Phòng: <white>Khởi đầu");
        } else {
            lines.add("<gray>Phòng: <white>" + (room.name().isEmpty() ? state.room : room.name()) + (state.roomCompleted ? " <green>✔" : ""));
            for (int i = 0; i < room.objectives().size(); i++) {
                RoomObjective objective = room.objectives().get(i);
                String label = objective.display().isEmpty() ? "Mục tiêu " + (i + 1) : objective.display();
                int done = state.progress(i);
                lines.add(done >= objective.count()
                        ? "<green>✔ " + label
                        : "<gray>" + label + " <white>" + done + "<gray>/<white>" + objective.count());
            }
        }
        lines.add("<gray>Điểm: <gold>" + state.score + " <gray>| Đã qua: <white>" + state.cleared);
        return lines;
    }

    @Override
    public String getTypeId() {
        return TYPE_KEY;
    }

    @Override
    public State createState() {
        return new State(List.of(), 0, 0, false, "", List.of(), List.of(), true, Map.of(), false, List.of());
    }

    @Override
    public Codec<State> stateCodec() {
        return State.CODEC;
    }

    @Override
    public void onStart(FeatureContext<State> context) {
        context.state().pool.addAll(rollPool());
    }

    @Override
    public void onEvent(FeatureContext<State> context, DungeonEvent event) {
        if (event instanceof DungeonEvent.BlockInteracted interacted) {
            if (interacted.block().getState() instanceof Vault) {
                interacted.event().setCancelled(true);
                openDoor(context, interacted.player(), interacted.block());
                return;
            }
            if (interacted.block().getState() instanceof TrialSpawner) {
                interacted.event().setCancelled(true);
                openBossTrigger(context, interacted.player(), interacted.block());
                return;
            }
        }
        progressObjectives(context, event);
    }

    private void openDoor(FeatureContext<State> context, Player player, Block door) {
        State state = context.state();
        if (state.bossDefeated) return;
        if (!state.roomCompleted) {
            player.sendMessage(Utils.fromString("<red>Hãy hoàn thành phòng hiện tại trước."));
            return;
        }
        if (state.nextRooms.isEmpty()) {
            state.nextRooms.addAll(rollNextRooms(state));
            context.save();
        }
        if (state.nextRooms.isEmpty()) {
            player.sendMessage(Utils.fromString("<gray>Không còn phòng nào phía sau cánh cửa này."));
            return;
        }
        new RoomChoiceGui(door, List.copyOf(state.nextRooms), (chooser, roomId) -> {
            if (isLive(context) && context.state().nextRooms.contains(roomId)) enterRoom(context, door, roomId);
        }).showInventory(player);
    }

    private void enterRoom(FeatureContext<State> context, Block door, String roomId) {
        RoomDefinition room = Registries.DUNGEON_ROOM.get(roomId);
        if (room == null) {
            RogueSmpCore.LOGGER.error("Dungeon {} refers to unknown room {}", context.instance().dungeonId(), roomId);
            return;
        }

        Location doorLocation = door.getLocation();
        BoundingBox bounds;
        try {
            bounds = RoomSchematics.paste(room.schematic(), doorLocation.clone().add(0, -2, 0));
        } catch (IllegalStateException e) {
            RogueSmpCore.LOGGER.error("Failed to paste room {} in dungeon run {}", roomId, context.instance().id(), e);
            return;
        }

        State state = context.state();
        state.pool.remove(roomId);
        state.room = roomId;
        state.bounds = List.of(bounds.getMinX(), bounds.getMinY(), bounds.getMinZ(), bounds.getMaxX(), bounds.getMaxY(), bounds.getMaxZ());
        state.door = List.of(doorLocation.getBlockX(), doorLocation.getBlockY(), doorLocation.getBlockZ());
        state.roomCompleted = room.objectives().isEmpty();
        state.progress.clear();
        state.bossSpawned = false;
        state.nextRooms.clear();
        context.save();

        setDoor(doorLocation, Material.AIR);
        startEffects(context, room);
        context.signal(ROOM_STARTED, 1, null, Map.of("room", roomId, "type", room.type().name().toLowerCase()));

        if (state.roomCompleted) {
            completeRoom(context, room);
        } else if (room.type().isCombat()) {
            Utils.runLater(() -> {
                if (!isLive(context) || !roomId.equals(context.state().room) || context.state().roomCompleted) return;
                for (Player player : context.onlinePlayers()) {
                    if (!bounds.contains(player.getLocation().toVector())) player.teleport(doorLocation.clone().add(0.5, 0, -1.5));
                }
                setDoor(doorLocation, Material.BEDROCK);
            }, (int) DOOR_CLOSE_DELAY_TICKS);
        }
    }

    private void openBossTrigger(FeatureContext<State> context, Player player, Block block) {
        State state = context.state();
        RoomDefinition room = state.room() == null ? null : Registries.DUNGEON_ROOM.get(state.room);
        if (room == null || room.type() != RoomType.BOSS || room.boss().isEmpty() || state.bossSpawned) return;

        new BossTriggerGui(block, summoner -> {
            if (!isLive(context) || context.state().bossSpawned) return;
            context.state().bossSpawned = true;
            context.save();
            EntityManager.getInstance().spawnEntity(room.boss(), block.getLocation().add(0.5, 3, 0.5));
            block.setType(Material.SPAWNER);
        }).showInventory(player);
    }

    private void progressObjectives(FeatureContext<State> context, DungeonEvent event) {
        State state = context.state();
        if (state.roomCompleted || state.room() == null) return;
        RoomDefinition room = Registries.DUNGEON_ROOM.get(state.room);
        if (room == null) return;

        Location location = event.location();
        BoundingBox bounds = state.boundingBox();
        if (location != null && bounds != null && !bounds.contains(location.toVector())) return;

        boolean changed = false;
        List<RoomObjective> objectives = room.objectives();
        for (int i = 0; i < objectives.size(); i++) {
            RoomObjective objective = objectives.get(i);
            int done = state.progress(i);
            if (done >= objective.count() || !objective.on().matches(context, event)) continue;

            int now = Math.min(objective.count(), done + Math.max(1, event.amount()));
            state.progress.put(String.valueOf(i), now);
            changed = true;
            if (now < objective.count()) continue;

            state.score += objective.score();
            if (objective.endsDungeon() && !state.bossDefeated) {
                state.bossDefeated = true;
                context.signal(BOSS_DEFEATED, 1, event.player());
            }
        }
        if (!changed) return;

        boolean allDone = true;
        for (int i = 0; i < objectives.size(); i++) {
            if (state.progress(i) < objectives.get(i).count()) allDone = false;
        }
        if (allDone) completeRoom(context, room);
        else context.save();
    }

    private void completeRoom(FeatureContext<State> context, RoomDefinition room) {
        State state = context.state();
        endEffects(context, room);
        String roomId = state.room;
        state.roomCompleted = true;
        state.cleared++;
        context.save();

        if (state.door.size() == 3) {
            World world = context.world();
            if (world != null) setDoor(new Location(world, state.door.get(0), state.door.get(1), state.door.get(2)), Material.AIR);
        }
        clearRoom(context);
        context.onlinePlayers().forEach(player -> player.sendMessage(Utils.fromString("<green>Phòng đã được dọn sạch!")));
        context.signal(ROOM_CLEARED, 1, null, Map.of("room", roomId, "type", room.type().name().toLowerCase()));
    }

    private void clearRoom(FeatureContext<State> context) {
        World world = context.world();
        BoundingBox bounds = context.state().boundingBox();
        if (world == null || bounds == null) return;

        for (Entity entity : world.getNearbyEntities(bounds)) {
            if (entity instanceof Player || entity instanceof Item || entity instanceof ExperienceOrb) continue;
            entity.remove();
        }

        Set<Chunk> chunks = new LinkedHashSet<>();
        for (int x = (int) bounds.getMinX() >> 4; x <= (int) bounds.getMaxX() >> 4; x++) {
            for (int z = (int) bounds.getMinZ() >> 4; z <= (int) bounds.getMaxZ() >> 4; z++) {
                if (world.isChunkLoaded(x, z)) chunks.add(world.getChunkAt(x, z));
            }
        }
        for (Chunk chunk : chunks) {
            for (BlockState tile : chunk.getTileEntities()) {
                if (!(tile instanceof CreatureSpawner spawner) || !bounds.contains(tile.getLocation().toVector())) continue;
                if (spawner.getPersistentDataContainer().has(Keys.SPAWNER_TID, PersistentDataType.STRING)) SpawnerSystem.getInstance().disarm(spawner);
            }
        }
    }

    @Override
    public void onTick(FeatureContext<State> context) {
        State state = context.state();
        RoomDefinition room = state.room() == null ? null : Registries.DUNGEON_ROOM.get(state.room);
        BoundingBox bounds = state.boundingBox();
        if (state.roomCompleted || room == null || bounds == null || room.effects().isEmpty() || context.world() == null) return;

        if (state.effectStates == null) state.effectStates = createEffectStates(room);
        state.roomSeconds++;
        forEachEffect(context, room, RoomEffect::onTick);
    }

    @Override
    public void onEnd(FeatureContext<State> context) {
        State state = context.state();
        RoomDefinition room = state.room() == null ? null : Registries.DUNGEON_ROOM.get(state.room);
        if (room != null && !state.roomCompleted) endEffects(context, room);
    }

    private void startEffects(FeatureContext<State> context, RoomDefinition room) {
        State state = context.state();
        state.effectStates = createEffectStates(room);
        state.roomSeconds = 0;
        forEachEffect(context, room, RoomEffect::onStart);
    }

    private void endEffects(FeatureContext<State> context, RoomDefinition room) {
        State state = context.state();
        if (state.effectStates != null) forEachEffect(context, room, RoomEffect::onEnd);
        state.effectStates = null;
    }

    private static List<Object> createEffectStates(RoomDefinition room) {
        List<Object> states = new ArrayList<>();
        room.effects().forEach(effect -> states.add(effect.createState()));
        return states;
    }

    @SuppressWarnings("unchecked")
    private void forEachEffect(FeatureContext<State> context, RoomDefinition room, BiConsumer<RoomEffect<Object>, RoomEffectContext<Object>> hook) {
        State state = context.state();
        BoundingBox bounds = state.boundingBox();
        if (bounds == null || state.effectStates == null) return;
        for (int i = 0; i < room.effects().size() && i < state.effectStates.size(); i++) {
            RoomEffect<Object> effect = (RoomEffect<Object>) room.effects().get(i);
            try {
                hook.accept(effect, new RoomEffectContext<>(context, bounds, state.roomSeconds, state.effectStates.get(i)));
            } catch (Exception e) {
                RogueSmpCore.LOGGER.error("Room effect {} of room {} failed in dungeon run {}", effect.getTypeId(), state.room, context.instance().id(), e);
            }
        }
    }

    private List<String> rollPool() {
        List<String> result = new ArrayList<>();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (Pool pool : pools) {
            List<Entry> remaining = pool.rooms().stream().filter(entry -> !result.contains(entry.room())).collect(Collectors.toCollection(ArrayList::new));
            int count = Math.min(remaining.size(), pool.min() + random.nextInt(Math.max(1, pool.max() - pool.min() + 1)));
            for (int i = 0; i < count; i++) {
                double total = remaining.stream().mapToDouble(Entry::weight).sum();
                double roll = random.nextDouble() * total;
                for (Entry entry : remaining) {
                    roll -= entry.weight();
                    if (roll < 0) {
                        result.add(entry.room());
                        remaining.remove(entry);
                        break;
                    }
                }
            }
        }
        return result;
    }

    private List<String> rollNextRooms(State state) {
        List<String> eligible = new ArrayList<>(state.pool.stream()
                .distinct()
                .filter(roomId -> {
                    RoomDefinition room = Registries.DUNGEON_ROOM.get(roomId);
                    if (room == null || room.type() == RoomType.SPAWN || room.type() == RoomType.TREASURE) return false;
                    return room.type() != RoomType.BOSS || state.cleared >= minimumRooms;
                })
                .toList());
        Collections.shuffle(eligible);
        int count = ThreadLocalRandom.current().nextInt(1, Math.max(1, maxChoices) + 1);
        return eligible.subList(0, Math.min(count, eligible.size()));
    }

    private static void setDoor(Location middle, Material material) {
        World world = middle.getWorld();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                world.getBlockAt(middle.getBlockX() + dx, middle.getBlockY() + dy, middle.getBlockZ()).setType(material);
            }
        }
    }

    private static boolean isLive(FeatureContext<State> context) {
        return DungeonManager.getInstance().get(context.instance().id()) == context.instance();
    }
}
