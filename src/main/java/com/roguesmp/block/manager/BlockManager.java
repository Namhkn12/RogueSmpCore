package com.roguesmp.block.manager;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.roguesmp.RogueSmpCore;
import com.roguesmp.block.BlockPos;
import com.roguesmp.block.BlockType;
import com.roguesmp.block.BlockVisual;
import com.roguesmp.block.SmpBlock;
import com.roguesmp.block.BlockTypes;
import com.roguesmp.block.Tickable;
import com.roguesmp.block.event.SmpBlockBreakEvent;
import com.roguesmp.block.event.SmpBlockPlaceEvent;
import com.roguesmp.block.mining.MiningManager;
import com.roguesmp.block.persistence.BlockDataRepository;
import com.roguesmp.codec.DataResult;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.*;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;

public class BlockManager {
    private static BlockManager INSTANCE = null;
    private final Map<BlockPos, SmpBlock> blocks = new HashMap<>();
    private final Set<Tickable> tickingBlocks = new HashSet<>();
    private final RogueSmpCore plugin;
    private final BlockDataRepository repository;
    private final MiningManager mining;
    private final int FLUSH_PERIOD = 1200;
    private final ArrayDeque<SmpBlock> flushQueue = new ArrayDeque<>();
    private int flushTick = 0;
    private int flushBatchSize = 1;

    public BlockManager(RogueSmpCore plugin){
        this.plugin = plugin;

        plugin.getDataFolder().mkdirs();
        this.repository = new BlockDataRepository(new File(plugin.getDataFolder(), "blocks.db").toPath());
        this.mining = new MiningManager(this, plugin);

        plugin.getServer().getScheduler().runTaskTimer(plugin, this::flushSlice, 1L, 1L);
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::tickBlocks, 1L, 1L);
    }

    private void tickBlocks(){
        for(Tickable block : tickingBlocks){
            block.tick();
        }
    }

    private void registerIfTickable(SmpBlock block){
        if(block instanceof Tickable tickable) tickingBlocks.add(tickable);
    }

    public boolean place(Player player, EquipmentSlot hand, Block clicked, BlockFace face, String blockId){
        Block target = clicked.isReplaceable() ? clicked : clicked.getRelative(face);
        if(!target.isReplaceable() || blocks.containsKey(BlockPos.of(target)) || isOccupied(target)) return false;

        SmpBlock block = BlockTypes.create(blockId);
        if(block == null) return false;

        ItemStack inHand = player.getInventory().getItem(hand);
        BlockState replaced = target.getState(false);
        target.setType(Material.BARRIER, false);

        BlockPlaceEvent event = new BlockPlaceEvent(target, replaced, clicked, inHand, player, true, hand);
        Bukkit.getPluginManager().callEvent(event);
        if(event.isCancelled() || !event.canBuild()){
            replaced.update(true, false);
            return false;
        }

        BlockType<?> type = block.getType();

        SmpBlockPlaceEvent smpEvent = new SmpBlockPlaceEvent(player, type, target);
        Bukkit.getPluginManager().callEvent(smpEvent);
        if(smpEvent.isCancelled()){
            replaced.update(true, false);
            return false;
        }

        BlockPos pos = BlockPos.of(target);
        Entity display = BlockVisual.spawn(pos, type, facingYaw(player));
        block.bind(pos, display.getUniqueId());
        block.onPlaced(player, inHand);
        block.markDirty();
        block.markHydrated();
        blocks.put(pos, block);
        registerIfTickable(block);
        Utils.runLater(() -> forcePhysicsUpdate(pos, block));

        if(player.getGameMode() != GameMode.CREATIVE){
            inHand.setAmount(inHand.getAmount() - 1);
            player.getInventory().setItem(hand, inHand);
        }
//        Sound is also played client-side, so it's best to use a vanilla item that is not a placeable block to prevent double sound.
        target.getWorld().playSound(target.getLocation().toCenterLocation(), type.placeSound().asString(), SoundCategory.BLOCKS, 1f, 0.8f);
        player.swingHand(hand);
        return true;
    }

    private void forcePhysicsUpdate(BlockPos pos, SmpBlock block){
        if(blocks.get(pos) != block) return;

        Block bukkitBlock = pos.getBlock();
        if(bukkitBlock == null || bukkitBlock.getType() != Material.BARRIER) return;

        bukkitBlock.setType(Material.BARRIER, true);
    }

    public boolean breakBlock(Block block, Player player){
        BlockBreakEvent event = new BlockBreakEvent(block, player);
        Bukkit.getPluginManager().callEvent(event);
        if(event.isCancelled()) return false;

        block.setType(Material.AIR, true);
        return true;
    }

    public void destroy(BlockBreakEvent event){
        SmpBlock block = blocks.get(BlockPos.of(event.getBlock()));
        if(block == null) return;
        SmpPlayer smpPlayer = PlayerManager.getInstance().getSmpPlayer(event.getPlayer());
        if (smpPlayer == null) return;
        boolean shouldDrop = event.isDropItems() && event.getPlayer().getGameMode() != GameMode.CREATIVE;
        List<ItemStack> drops = shouldDrop ? new ArrayList<>(block.getDrops(smpPlayer)) : new ArrayList<>();
        int experience = shouldDrop ? block.getExperience(smpPlayer) : 0;

        SmpBlockBreakEvent smpEvent = new SmpBlockBreakEvent(block, smpPlayer, event, drops, experience);
        Bukkit.getPluginManager().callEvent(smpEvent);
        if(smpEvent.isCancelled()){
            event.setCancelled(true);
            return;
        }

        block.onBlockBreak(smpEvent);
        block.onUnload();

        Location location = event.getBlock().getLocation();
        for (ItemStack drop : smpEvent.getDrops()) {
            location.getWorld().dropItemNaturally(location.toCenterLocation(), drop);
        }

        int experienceDropped = smpEvent.getExperience();
        if (experienceDropped > 0) {
            location.getWorld().spawn(location.toCenterLocation(), ExperienceOrb.class, orb -> orb.setExperience(experience));
        }

        Entity display = Bukkit.getEntity(block.getDisplayId());
        playBreakEffects(block);
        remove(block, display);
    }

    private void remove(SmpBlock block, @Nullable Entity display){
        BlockPos pos = block.getPos();
        blocks.remove(pos);
        if (block instanceof Tickable) tickingBlocks.remove(block);
        mining.cancelAt(pos);
        if(block.isStateful()) repository.delete(pos);

        if(display != null) display.remove();
    }

    /**
     * Small burst of item particles hugging the face the player is looking at, mirroring vanilla's
     * own per-tick dig particles.
     */
    public void spawnMiningParticles(SmpBlock block, BlockFace face){
        Location center = block.getLocation().toCenterLocation();
        World world = center.getWorld();

        Location origin = center.clone().add(face.getModX() * 0.5, face.getModY() * 0.5, face.getModZ() * 0.5);
        double spreadX = face.getModX() == 0 ? 0.2 : 0.03;
        double spreadY = face.getModY() == 0 ? 0.2 : 0.03;
        double spreadZ = face.getModZ() == 0 ? 0.2 : 0.03;

        world.spawnParticle(Particle.ITEM, origin, 1, spreadX, spreadY, spreadZ, 0f, BlockVisual.displayStack(block));
    }

    private void playBreakEffects(SmpBlock block){
        BlockType<?> type = block.getType();
        Location center = block.getLocation().toCenterLocation();
        World world = center.getWorld();
        ItemStack stack = BlockVisual.displayStack(block);
        world.playSound(center, type.breakSound().asString(), SoundCategory.BLOCKS, 1f, 1f);
        world.spawnParticle(Particle.ITEM, center, 24, 0.25, 0.25, 0.25, 0.05, stack);
    }

    private boolean isOccupied(Block target){
        return !target.getWorld().getNearbyEntities(BoundingBox.of(target), entity ->
                entity instanceof LivingEntity living && !(living instanceof Player p && p.getGameMode() == GameMode.SPECTATOR)
        ).isEmpty();
    }

    private float facingYaw(Player player){
        return (Math.round(player.getLocation().getYaw() / 90f) * 90f + 180f) % 360f;
    }

    public void onEntitiesLoad(Collection<Entity> entities){
        for(Entity entity : entities){
            BlockVisual.Marker marker = BlockVisual.read(entity);
            if(marker != null) attach(marker, entity);
        }
    }

    public void onEntitiesUnload(Collection<Entity> entities){
        Map<BlockPos, String> changed = null;
        for(Entity entity : entities){
            BlockVisual.Marker marker = BlockVisual.read(entity);
            if(marker == null) continue;

            if(changed == null) changed = new HashMap<>();
            detach(marker.pos(), entity, changed);
        }
        if(changed != null) repository.saveAll(changed);
    }

    public void sweepLoadedChunks(){
        for(World world : Bukkit.getWorlds()){
            for(Chunk chunk : world.getLoadedChunks()){
                if(chunk.isEntitiesLoaded()) onEntitiesLoad(List.of(chunk.getEntities()));
            }
        }
    }

    private void attach(BlockVisual.Marker marker, Entity display){
        SmpBlock existing = blocks.get(marker.pos());
        if(existing != null){
            if(!existing.getDisplayId().equals(display.getUniqueId())){
                RogueSmpCore.LOGGER.warn("Duplicate display for block {} at {}, removing it", marker.blockId(), marker.pos());
                Utils.runLater(display::remove);
            }
            return;
        }

        SmpBlock block = BlockTypes.create(marker.blockId());
        if(block == null){
            RogueSmpCore.LOGGER.warn("Display at {} references unknown block '{}'", marker.pos(), marker.blockId());
            return;
        }

        block.bind(marker.pos(), display.getUniqueId());
        blocks.put(marker.pos(), block);

        if(block.isStateful()) hydrate(block);
        else {
            block.markHydrated();
            registerIfTickable(block);
        }
    }

    private void detach(BlockPos pos, Entity display, Map<BlockPos, String> changed){
        SmpBlock block = blocks.get(pos);
        if(block == null || !block.getDisplayId().equals(display.getUniqueId())) return;

        String state = dirtyState(block);
        if(state != null) changed.put(pos, state);
        block.onUnload();
        mining.cancelAt(pos);
        blocks.remove(pos);
        if (block instanceof Tickable) tickingBlocks.remove(block);
    }

    private void hydrate(SmpBlock block){
        repository.load(block.getPos()).whenComplete((state, error) -> {
            Utils.runLater(() -> finishHydration(block, state, error));
        });
    }

    private void finishHydration(SmpBlock block, Optional<String> state, Throwable error){
        if(blocks.get(block.getPos()) != block) return;
        if(error != null){
            RogueSmpCore.LOGGER.error("Could not load state of block at {}, leaving it inactive", block.getPos(), error);
            return;
        }

        if(state.isPresent()){
            DataResult<Runnable> decoded;
            try{
                decoded = block.decodeState(JsonParser.parseString(state.get()));
            }
            catch(RuntimeException e){
                RogueSmpCore.LOGGER.error("Corrupt state of block at {}, leaving it inactive", block.getPos(), e);
                return;
            }

            if(!decoded.isSuccess()){
                RogueSmpCore.LOGGER.error("Could not decode state of block at {}, leaving it inactive: {}", block.getPos(), decoded.error());
                return;
            }
            decoded.result().run();
        }

        block.clearDirty();
        block.markHydrated();
        registerIfTickable(block);
        block.onHydrated();
    }

    /**
     * The block's encoded state if it is dirty, else null. The dirty flag is cleared only once the
     * state encoded successfully, so a failed encode is retried on the next cycle.
     */
    private @Nullable String dirtyState(SmpBlock block){
        if(!block.isDirty() || !block.isHydrated() || !block.isStateful()) return null;

        DataResult<JsonElement> encoded = block.encodeState();
        if(!encoded.isSuccess()){
            RogueSmpCore.LOGGER.error("Could not encode state of block at {}: {}", block.getPos(), encoded.error());
            return null;
        }

        block.clearDirty();
        return encoded.result().toString();
    }

    /**
     * Spreads the periodic save over FLUSH_PERIOD ticks: once per cycle the loaded blocks are
     * snapshotted, then each tick saves the dirty blocks of an equal slice in a single batch.
     */
    private void flushSlice(){
        if(flushTick == 0){
            flushQueue.clear();
            flushQueue.addAll(blocks.values());
            flushBatchSize = Math.max(1, (int) Math.ceil(flushQueue.size() / (double) FLUSH_PERIOD));
        }
        flushTick = (flushTick + 1) % FLUSH_PERIOD;
        if(flushQueue.isEmpty()) return;

        Map<BlockPos, String> changed = null;
        for(int i = 0; i < flushBatchSize && !flushQueue.isEmpty(); i++){
            SmpBlock block = flushQueue.poll();
            if(blocks.get(block.getPos()) != block) continue;

            String state = dirtyState(block);
            if(state != null){
                if(changed == null) changed = new HashMap<>();
                changed.put(block.getPos(), state);
            }
        }
        if(changed != null) repository.saveAll(changed);
    }

    public void shutdown(){
        mining.stopAll();

        Map<BlockPos, String> states = new HashMap<>();
        for(SmpBlock block : blocks.values()){
            if(!block.isHydrated() || !block.isStateful()) continue;

            DataResult<JsonElement> encoded = block.encodeState();
            if(encoded.isSuccess()) states.put(block.getPos(), encoded.result().toString());
            else RogueSmpCore.LOGGER.error("Could not encode state of block at {}: {}", block.getPos(), encoded.error());
        }

        repository.closeWith(states);
        blocks.clear();
        tickingBlocks.clear();
    }

    public static BlockManager getInstance() {
        if (INSTANCE == null) {
            throw new RuntimeException(BlockManager.class.getSimpleName() + "is null when getInstance() is called.");
        }
        return INSTANCE;
    }

    public @Nullable SmpBlock get(BlockPos pos){
        return blocks.get(pos);
    }

    public boolean isSmpBlock(Location loc){
        return blocks.containsKey(BlockPos.of(loc));
    }

    /**
     * Only returns blocks whose saved state is already restored, so nothing can push items or
     * energy into a machine that is about to have its contents overwritten.
     */
    public @Nullable SmpBlock getBlock(Location loc){
        SmpBlock block = blocks.get(BlockPos.of(loc));
        return block != null && block.isHydrated() ? block : null;
    }

    public Collection<SmpBlock> getAllBlocks() {return Collections.unmodifiableCollection(blocks.values());}

    public MiningManager getMining() {return mining;}

    public static void init(RogueSmpCore core) {INSTANCE = new BlockManager(core);}

}
