package com.roguesmp.world;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Blocking;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.List;
import java.util.function.Predicate;

/**
 * PolarPaper save world config in its own config file while our worlds keep their settings in different place, so Polar config entries are only clutter.
 */
final class PolarPaperConfig {

    private static final String WORLDS_SECTION = "worlds";

    private PolarPaperConfig() {
    }

    /**
     * Removes the entries of every world whose key matches. Main thread only, as it edits PolarPaper's live config.
     *
     * @return how many entries were removed
     */
    @Blocking
    static int forget(Predicate<String> worldKeyValue) {
        Plugin polarPlugin = Bukkit.getPluginManager().getPlugin("polarpaper");
        if (polarPlugin == null) return 0;

        FileConfiguration config = polarPlugin.getConfig();
        ConfigurationSection worlds = config.getConfigurationSection(WORLDS_SECTION);
        if (worlds == null) return 0;

        List<String> stale = worlds.getKeys(false).stream().filter(worldKeyValue).toList();
        if (stale.isEmpty()) return 0;

        stale.forEach(name -> worlds.set(name, null));
        try {
            Files.writeString(live.minehub.polarpaper.PolarPaper.getConfigPath(), config.saveToString());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return stale.size();
    }
}
