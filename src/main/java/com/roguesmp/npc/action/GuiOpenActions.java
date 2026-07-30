package com.roguesmp.npc.action;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.gui.BlacksmithGui;
import com.roguesmp.gui.quest.DailyQuestGui;
import com.roguesmp.registry.Registries;
import io.papermc.paper.registry.keys.SoundEventKeys;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

/**
 * NPC GUI-open actions. Each constant registers itself into {@link Registries#NPC_GUI_OPEN_ACTION}
 * as it's initialized - call {@link #loadClass()} to force that to happen.
 */
public class GuiOpenActions {

    @FunctionalInterface
    public interface OpenAction {
        void run(Player player);
    }

    public static final OpenAction BLACKSMITH = register("blacksmith", player -> {
        new BlacksmithGui(player).showInventory(player);
        player.playSound(Sound.sound(SoundEventKeys.ITEM_ARMOR_EQUIP_CHAIN, Sound.Source.PLAYER, 1f, 1.2f));
    });
    public static final OpenAction QUEST = register("quest", player -> {
        new DailyQuestGui(player).showInventory(player);
        player.playSound(Sound.sound(SoundEventKeys.ITEM_BOOK_PAGE_TURN, Sound.Source.PLAYER, 1f, 0.9f));
    });

    public static void loadClass() {

    }

    public static void runOpenAction(String id, Player player) {
        OpenAction openAction = Registries.NPC_GUI_OPEN_ACTION.get(id);
        if (openAction == null) {
            player.sendMessage(Component.text("Could not find GUI open action with id: " + id, NamedTextColor.RED));
            RogueSmpCore.LOGGER.warn("Could not find GUI open action with id: {}", id);
            return;
        }
        openAction.run(player);
    }

    private static OpenAction register(String id, OpenAction action) {
        Registries.NPC_GUI_OPEN_ACTION.register(id, action);
        return action;
    }
}
