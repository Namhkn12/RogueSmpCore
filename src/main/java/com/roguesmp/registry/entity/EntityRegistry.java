package com.roguesmp.registry.entity;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.entity.BaseEntity;
import com.roguesmp.entity.SmpEntity;
import com.roguesmp.entity.boss.hellknight.HellKnight;
import com.roguesmp.entity.boss.hellknight.minion.*;
import com.roguesmp.entity.boss.hellknight.minion.companion.HellKnightCompanion;
import com.roguesmp.entity.boss.primordialslime.PrimordialSlime;
import com.roguesmp.utils.Utils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.StringArgument;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

public class EntityRegistry {
    private static EntityRegistry INSTANCE;
    public static final String FOLDER_NAME = "entities";

    private final Map<String, BaseEntity> definitions = new HashMap<>();
    private final Map<String, BiFunction<BaseEntity, LivingEntity, SmpEntity>> factories = new HashMap<>();
    private final RogueSmpCore plugin;

    private EntityRegistry(RogueSmpCore plugin) {
        this.plugin = plugin;

        registerSpecial("primordial_slime", PrimordialSlime::new);

        registerSpecial("hell_knight", HellKnight::new);
        registerSpecial(HellKnightCompanion.ID, HellKnightCompanion::new);
        registerSpecial("hell_knight_hordes", HellKnightHordes::new);
        registerSpecial("hell_knight_minion_melee", HellKnightMinionMelee::new);
        registerSpecial("hell_knight_minion_ranged", HellKnightMinionRanged::new);
        registerSpecial("hell_knight_evoker", HellKnightEvoker::new);
        registerSpecial("hell_knight_stray", HellKnightStray::new);
        registerSpecial("hell_knight_blaze", HellKnightBlaze::new);
        registerSpecial("hell_knight_golem", HellKnightGolem::new);

    }

    public @Nullable SmpEntity spawnEntity(String id, Location location) {
        BaseEntity base = definitions.get(id);
        if (base == null) return null;
        return base.spawn(location);
    }

    /**
     * Wrap an Entity within SmpEntity, or its subclasses
     */
    public SmpEntity wrap(BaseEntity base, LivingEntity living) {
        // Default to standard SmpEntity if no special factory exists
        return factories.getOrDefault(base.getId(), SmpEntity::new).apply(base, living);
    }

    public boolean isSpecialEntity(String id) {
        return factories.containsKey(id);
    }

    public BaseEntity getBaseEntity(String id) {
        return definitions.get(id);
    }

    public void loadFromFile() {
        File itemsDir = new File(plugin.getDataFolder(), FOLDER_NAME);

        if (!itemsDir.exists() || !itemsDir.isDirectory()) {
            plugin.getLogger().info("No entity folder found, starting with empty registry");
            return;
        }

        File[] files = itemsDir.listFiles((dir, name) -> name.endsWith(".json"));

        if (files == null || files.length == 0) {
            plugin.getLogger().info("Entity folder is empty");
            return;
        }

        for (File file : files) {
            try (Reader reader = new FileReader(file)) {
                BaseEntity entity = Utils.GSON.fromJson(reader, BaseEntity.class);

                if (entity == null || entity.getId() == null) {
                    plugin.getLogger().warning("Invalid entity file: " + file.getName());
                    continue;
                }

                definitions.put(entity.getId(), entity);

            } catch (Exception e) {
                plugin.getLogger().severe("Failed to load entity file: " + file.getName());
                e.printStackTrace();
            }
        }

        plugin.getLogger().info("Loaded entity registry (" + definitions.size() + " entries)");
    }

    private void registerSpecial(String id, BiFunction<BaseEntity, LivingEntity, SmpEntity> factory) {
        factories.put(id, factory);
    }

    public static void init(RogueSmpCore plugin) {
        INSTANCE = new EntityRegistry(plugin);
    }

    public static EntityRegistry getInstance() {
        if (INSTANCE == null) {
            throw new RuntimeException("EntityRegistry is null");
        }
        return INSTANCE;
    }

    public void reload() {
        definitions.clear();
        loadFromFile();
    }

    public static void registerCommand() {

        // Spawn command
        new CommandAPICommand("smpentity")
                .withArguments(new StringArgument("entity_id"))
                .executesPlayer((player, args) -> {

                    String id = (String) args.get("entity_id");

                    BaseEntity base = getInstance().getBaseEntity(id);

                    if (base == null) {
                        player.sendMessage("Id not found");
                        return;
                    }

                    base.spawn(player.getLocation());

                }).register();


        new CommandAPICommand("smpentityreload")
                .executes((sender, args) -> {

                    getInstance().reload();

                    sender.sendMessage("§aEntity registry reloaded.");

                }).register();
    }
}
