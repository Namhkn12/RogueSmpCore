package com.roguesmp;

import com.roguesmp.constant.ComponentKeys;
import com.roguesmp.dungeon.party.PartyManager;
import com.roguesmp.dungeon.schemeta.SchemetaManager;
import com.roguesmp.gui.ItemBrowser;
import com.roguesmp.item.gem.GemData;
import com.roguesmp.listener.GuiListener;
import com.roguesmp.registry.GemRegistry;
import com.roguesmp.registry.ItemRegistry;
import com.roguesmp.listener.PlayerListener;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.registry.ModifierRegistry;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public final class RogueSmpCore extends JavaPlugin {

    private static RogueSmpCore INSTANCE;

    // Init whatever here, called before initListeners
    public void init() {
        ComponentKeys.loadClass();

        ModifierRegistry.init(this);

        ItemRegistry.init(this);
        GemRegistry.init(this);

        PlayerManager.init(this);
        PartyManager.init();
        SchemetaManager.init(this);
    }

    // Load data from files, databases, etc
    public void loadData() {
        ItemRegistry.getInstance().loadFromFile();
        GemRegistry.getInstance().loadFromFile();
    }

    //Run on onDisable
    public void saveData() {
        ItemRegistry.getInstance().saveToFile(false);
        GemRegistry.getInstance().saveToFile(false);
    }

    // Register Listener here
    public void initListeners() {
        registerListener(new PlayerListener(PlayerManager.getInstance()));
        registerListener(new GuiListener());
    }

    //Register CommandAPICommand
    public void initCommands() {
        ItemBrowser.registerCommand();
        PartyManager.getInstance().registerCommands();
        SchemetaManager.getInstance().registerCommands();
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

    private void registerListener(Listener listener) {
        this.getServer().getPluginManager().registerEvents(listener, this);
    }
}
