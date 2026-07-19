package com.roguesmp.registry.npc;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.gui.BlacksmithGui;
import io.papermc.paper.registry.keys.SoundEventKeys;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class GuiOpenActionRegistry {
    @FunctionalInterface
    public interface OpenAction {
        void run(Player player);
    }

    private static final Map<String, OpenAction> registry = new HashMap<>();

    static {
        register("blacksmith", player -> {
            new BlacksmithGui(player).showInventory(player);
            player.playSound(Sound.sound(SoundEventKeys.ITEM_ARMOR_EQUIP_CHAIN, Sound.Source.PLAYER, 1f, 1.2f));
        });
    }

    public static void runOpenAction(String id, Player player) {
        OpenAction openAction = registry.get(id);
        if (openAction == null) {
            player.sendMessage(Component.text("Could not find GUI open action with id: "+ id, NamedTextColor.RED));
            RogueSmpCore.LOGGER.warn("Could not find GUI open action with id: {}", id);
            return;
        }
        openAction.run(player);
    }

    private static void register(String id, OpenAction openAction) {
        registry.put(id, openAction);
    }
}
