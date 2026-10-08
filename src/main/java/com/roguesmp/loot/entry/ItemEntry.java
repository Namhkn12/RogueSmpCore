package com.roguesmp.loot.entry;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.codec.Codec;
import com.roguesmp.codec.DataResult;
import com.roguesmp.item.BaseItem;
import com.roguesmp.loot.LootEntry;
import com.roguesmp.loot.context.LootContext;
import com.roguesmp.registry.Holder;
import com.roguesmp.registry.Registries;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Drops a random amount of a single item in {@code [min, max]} - either a custom
 * {@link BaseItem} (referenced through a {@link Holder}, so a dangling id is reported by
 * {@code Registry#validateAllHolders} right after load) or a plain vanilla {@link Material}
 * ({@code "minecraft:"}-prefixed id, validated at decode time).
 */
public final class ItemEntry extends LootEntry {

    public static final String TYPE_KEY = "item";

    public static final Codec<ItemEntry> CODEC = Codec.composite(
            LootEntry.BASE_CODEC.forGetter(LootEntry::getBaseProperties),
            ItemRef.CODEC.fieldOf("item_id").forGetter(ItemEntry::getItemRef),
            Codec.INT.optionalFieldOf("min_amount", 1).forGetter(ItemEntry::getMinAmount),
            Codec.INT.optionalFieldOf("max_amount", 1).forGetter(ItemEntry::getMaxAmount),
            ItemEntry::new
    );

    public sealed interface ItemRef {

        String VANILLA_PREFIX = "minecraft:";

        Codec<ItemRef> CODEC = Codec.STRING.comapFlatMap(ItemRef::parse, ItemRef::id);

        String id();

        static DataResult<ItemRef> parse(String id) {
            if (!id.startsWith(VANILLA_PREFIX)) {
                return DataResult.success(new Custom(Registries.ITEM.getHolder(id)));
            }
            Material material = Material.matchMaterial(id);
            return material != null
                    ? DataResult.success(new Vanilla(id, material))
                    : DataResult.error("Unknown vanilla item_id: " + id);
        }

        record Vanilla(String id, Material material) implements ItemRef {}

        record Custom(Holder<BaseItem> holder) implements ItemRef {
            @Override
            public String id() {
                return holder.getId();
            }
        }
    }

    private final ItemRef itemRef;
    private final int minAmount;
    private final int maxAmount;

    public ItemEntry(@NotNull BaseProperties base, @NotNull ItemRef itemRef, int minAmount, int maxAmount) {
        super(base);
        this.itemRef = itemRef;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
    }

    public ItemEntry(@NotNull BaseProperties base, @NotNull String itemId, int minAmount, int maxAmount) {
        this(base, unwrap(ItemRef.parse(itemId)), minAmount, maxAmount);
    }

    private static ItemRef unwrap(DataResult<ItemRef> result) {
        if (!result.isSuccess()) throw new IllegalArgumentException(result.error());
        return result.result();
    }

    /**
     * Vanilla refs build a plain {@link ItemStack}, custom refs build the stack via the held
     * {@link BaseItem}. Either way, amount is a random value in [min, max].
     */
    @Override
    protected @NotNull List<ItemStack> generate(@NotNull LootContext context, int depth) {
        int amount = randomAmount();

        return switch (itemRef) {
            case ItemRef.Vanilla vanilla -> List.of(new ItemStack(vanilla.material(), amount));
            case ItemRef.Custom custom -> {
                if (!custom.holder().isBound()) {
                    RogueSmpCore.LOGGER.warn("[LootTable] Unknown item_id '" + custom.id() + "' — skipping.");
                    yield List.of();
                }
                yield List.of(custom.holder().value().generateItemStack(context.getPlayer(), amount));
            }
        };
    }

    private int randomAmount() {
        if (minAmount >= maxAmount) return Math.max(1, minAmount);
        return ThreadLocalRandom.current().nextInt(minAmount, maxAmount + 1);
    }

    @Override
    public @NotNull String getTypeId() {
        return TYPE_KEY;
    }

    public @NotNull ItemRef getItemRef() {
        return itemRef;
    }

    public @NotNull String getItemId() {
        return itemRef.id();
    }

    public int getMinAmount() {
        return minAmount;
    }

    public int getMaxAmount() {
        return maxAmount;
    }
}
