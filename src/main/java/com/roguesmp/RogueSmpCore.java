package com.roguesmp;

import com.roguesmp.constant.ComponentKeys;
import com.roguesmp.effect.EffectManager;
import com.roguesmp.gui.ItemBrowser;
import com.roguesmp.integration.PlaceholderAPIIntegration;
import com.roguesmp.listener.DamageListener;
import com.roguesmp.listener.EffectListener;
import com.roguesmp.listener.GuiListener;
import com.roguesmp.listener.PlayerListener;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.registry.GemRegistry;
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

        ComponentKeys.loadClass();

        ModifierRegistry.init(this);

        ItemRegistry.init(this);
        GemRegistry.init(this);

        PlayerManager.init(this);
        EffectManager.init(this);
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
        registerListener(new GuiListener());

        registerListener(new DamageListener());
        registerListener(new PlayerListener(PlayerManager.getInstance()));
        registerListener(new EffectListener(EffectManager.getInstance()));
    }

    //Register CommandAPICommand
    public void initCommands() {
        ItemBrowser.registerCommand();
        EffectManager.registerCommand();
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
