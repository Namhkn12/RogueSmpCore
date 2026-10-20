package com.roguesmp.loot;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.codec.Codec;
import com.roguesmp.loot.context.LootContext;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

public class LootTable {
    public static final Codec<LootTable> CODEC = Codec.composite(
            Codec.listOf(LootPool.CODEC).fieldOf("pools").forGetter(LootTable::getPools),
            LootTable::new
    );

    private static final int MAX_DEPTH = 8;

    private final @NotNull List<LootPool> pools;

    public LootTable(@NotNull List<LootPool> pools) {
        this.pools = Collections.unmodifiableList(pools);
    }

    public @NotNull @Unmodifiable List<LootPool> getPools() {
        return pools;
    }

    public @NotNull List<ItemStack> roll(@NotNull LootContext context) {
        List<ItemStack> results = new ArrayList<>();
        rollInto(context, results, 0);
        return List.copyOf(results);
    }

    public void rollInto(@NotNull LootContext context, @NotNull List<ItemStack> results, int depth) {
        if (depth > MAX_DEPTH) {
            RogueSmpCore.LOGGER.warn("[LootTable] Max recursion depth reached — possible circular nested loot_table reference.");
            return;
        }

        for (LootPool pool : pools) {
            pool.roll(context, results, depth);
        }
    }

    public @NotNull LootOdds odds(@NotNull LootContext context) {
        return new LootOdds(IntStream.range(0, pools.size())
                .mapToObj(i -> new LootOdds.PoolOdds(i, pools.get(i).isActive(context), pools.get(i).odds(context)))
                .toList());
    }

    public static @NotNull Component rollMessage(@NotNull String id, @NotNull List<ItemStack> items) {
        Component summary = items.isEmpty()
                ? Component.text("Không có vật phẩm.", NamedTextColor.GRAY)
                : Component.join(JoinConfiguration.newlines(), items.stream()
                        .map(item -> Component.text(item.getAmount() + "x ", NamedTextColor.GRAY).append(item.effectiveName()))
                        .toList());
        return Utils.fromString("<green>Đã roll <yellow>" + id + "<green>: " + items.size() + " vật phẩm. <gray>(Di chuột để xem)")
                .hoverEvent(HoverEvent.showText(summary));
    }
}
