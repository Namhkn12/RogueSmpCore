package com.roguesmp.item.component;

import com.roguesmp.constant.EquipSlot;
import com.roguesmp.item.SmpItem;
import com.roguesmp.player.SmpPlayer;

public interface TickingComponent extends ItemComponent {
    /**
     * Run every {@code interval} when player have this item with this component equipped at {@code equipSlot}.
     * @param item The item with this component. Is guaranteed to be at {@code equipSlot}.
     */
    void tick(SmpPlayer player, SmpItem item, EquipSlot equipSlot, int interval);
}
