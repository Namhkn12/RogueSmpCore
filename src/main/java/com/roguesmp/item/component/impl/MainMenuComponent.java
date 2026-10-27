package com.roguesmp.item.component.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.context.ItemLoreContext;
import com.roguesmp.item.component.InteractableComponent;
import com.roguesmp.item.component.ItemComponent;
import com.roguesmp.menu.gui.MainMenuGui;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record MainMenuComponent() implements InteractableComponent {

    public static final Codec<MainMenuComponent> CODEC = Codec.unit(MainMenuComponent::new);

    private static final int LORE_PRIORITY = 950;

    @Override
    public @NotNull ItemComponent copy() {
        return this;
    }

    @Override
    public void contributeLore(ItemLoreContext context) {
        context.builder().putLines(LORE_PRIORITY, List.of(Utils.text("Chuột phải để mở menu", NamedTextColor.YELLOW)));
    }

    @Override
    public void onItemInteract(SmpPlayer smpPlayer, PlayerInteractEvent event) {
        if (!event.getAction().isRightClick()) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        new MainMenuGui(player).showInventory(player);
    }
}
