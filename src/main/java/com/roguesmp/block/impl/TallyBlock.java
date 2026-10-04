package com.roguesmp.block.impl;

import com.roguesmp.block.BlockProperties;
import com.roguesmp.block.SmpBlock;
import com.roguesmp.block.persistence.StateSection;
import com.roguesmp.codec.Codec;
import net.kyori.adventure.text.Component;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.List;

/**
 * Example of a stateful block: right-click counts up, and the count persists.
 */
public class TallyBlock extends SmpBlock {

    private int count = 0;

    public TallyBlock(BlockProperties properties) {
        super(properties);
    }

    @Override
    protected void collectSections(List<StateSection<?>> sections) {
        super.collectSections(sections);
        sections.add(StateSection.of(Codec.INT.optionalFieldOf("count", 0), () -> count, value -> count = value));
    }

    @Override
    public void onBlockInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getPlayer().isSneaking()) return;

        event.setCancelled(true);
        count++;
        markDirty();
        event.getPlayer().sendActionBar(Component.text("Tally: " + count));
    }
}
