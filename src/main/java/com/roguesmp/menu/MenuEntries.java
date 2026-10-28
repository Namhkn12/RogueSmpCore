package com.roguesmp.menu;

import com.roguesmp.gui.BaseGui;
import com.roguesmp.gui.BlacksmithGui;
import com.roguesmp.gui.ability.AbilityLoadoutGui;
import com.roguesmp.gui.classes.ClassSelectionGui;
import com.roguesmp.player.SmpPlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import com.roguesmp.gui.info.SmpWikiMainMenuGui;
import com.roguesmp.gui.island.IslandMainGui;
import com.roguesmp.menu.gui.MainMenuGui;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.player.inbox.gui.InboxGui;
import com.roguesmp.quest.board.gui.QuestBoardGui;
import com.roguesmp.warp.gui.WarpGui;
import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public final class MenuEntries {

    public static final List<MenuEntry> ENTRIES = List.of(
            new MenuEntry(10, () -> icon(Material.GRASS_BLOCK, "Đảo", NamedTextColor.GREEN), IslandMainGui::new),
            new MenuEntry(11, () -> icon(Material.WRITABLE_BOOK, "Nhiệm vụ", NamedTextColor.GOLD), QuestBoardGui::new),
            new MenuEntry(12, () -> icon(Material.BLAZE_POWDER, "Kỹ năng", NamedTextColor.RED),
                    MenuEntries::openSkillMenu),
            new MenuEntry(13, () -> icon(Material.ENDER_PEARL, "Dịch chuyển", NamedTextColor.LIGHT_PURPLE), WarpGui::new),
            new MenuEntry(14, () -> icon(Material.ANVIL, "Thợ rèn", NamedTextColor.GRAY), BlacksmithGui::new),
            new MenuEntry(15, () -> icon(Material.CHEST, "Hộp thư", NamedTextColor.YELLOW),
                    player -> new InboxGui(player, new MainMenuGui(player))),
            new MenuEntry(16, () -> icon(Material.KNOWLEDGE_BOOK, "Thư viện", NamedTextColor.AQUA),
                    player -> new SmpWikiMainMenuGui())
    );

    private MenuEntries() {
    }

    public static @Nullable BaseGui openSkillMenu(Player player) {
        SmpPlayer smpPlayer = PlayerManager.getInstance().getSmpPlayer(player);
        if (smpPlayer == null) return null;
        if (smpPlayer.getPlayerClass() != null) return new AbilityLoadoutGui(smpPlayer);
        player.sendMessage(Component.text("Bạn cần chọn một lớp nhân vật trước khi sử dụng kĩ năng!", NamedTextColor.YELLOW));
        return new ClassSelectionGui(smpPlayer);
    }

    private static ItemStack icon(Material material, String name, NamedTextColor color) {
        ItemStack item = ItemStack.of(material);
        item.setData(DataComponentTypes.ITEM_NAME, Component.text(name, color));
        return item;
    }
}
