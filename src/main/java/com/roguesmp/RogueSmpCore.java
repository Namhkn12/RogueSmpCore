package com.roguesmp;

import com.roguesmp.block.manager.BlockManager;
import com.roguesmp.block.storage.BlockStorage;
import com.roguesmp.constant.ComponentKeys;
import com.roguesmp.dungeon.DungeonRegistry;
import com.roguesmp.effect.EffectManager;
import com.roguesmp.entity.EntityManager;
import com.roguesmp.gui.ItemBrowser;
import com.roguesmp.gui.ability.AbilityCatalogue;
import com.roguesmp.integration.PlaceholderAPIIntegration;
import com.roguesmp.listener.*;
import com.roguesmp.player.PlayerDataManager;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.registry.BlockRegistry;
import com.roguesmp.registry.EntityRegistry;
import com.roguesmp.registry.ItemRegistry;
import com.roguesmp.registry.ModifierRegistry;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RogueSmpCore extends JavaPlugin {

    private static RogueSmpCore INSTANCE;

    public static final Logger LOGGER = LoggerFactory.getLogger("RogueSMP");

    // Init whatever here, called before initListeners
    public void init() {
        new PlaceholderAPIIntegration(this).register();

        PlayerDataManager.init();

        ComponentKeys.loadClass();

        PlayerManager.init(this, PlayerDataManager.getInstance());
        EffectManager.init(this);
        BlockManager.init(this);

        //Entity
        EntityManager.init();
        EntityRegistry.init(this);

        ItemRegistry.init(this);
        BlockRegistry.init(this);
        ModifierRegistry.init(this);

        BlockStorage.init(this, BlockManager.getInstance());
        BlockRegistry.getInstance().registerMachineRecipes();
        //dungeon register
        DungeonRegistry.onEnable();
    }

    // Load data from files, databases, etc
    public void loadData() {
        ItemRegistry.getInstance().loadFromFile();
        EntityRegistry.getInstance().loadFromFile();
        BlockStorage.getInstance().loadFromFile();
    }

    //Run on onDisable
    public void saveData() {
        ItemRegistry.getInstance().saveToFile(true);
        BlockStorage.getInstance().saveToFile(true);

        PlayerManager.getInstance().onDisable();
    }

    // Register Listener here
    public void initListeners() {
        registerListener(new GuiListener());
        registerListener(new BlockListener());
        registerListener(new WrenchListener());

        registerListener(new DamageListener());
        registerListener(new PlayerListener(PlayerManager.getInstance(), PlayerDataManager.getInstance()));
        registerListener(new EffectListener(EffectManager.getInstance()));
        registerListener(new EntityListener(EntityManager.getInstance(), EntityRegistry.getInstance()));
    }

    //Register CommandAPICommand
    public void initCommands() {
        ItemBrowser.registerCommand();
        EffectManager.registerCommand();
        EntityRegistry.registerCommand();

        AbilityCatalogue.register();
    }

    @Override
    public void onEnable() {
        // Plugin startup logic

        INSTANCE = this;

        init();
        loadData();
        initListeners();
        initCommands();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        saveData();
        DungeonRegistry.onDisable();
    }

    public static RogueSmpCore getInstance() {
        return INSTANCE;
    }

    public void registerListener(Listener listener) {
        this.getServer().getPluginManager().registerEvents(listener, this);
    }
}
