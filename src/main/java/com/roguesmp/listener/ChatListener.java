package com.roguesmp.listener;

import com.roguesmp.integration.LuckPermsIntegration;
import com.roguesmp.quest.QuestManager;
import com.roguesmp.quest.board.QuestBoardManager;
import com.roguesmp.quest.data.PlayerQuestData;
import com.roguesmp.utils.Utils;
import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class ChatListener implements Listener {

    private static final Component SEPARATOR = Component.text(": ", NamedTextColor.GRAY);

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        LuckPermsIntegration luckPerms = LuckPermsIntegration.getInstance();
        PlayerQuestData questData = QuestManager.getInstance().getPlayerQuestData(event.getPlayer().getUniqueId());
        Component adventureSuffix = questData == null ? Component.empty() : QuestBoardManager.getInstance().getRank(questData).getDisplayIcon();
        event.renderer(ChatRenderer.viewerUnaware((source, sourceDisplayName, message) -> Component.empty()
                .append(Utils.fromString(luckPerms.prefix(source)))
                .append(sourceDisplayName)
                .append(Utils.fromString(luckPerms.suffix(source)))
                .append(adventureSuffix)
                .append(SEPARATOR)
                .append(message)));
    }
}
