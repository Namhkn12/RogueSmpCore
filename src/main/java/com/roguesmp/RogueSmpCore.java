package com.roguesmp;

import com.roguesmp.constant.ComponentKeys;
import com.roguesmp.gui.ItemBrowser;
import com.roguesmp.listener.GuiListener;
import com.roguesmp.registry.ItemRegistry;
import com.roguesmp.listener.PlayerListener;
import com.roguesmp.player.PlayerManager;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public final class RogueSmpCore extends JavaPlugin {

    private static RogueSmpCore INSTANCE;

    // Init whatever here, called before initListeners
    public void init() {
        ComponentKeys.loadClass();

        ItemRegistry.init(this);

        PlayerManager.init(this);
    }

    // Load data from files, databases, etc
    public void loadData() {
        ItemRegistry.getInstance().loadFromFile();
    }

    //Run on onDisable
    public void saveData() {
        ItemRegistry.getInstance().saveToFile();
    }

    // Register Listener here
    public void initListeners() {
        registerListener(new PlayerListener(PlayerManager.getInstance()));
        registerListener(new GuiListener());
    }

    //Register CommandAPICommand
    public void initCommands() {
        ItemBrowser.registerCommand();
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
