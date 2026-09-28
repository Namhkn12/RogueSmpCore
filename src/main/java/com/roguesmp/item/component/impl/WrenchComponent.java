package com.roguesmp.item.component.impl;

import com.roguesmp.block.SmpBlock;
import com.roguesmp.block.manager.BlockManager;
import com.roguesmp.codec.Codec;
import com.roguesmp.codec.MapCodec;
import com.roguesmp.item.component.InteractableComponent;
import com.roguesmp.item.component.ItemComponent;
import com.roguesmp.player.SmpPlayer;
import org.bukkit.block.Block;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Shift + right-click quick-breaks a custom {@link SmpBlock}.
 */
public class WrenchComponent implements InteractableComponent {

    public static final Codec<WrenchComponent> CODEC = MapCodec.unit(WrenchComponent::new).codec();

    @Override
    public @NotNull ItemComponent copy() {
        return this;
    }

    @Override
    public void onItemInteract(SmpPlayer smpPlayer, PlayerInteractEvent event) {
        Block clicked = event.getClickedBlock();
        if (clicked == null) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || !event.getPlayer().isSneaking()) return;

        BlockManager manager = BlockManager.getInstance();
        if (!manager.isSmpBlock(clicked.getLocation())) return;

        event.setCancelled(true);
        manager.breakBlock(clicked, event.getPlayer());
    }
}
