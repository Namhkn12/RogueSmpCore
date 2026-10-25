package com.roguesmp.player.inbox;

import com.roguesmp.codec.Codec;
import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * Where an inbox entry came from: {@code origin} is the system (e.g. "Bảng nhiệm vụ"), {@code name} the specific
 * thing inside it (e.g. the quest name).
 */
public record InboxSource(String origin, String name, Key icon) {

    public static final Codec<InboxSource> CODEC = Codec.composite(
            Codec.STRING.fieldOf("origin").forGetter(InboxSource::origin),
            Codec.STRING.fieldOf("name").forGetter(InboxSource::name),
            Codec.KEY.optionalFieldOf("model", Material.CHEST.key()).forGetter(InboxSource::icon),
            InboxSource::new
    );

    public ItemStack createIcon() {
        ItemStack stack = ItemStack.of(Material.PAPER);
        stack.setData(DataComponentTypes.ITEM_MODEL, icon);
        return stack;
    }
}
