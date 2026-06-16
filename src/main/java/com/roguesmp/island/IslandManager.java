package com.roguesmp.island;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.gui.island.IslandMainGui;
import com.roguesmp.player.PlayerData;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.utils.Utils;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.PlayerProfileArgument;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import org.mvplugins.multiverse.core.teleportation.PassengerModes;

import java.io.File;
import java.io.FileInputStream;
import java.util.UUID;

public class IslandManager {

    private static IslandManager INSTANCE;
    private static final int TEAM_SIZE = 4;

    private final RogueSmpCore plugin;
    private final PlayerManager playerManager;
    private final IslandWorldManager islandWorldManager;
    private final IslandDataManager islandDataManager;
    private final IslandRegionManager islandRegionManager;

    private IslandManager(RogueSmpCore plugin, PlayerManager playerManager) {
        this.plugin = plugin;
        this.playerManager = playerManager;
        this.islandWorldManager = new IslandWorldManager(plugin);
        this.islandWorldManager.loadSkyblockWorld();
        this.islandDataManager = new IslandDataManager(plugin);
        this.islandRegionManager = new IslandRegionManager();
    }

    public IslandData createIsland(Player creator) {
        UUID creatorId = creator.getUniqueId();

        int assignedIndex = islandWorldManager.requestNextFreeIndex();

        Location centerLocation = islandWorldManager.getIslandCenter(assignedIndex);
        generateBaselineIslandStructure(centerLocation);

        Location spawnPoint = centerLocation.clone().add(0.5, 2.0, 0.5);
        islandWorldManager.getMultiverseApi().getSafetyTeleporter()
                .to(spawnPoint)
                .passengerMode(PassengerModes.RETAIN_ALL)
                .teleportSingle(creator)
                .onSuccess(() -> creator.setRespawnLocation(spawnPoint, true));

        IslandData islandData = new IslandData(creatorId, assignedIndex);
        islandData.setSpawnLocation(spawnPoint);

        //Create WorldGuard Region
        islandRegionManager.createIslandRegion(creator, centerLocation, islandData.getIslandId());

        creator.sendMessage(Utils.fromString("<green>Tạo đảo thành công!"));

        PlayerData playerData = playerManager.getDataManager().getData(creatorId);
        if (playerData != null) playerData.setIslandId(islandData.getIslandId());

        //Data save
        islandDataManager.cache(islandData);
        Utils.runAsync(() -> islandDataManager.saveIslandData(islandData));

        return islandData;
    }

    /**
     * Updates an island's custom spawn point if the player is within their own territory.
     *
     * @param player         The player requesting the change.
     * @param targetLocation The new spawn location vector.
     * @return true if successfully updated; false if unauthorized or location is out of bounds.
     */
    public boolean setIslandSpawn(Player player, Location targetLocation) {

        if (player.getLocation().getBlock().getType() == Material.AIR) {
            player.sendMessage(Utils.fromString("<red>Không thể đặt spawn ở vị trí này"));
            return false;
        }

        PlayerData playerData = playerManager.getDataManager().getData(player.getUniqueId());
        if (playerData == null || playerData.getIslandId() == null) return false;

        IslandData islandData = islandDataManager.getCachedData(playerData.getIslandId());
        if (islandData == null) return false;

        // Ensure the player is in the correct island world container
        if (!isInIslandWorld(player)) {
            player.sendMessage(Utils.fromString("<red>Hành động này chỉ hoạt động ở đảo cá nhân."));
            return false;
        }

        // Use WorldGuard to verify if the player actually has permission at this exact block vector
        if (!islandRegionManager.isLocationInsideRegion(islandData.getIslandId(), targetLocation)) {
            player.sendMessage(Utils.fromString("<red>Không thể đặt spawn ở vị trí này"));
            return false;
        }

        // Update values and update active team references
        islandData.setSpawnLocation(targetLocation);

        return true;
    }

    /**
     * Integrates a new player into an existing island cooperative network.
     * Validates restrictions, mutates cached records, handles WorldGuard configurations,
     * and binds telemetry data vectors safely.
     *
     * @param islandId The unique ID of the targeted skyblock island.
     * @param recruit  The Player joining the team roster.
     * @return true if successfully added; false if they fail preconditions or are already teamed.
     */
    public boolean addMember(UUID islandId, Player recruit) {
        if (islandId == null || recruit == null) return false;

        // 1. Resolve live cached profile context
        IslandData islandData = islandDataManager.getCachedData(islandId);
        if (islandData == null) {
            RogueSmpCore.LOGGER.warn("Attempted to add member to island {}, but it isn't cached!", islandId);
            return false;
        }

        // 2. Validate current team size
        if (islandData.getMembers().size() >= TEAM_SIZE) {
            recruit.sendMessage(Utils.fromString("<red>Đảo này đã đạt giới hạn tối đa thành viên <yellow>(4/4)!"));
            return false;
        }

        // 3. Ensure the target player is completely unbound from other island nodes
        PlayerData recruitData = playerManager.getDataManager().getData(recruit.getUniqueId());
        if (recruitData == null) return false;

        if (recruitData.getIslandId() != null) {
            recruit.sendMessage(Utils.fromString("<red>Bạn đã là thành viên một hòn đảo rồi! Bạn phải rời đảo cũ trước."));
            return false;
        }

        // 4. Mutate tracking components and update relationships
        islandData.addMember(recruit.getUniqueId());
        recruitData.setIslandId(islandId); // Bind relationships across domains

        // 5. Append matching build/break clearance rules inside WorldGuard definitions
        org.bukkit.World world = islandWorldManager.getIslandWorld(islandData);
        if (world != null) {
            islandRegionManager.addTeammateToIslandRegion(recruit.getUniqueId(), islandId, world);
        }

        // 7. Flush mutations out to file records completely off-thread
        Utils.runAsync(() -> islandDataManager.saveIslandData(islandData));

        RogueSmpCore.LOGGER.info("Player {} ({}) successfully joined island network {}.",
                recruit.getName(), recruit.getUniqueId(), islandId);

        return true;
    }

    /**
     * Evicts a member from an island coop network.
     * Clears their data context, revokes region access, updates spawns, and ejects them if present.
     *
     * @param islandId   The unique ID of the target skyblock island.
     * @param targetUuid The UUID of the player being removed.
     * @return true if successfully evicted; false if they weren't part of the team.
     */
    public boolean removeMember(UUID islandId, UUID targetUuid) {
        if (islandId == null || targetUuid == null) return false;

        // 1. Resolve live cached profile context
        IslandData islandData = islandDataManager.getCachedData(islandId);
        if (islandData == null) {
            RogueSmpCore.LOGGER.warn("Attempted to remove member from island {}, but it isn't cached!", islandId);
            return false;
        }

        // 2. Break if player isn't actually a part of this team
        if (!islandData.getMembers().contains(targetUuid)) {
            return false;
        }

        // 3. Mutate data model layers
        islandData.removeMember(targetUuid);
        if (islandData.getMembers().isEmpty()) {
            islandData.setArchived(true);
        }

        // 4. Wipe the player's saved session data link
        PlayerData playerData = playerManager.getDataManager().getData(targetUuid);
        if (playerData != null) {
            playerData.setIslandId(null); // Detach relationship
        }

        // 5. Revoke spatial authority inside WorldGuard records
        org.bukkit.World world = islandWorldManager.getIslandWorld(islandData);
        if (world != null) {
            islandRegionManager.removeTeammateFromIslandRegion(targetUuid, islandId, world);
        }

        // 6. Handle active online player contexts (Ejection / Spawns)
        Player onlineTarget = Bukkit.getPlayer(targetUuid);
        if (onlineTarget != null && onlineTarget.isOnline()) {
            // If they are currently standing on the island grid, eject them back to spawn city safely
            if (isInIslandWorld(onlineTarget)) {
                onlineTarget.sendMessage(Utils.fromString("<yellow>Bạn đã trở thành người vô gia cư nên sẽ được hộ tống về hub."));
                islandWorldManager.getMultiverseApi().getWorldManager().getDefaultWorld().peek(loadedMultiverseWorld -> {
                    islandWorldManager.getMultiverseApi().getSafetyTeleporter().to(loadedMultiverseWorld.getSpawnLocation()).passengerMode(PassengerModes.RETAIN_ALL).teleportSingle(onlineTarget);
                });
            }
        }

        Utils.runAsync(() -> islandDataManager.saveIslandData(islandData));
        return true;
    }

    /**
     * Identifies which IslandData configuration owns a specific location coordinate vector.
     * Essential for admin bypasses, event handling, and cross-island tracing metrics.
     *
     * @param location The coordinates to inspect.
     * @return The IslandData entity model found, or null if matching regions do not intersect.
     */
    public @Nullable IslandData getIslandByLocation(Location location) {
        if (!location.getWorld().getName().equals(IslandWorldManager.WORLD_NAME)) {
            return null;
        }

        ProtectedRegion region = islandRegionManager.getRegionAtLocation(location);
        if (region == null) return null;

        String uuidPart = region.getId().substring("island_".length());
        UUID islandId = UUID.fromString(uuidPart);

        return islandDataManager.getCachedData(islandId);

    }

    public void teleportToIsland(Player player) {
        PlayerData playerData = playerManager.getDataManager().getData(player.getUniqueId());
        if (playerData == null) return;
        var islandData = islandDataManager.getCachedData(playerData.getIslandId());
        if (islandData == null) {
            player.sendMessage(Utils.fromString("<red>Bạn đang vô gia cư, hãy tạo đảo trước."));
            return;
        }

        // Compute location math
        Location targetCenter = islandWorldManager.getIslandCenter(islandData);
        Location targetSpawn = islandData.getSpawnLocationWorld(targetCenter.getWorld());
        islandWorldManager.getMultiverseApi().getSafetyTeleporter().to(targetSpawn).passengerMode(PassengerModes.RETAIN_ALL).teleportSingle(player);

    }

    private void generateBaselineIslandStructure(Location center) {
        // Force the chunk context to map safely without throwing generation exceptions
        center.getChunk().load(true);

        // Paste an immutable bedrock anchor block at coordinate floor
        center.getBlock().setType(Material.BEDROCK);

        // Build a tiny 3x3 dirt platform surrounding the bedrock layer for instant player utility
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0) continue; // Leave bedrock core clear
                center.clone().add(x, 0, z).getBlock().setType(Material.DIRT);
            }
        }

        // Cap the block layers directly above with grass blocks
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                center.clone().add(x, 1, z).getBlock().setType(Material.GRASS_BLOCK);
            }
        }
    }

    private void genWorld(Location center) {

        File schematicFile = new File(plugin.getDataFolder(), "schematics/island.schem");

        if (!schematicFile.exists()) {
            RogueSmpCore.LOGGER.error("Could not generate island! Schematic missing at: {}", schematicFile.getPath());
            return;
        }

        Utils.runAsync(() -> {
            ClipboardFormat format = ClipboardFormats.findByFile(schematicFile);
            if (format == null) {
                plugin.getLogger().severe("Unknown schematic format for file: " + schematicFile.getName());
                return;
            }

            // 2. Read the clipboard stream from the disk
            try (FileInputStream fis = new FileInputStream(schematicFile);
                 ClipboardReader reader = format.getReader(fis)) {

                Clipboard clipboard = reader.read();

                Utils.runLater(() -> {

                    com.sk89q.worldedit.world.World faweWorld = BukkitAdapter.adapt(center.getWorld());

                    EditSession editSession = WorldEdit.getInstance().newEditSessionBuilder()
                            .world(faweWorld)
                            .fastMode(true)
                            .build();

                    Operation operation = new ClipboardHolder(clipboard)
                            .createPaste(editSession)
                            .to(BlockVector3.at(center.getX(), center.getY(), center.getZ()))
                            .ignoreAirBlocks(false) // Keeps air blocks from cutting off vanilla terrain shapes
                            .build();

                    Operations.complete(operation);

                    editSession.flushQueue();

                    plugin.getLogger().info("Successfully pasted island schematic at " + center.toVector());

                });


            } catch (Exception e) {
                RogueSmpCore.LOGGER.error("Failed to read or paste island schematic file system assets!", e);
            }
        });
    }

    public boolean isInIslandWorld(Player player) {
        return islandWorldManager.getIslandWorld(0).getName().equals(player.getWorld().getName());
    }

    /**
     * Get player island's world. Player may or may not be in that world, use {@code isInIslandWorld()} to check
     */
    public @Nullable World getIslandWorld(Player player) {
        PlayerData playerData = playerManager.getDataManager().getData(player.getUniqueId());
        if (playerData == null) return null;
        var islandData = islandDataManager.getCachedData(playerData.getIslandId());
        if (islandData == null) {
            return null;
        }
        return islandWorldManager.getIslandWorld(islandData);
    }

    /**
     * Get player's island spawn location (The location could be set by player)
     * @return The island's spawn Location or {@code null} if player island doesn't have a spawn location.
     */
    public @Nullable Location getIslandSpawnLocation(Player player) {
        PlayerData playerData = playerManager.getDataManager().getData(player.getUniqueId());
        if (playerData == null) return null;
        var islandData = islandDataManager.getCachedData(playerData.getIslandId());
        if (islandData == null) {
            return null;
        }
        return islandData.getSpawnLocationWorld(islandWorldManager.getIslandWorld(islandData));
    }

    public void onDisable() {
        islandWorldManager.onDisable();
        islandDataManager.onDisable();
    }

    public IslandWorldManager getIslandWorldManager() {
        return islandWorldManager;
    }

    public IslandDataManager getIslandDataManager() {
        return islandDataManager;
    }

    public PlayerManager getPlayerManager() {
        return playerManager;
    }

    public IslandRegionManager getIslandRegionManager() {
        return islandRegionManager;
    }

    public void registerCommands() {

        // 1. Subcommand: /is create
        CommandAPICommand createSub = new CommandAPICommand("create")
                .executesPlayer((player, args) -> {
                    PlayerData playerData = playerManager.getDataManager().getData(player.getUniqueId());
                    if (playerData == null) return;
                    var existingIslandId = islandDataManager.getCachedData(playerData.getIslandId());
                    if (existingIslandId != null) {
                        player.sendMessage(Utils.fromString("<red>Bạn đã có đảo rồi! Dùng <yellow>/is go <red>để dịch chuyển"));
                        return;
                    }

                    player.sendMessage(Utils.fromString("<green>Đang sắp xếp vị trí đảo cho bạn. Vui lòng chờ..."));
                    this.createIsland(player);
                });

        // 2. Subcommand: /is go
        CommandAPICommand goSub = new CommandAPICommand("go")
                .executesPlayer((player, args) -> {
                    player.sendMessage(Utils.fromString("<green>Đang dịch chuyển về đảo..."));
                    this.teleportToIsland(player);
                });

        // 3. Subcommand: /is home (Alias setup)
        CommandAPICommand homeSub = new CommandAPICommand("home")
                .executesPlayer((player, args) -> {
                    player.sendMessage(Utils.fromString("<green>Đang dịch chuyển về đảo..."));
                    this.teleportToIsland(player);
                });

        CommandAPICommand inviteSub = new CommandAPICommand("invite")
                .withArguments(new EntitySelectorArgument.OnePlayer("target"))
                .executesPlayer((player, args) -> {
                    Player target = (Player) args.get(0);
                    if (target == null) return;

                    if (player.getUniqueId().equals(target.getUniqueId())) {
                        player.sendMessage(Utils.fromString("<red>Bạn không thể tự mời chính mình!"));
                        return;
                    }

                    // Fire text notification sequence straight from the manager engine
                    IslandInviteManager.sendInvitation(player, target);
                });

        CommandAPICommand acceptSub = new CommandAPICommand("accept")
                .withArguments(new EntitySelectorArgument.OnePlayer("sender"))
                .executesPlayer((player, args) -> {
                    Player sender = (Player) args.get(0);
                    if (sender == null) return;

                    // 1. Check if the invitation lifetime window has elapsed
                    if (!IslandInviteManager.validateAndConsumeInvite(player, sender)) {
                        player.sendMessage(Utils.fromString("<red>Lời mời này đã hết hạn (quá 60 giây) hoặc không tồn tại!"));
                        return;
                    }

                    // 2. Fetch data model configurations
                    PlayerData senderData = IslandManager.getInstance().getPlayerManager().getDataManager().getData(sender.getUniqueId());
                    if (senderData == null || senderData.getIslandId() == null) {
                        player.sendMessage(Utils.fromString("<red>Đảo của đối phương không còn tồn tại."));
                        return;
                    }

                    // 3. Append the player to the team data registry structures
                    boolean joined = IslandManager.getInstance().addMember(senderData.getIslandId(), player);
                    if (joined) {
                        player.sendMessage(Utils.fromString("<green>Chào mừng! Bạn đã gia nhập đảo của " + sender.getName()));
                        sender.sendMessage(Utils.fromString("<green><b>" + player.getName() + "</b> đã chấp nhận lời mời và gia nhập hòn đảo của bạn!"));
                    } else {
                        player.sendMessage(Utils.fromString("<red>Không thể gia nhập đảo. Bạn có thể đã ở trong đảo khác, hoặc người mời bạn không online!"));
                    }
                });

        CommandAPICommand denySub = new CommandAPICommand("deny")
                .withArguments(new EntitySelectorArgument.OnePlayer("sender"))
                .executesPlayer((player, args) -> {
                    Player sender = (Player) args.get(0);
                    if (sender == null) return;

                    // Validate invitation footprint before running cancellation updates
                    if (IslandInviteManager.validateAndConsumeInvite(player, sender)) {
                        player.sendMessage(Utils.fromString("<red>Bạn đã từ chối lời mời của <b>" + sender.getName()));
                        sender.sendMessage(Utils.fromString("<red>Người chơi <b>" + player.getName() + "</b> đã từ chối lời mời vào đảo."));
                    } else {
                        player.sendMessage(Utils.fromString("<red>Lời mời này đã hết hạn hoặc không khả dụng."));
                    }
                });

        // 4. Base Command Wrapper: /island & /is
        new CommandAPICommand("island")
                .withAliases("is")
                .withSubcommand(createSub)
                .withSubcommand(goSub)
                .withSubcommand(homeSub)
                .withSubcommand(inviteSub)
                .withSubcommand(acceptSub)
                .withSubcommand(denySub)
                .executesPlayer((player, args) -> {
                    new IslandMainGui(player).showInventory(player);
                })
                .register();
    }

    public static void init(RogueSmpCore plugin, PlayerManager playerManager) {
        INSTANCE = new IslandManager(plugin, playerManager);

    }

    public static IslandManager getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("IslandManager is null!");
        }
        return INSTANCE;
    }
}
