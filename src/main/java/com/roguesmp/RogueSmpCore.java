package com.roguesmp;

import com.roguesmp.block.manager.BlockManager;
import com.roguesmp.block.storage.BlockStorage;
import com.roguesmp.constant.ComponentKeys;
import com.roguesmp.constant.Tags;
import com.roguesmp.dungeon.DungeonRegistry;
import com.roguesmp.effect.EffectManager;
import com.roguesmp.entity.EntityManager;
import com.roguesmp.goal.zombified_piglin.PigZombieSpawnListener;
import com.roguesmp.gui.ItemBrowser;
import com.roguesmp.gui.SkinBrowserGui;
import com.roguesmp.gui.TrashGui;
import com.roguesmp.gui.ability.AbilityCatalogue;
import com.roguesmp.integration.PlaceholderAPIIntegration;
import com.roguesmp.island.IslandManager;
import com.roguesmp.listener.*;
import com.roguesmp.npc.NpcManager;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.registry.BlockRegistry;
import com.roguesmp.registry.SkinRegistry;
import com.roguesmp.registry.VanillaCraftingRecipeRegistry;
import com.roguesmp.registry.entity.EntityRegistry;
import com.roguesmp.registry.ItemRegistry;
import com.roguesmp.registry.ability.AbilityRegistry;
import com.roguesmp.registry.npc.NpcRegistry;
import com.roguesmp.utils.GlowUtils;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RogueSmpCore extends JavaPlugin {

    private static RogueSmpCore INSTANCE;

    public static final Logger LOGGER = LoggerFactory.getLogger("RogueSMP");

    private GlobalConfig globalConfig;

    // Init whatever here, called before initListeners
    public void init() {
        globalConfig = GlobalConfig.loadGlobalConfig(this);
        new PlaceholderAPIIntegration(this).register();
        GlowUtils.init(this);

        ComponentKeys.loadClass();

        SkinRegistry.init();

        AbilityRegistry.init();

        //Player
        PlayerManager.init(this);

        //Island
        IslandManager.init(this, PlayerManager.getInstance());

        EffectManager.init(this);
        BlockManager.init(this);

        //Entity
        EntityRegistry.init(this);
        EntityManager.init(EntityRegistry.getInstance());

        //Npc
        NpcRegistry.init();
        NpcManager.init();

        ItemRegistry.init(this);
        BlockRegistry.init(this);

        VanillaCraftingRecipeRegistry.init(this);

        BlockStorage.init(this, BlockManager.getInstance());
        BlockRegistry.getInstance().registerMachineRecipes();
        //dungeon register
        DungeonRegistry.onEnable(this, ItemRegistry.getInstance());
    }

    // Load data from files, databases, etc
    public void loadData() {
        SkinRegistry.getInstance().loadSkin();
        ItemRegistry.getInstance().loadFromFile();
        EntityRegistry.getInstance().loadFromFile();
        BlockStorage.getInstance().loadFromFile();
        AbilityRegistry.getInstance().loadAll();
        NpcRegistry.getInstance().loadData();

        Tags.loadTagData(this);
    }

    //Run on onDisable
    public void saveData() {
        SkinRegistry.getInstance().saveSkin();
        ItemRegistry.getInstance().saveToFile(false);
        BlockStorage.getInstance().saveToFile(true);

        PlayerManager.getInstance().onDisable();
        DungeonRegistry.onDisable();

        IslandManager.getInstance().onDisable();
    }

    // Register Listener here
    public void initListeners() {
        registerListener(new ItemInteractionListener());
        registerListener(new GuiListener());
        registerListener(new BlockListener());

        registerListener(new DamageListener());
        registerListener(new PlayerListener(PlayerManager.getInstance(), IslandManager.getInstance()));
        registerListener(new EffectListener(EffectManager.getInstance()));
        registerListener(new PigZombieSpawnListener(this));
        registerListener(new EntityListener(EntityManager.getInstance()));
        registerListener(new NpcListener(NpcManager.getInstance()));

        registerListener(new IslandListener(IslandManager.getInstance()));
        registerListener(new WalletPickupListener());
    }

    //Register CommandAPICommand
    public void initCommands() {
        SkinRegistry.registerSkinFetchCommand();
        SkinBrowserGui.registerCommand();
        ItemBrowser.registerCommand();
        EffectManager.registerCommand();
        EntityRegistry.registerCommand();
        NpcManager.getInstance().registerCommand();

        AbilityCatalogue.register();

        TrashGui.register();

        IslandManager.getInstance().registerCommands();
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
    }

    public static RogueSmpCore getInstance() {
        return INSTANCE;
    }

    public static GlobalConfig getGlobalConfig() {
        return INSTANCE.globalConfig;
    }

    public void registerListener(Listener listener) {
        this.getServer().getPluginManager().registerEvents(listener, this);
    }
}
