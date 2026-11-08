package com.roguesmp.block.upgrade;

import com.roguesmp.block.StoredItem;
import com.roguesmp.block.persistence.StateSection;
import com.roguesmp.block.storage.FixedSlots;
import com.roguesmp.codec.MapCodec;
import com.roguesmp.item.component.impl.BlockUpgradeComponent;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public final class CatalystBurner {

    private static final MapCodec<Optional<ActiveCatalyst>> CODEC = ActiveCatalyst.CODEC.optionalFieldOf("active_catalyst");

    private final UpgradeHost host;
    private final FixedSlots feed;
    private final Runnable markDirty;
    private @Nullable ActiveCatalyst active;

    public CatalystBurner(UpgradeHost host, FixedSlots feed, Runnable markDirty) {
        this.host = host;
        this.feed = feed;
        this.markDirty = markDirty;
    }

    public static boolean isValid(UpgradeHost host, String itemId) {
        BlockUpgradeComponent upgrade = BlockUpgradeComponent.of(itemId);
        return upgrade != null && upgrade.duration() > 0 && BlockUpgradeComponent.check(itemId, host, UpgradeType.CATALYST) == UpgradeInstallResult.INSTALLED;
    }

    public boolean isValid(String itemId) {
        return isValid(host, itemId);
    }

    public void tick(boolean running) {
        StoredItem stored = active == null ? feed.get(0) : null;
        if (stored != null && isValid(stored.item())) {
            feed.extract(0, 1, false);
            set(new ActiveCatalyst(stored.item(), BlockUpgradeComponent.of(stored.item()).duration()));
        }
        if (running && unit() == DurationUnit.TICKS) countDown();
    }

    public void cycle() {
        if (unit() == DurationUnit.CYCLES) countDown();
    }

    public void revalidate() {
        if (active == null || isValid(active.item())) return;

        active = null;
        markDirty.run();
    }

    public void clear() {
        if (active != null) set(null);
    }

    public boolean isPaused(boolean running) {
        return active != null && unit() == DurationUnit.TICKS && !running;
    }

    public void collectItems(List<String> out) {
        if (active != null && isValid(active.item())) out.add(active.item());
    }

    public FixedSlots feed() {
        return feed;
    }

    public @Nullable ActiveCatalyst active() {
        return active;
    }

    public StateSection<?> section() {
        return StateSection.of(CODEC, () -> Optional.ofNullable(active), value -> active = value.orElse(null));
    }

    private @Nullable DurationUnit unit() {
        BlockUpgradeComponent component = active == null ? null : active.component();
        return component == null ? null : component.unit();
    }

    private void countDown() {
        if (active == null) return;
        if (active.countDown()) set(null);
        markDirty.run();
    }

    private void set(@Nullable ActiveCatalyst value) {
        active = value;
        host.upgrades().rebuild();
        markDirty.run();
    }
}
