package com.roguesmp;

import com.roguesmp.block.manager.BlockManager;
import com.roguesmp.block.storage.BlockStorage;
import com.roguesmp.gui.crafting.FusionGui;
import com.roguesmp.crafting.CraftingManager;
import com.roguesmp.item.component.ItemComponentKeys;
import com.roguesmp.entity.component.EntityComponentKeys;
import com.roguesmp.registry.Registries;
import com.roguesmp.dungeon.DungeonRegistry;
import com.roguesmp.effect.EffectManager;
import com.roguesmp.entity.EntityManager;
import com.roguesmp.fx.FxCommand;
import com.roguesmp.fx.FxEngine;
import com.roguesmp.goal.zombified_piglin.PigZombieSpawnListener;
import com.roguesmp.gui.CreatorHubGui;
import com.roguesmp.gui.ItemBrowser;
import com.roguesmp.gui.ItemRepairGui;
import com.roguesmp.gui.SkinBrowserGui;
import com.roguesmp.gui.TrashGui;
import com.roguesmp.gui.ability.AbilityCatalogue;
import com.roguesmp.gui.crafting.CraftingGui;
import com.roguesmp.gui.info.SmpWikiMainMenuGui;
import com.roguesmp.gui.itemcreator.ItemCreatorGui;
import com.roguesmp.gui.entitycreator.EntityCreatorGui;
import com.roguesmp.integration.PlaceholderAPIIntegration;
import com.roguesmp.island.IslandManager;
import com.roguesmp.listener.*;
import com.roguesmp.gui.loottablecreator.LootTableGuiCreator;
import com.roguesmp.gui.recipecreator.RecipeCreatorGui;
import com.roguesmp.loot.manager.LootTableManager;
import com.roguesmp.loot.service.LootService;
import com.roguesmp.npc.NpcManager;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.quest.QuestManager;
import com.roguesmp.registry.*;
import com.roguesmp.server.DailyResetScheduler;
import com.roguesmp.tab.TabDemoCommand;
import com.roguesmp.tab.TabEngine;
import com.roguesmp.utils.GlowUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RogueSmpCore extends JavaPlugin {

    private static RogueSmpCore INSTANCE;

    public static final Logger LOGGER = LoggerFactory.getLogger("RogueSMP");

    private GlobalConfig globalConfig;

    private DailyResetScheduler resetScheduler;

    // Init whatever here, called before initListeners
    public void init() {
        globalConfig = GlobalConfig.loadGlobalConfig(this);
        new PlaceholderAPIIntegration(this).register();
        GlowUtils.init(this);

        Registries.boostrap(this);

        FxEngine.init(this);

        ItemComponentKeys.loadClass();
        EntityComponentKeys.loadClass();

        SkinRegistry.init();

        //Player
        PlayerManager.init(this);

        //Island
        IslandManager.init(this, PlayerManager.getInstance());

        //Quest
        QuestManager.init(this);

        EffectManager.init(this);
        BlockManager.init(this);

        //Entity
        EntityManager.init(this);

        //Loot - plugin-wide singleton so any system (mob death, chests, quests, ...) can roll
        LootService.init(new LootTableManager());

        //Npc
        NpcManager.init();

        ItemRegistry.init(this);
        BlockRegistry.init(this);

        VanillaCraftingRecipeRegistry.init(this);

        BlockStorage.init(this, BlockManager.getInstance());
        BlockRegistry.getInstance().registerMachineRecipes();
        //dungeon register
        DungeonRegistry.onEnable(this);

        this.resetScheduler = new DailyResetScheduler(this);
        this.resetScheduler.start();

        TabEngine.init(this);
    }

    // Load data from files, databases, etc
    public void loadData() {
        Registries.loadAllData(this); // also loads/resolves every registry's tags/ folder
        BlockStorage.getInstance().loadFromFile();

        CraftingManager.init(); // indexes Registries.CRAFTING_RECIPE - must run after it's loaded above
    }

    //Run on onDisable
    public void saveData() {
        BlockStorage.getInstance().saveToFile(true);

        PlayerManager.getInstance().onDisable();
        DungeonRegistry.onDisable();

        IslandManager.getInstance().onDisable();
        QuestManager.getInstance().onDisable();
    }

    // Register Listener here
    public void initListeners() {
        registerListener(new GuiListener());
        registerListener(new BlockListener());

        registerListener(new DamageListener());
        registerListener(new PlayerListener(PlayerManager.getInstance(), IslandManager.getInstance()));
        registerListener(new EffectListener(EffectManager.getInstance()));
        registerListener(new PigZombieSpawnListener(this));
        registerListener(new EntityListener(EntityManager.getInstance()));
        registerListener(new NpcListener(NpcManager.getInstance()));

        registerListener(new IslandListener(IslandManager.getInstance()));
        registerListener(new QuestListener(QuestManager.getInstance()));
    }

    //Register CommandAPICommand
    public void initCommands() {
        SkinRegistry.registerSkinFetchCommand();
        SkinBrowserGui.registerCommand();
        ItemBrowser.registerCommand();

        ItemCreatorGui.registerCommand();
        EntityCreatorGui.registerCommand();
        LootTableGuiCreator.registerCommand();
        RecipeCreatorGui.registerCommand();
        CreatorHubGui.registerCommand();

        EffectManager.registerCommand();
        EntityManager.registerCommand();

        FxCommand.register();

        NpcManager.getInstance().registerCommand();

        AbilityCatalogue.register();

        TrashGui.register();

        SmpWikiMainMenuGui.registerCommands();

        ItemRepairGui.registerCommand();
        CraftingGui.registerCmd();
        FusionGui.register();

        IslandManager.getInstance().registerCommands();
        QuestManager.getInstance().registerQuestCommand();
        TabDemoCommand.register();
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

        FxEngine.getInstance().shutdown();
        TabEngine.getInstance().shutdown();

        if (this.resetScheduler != null) {
            resetScheduler.stop();
        }

        Bukkit.getOnlinePlayers().forEach(player -> {
            player.closeInventory();
            player.kick(Component.text("Server đang tắt..."));
        });
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
