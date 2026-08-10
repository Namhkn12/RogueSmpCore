package com.roguesmp.tab.tablist;

import com.roguesmp.tab.TabContext;
import com.roguesmp.tab.TabEngine;
import com.roguesmp.tab.TabView;
import com.roguesmp.tab.element.TabElement;
import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.TabPlayer;
import me.neznamy.tab.api.tablist.layout.Layout;
import me.neznamy.tab.api.tablist.layout.LayoutManager;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * A per-player custom TAB tablist layout. {@link Layout} has no per-slot update method once sent, so any
 * change rebuilds the whole layout from the current bindings and resends it — but at most once every
 * {@value #RESEND_INTERVAL_TICKS} ticks, since players rarely have the tablist open and resending it
 * recreates every entry client-side, which flickers if done too often.
 */
public final class TabListView implements TabView {

    private static final int RESEND_INTERVAL_TICKS = 20;

    private record SlotSnapshot(int slotId, String text, @Nullable String skin, @Nullable Integer ping) {
    }

    private final class Binding {
        final int slotId;
        TabElement element;
        final String skin;
        final Integer ping;
        int ticksSinceRefresh;
        final List<Runnable> unbindTasks = new ArrayList<>();

        Binding(int slotId, TabElement element, String skin, Integer ping) {
            this.slotId = slotId;
            this.element = element;
            this.skin = skin;
            this.ping = ping;
            bind();
        }

        void bind() {
            for (String dep : element.dependencies()) {
                Consumer<TabContext> listener = ctx -> dirty = true;
                context.onChange(dep, listener);
                unbindTasks.add(() -> context.removeListener(dep, listener));
            }
        }

        void unbind() {
            unbindTasks.forEach(Runnable::run);
            unbindTasks.clear();
        }

        void replace(TabElement newElement) {
            unbind();
            this.element = newElement;
            bind();
        }
    }

    private final Player player;
    private final TabPlayer tabPlayer;
    private final TabContext context;
    private final String layoutName;
    private final Map<Integer, Binding> slots = new LinkedHashMap<>();
    private final Map<String, int[]> groups = new LinkedHashMap<>();
    private TabElement header;
    private TabElement footer;
    private boolean shown;
    private boolean dirty;
    private int ticksSinceResend;

    private TabListView(Player player, String name, TabContext context) {
        this.player = player;
        this.layoutName = name + "_" + player.getUniqueId();
        this.context = context;
        this.tabPlayer = TabEngine.tabPlayer(player);
        if (tabPlayer == null) {
            throw new IllegalStateException("Player " + player.getName() + " is not yet loaded by TAB; check TabEngine.isReady() first.");
        }
        TabEngine.getInstance().register(this);
    }

    public static TabListView create(Player player, String name, TabContext context) {
        return new TabListView(player, name, context);
    }

    public TabListView slot(int id, TabElement element) {
        return slot(id, element, null, null);
    }

    public TabListView slot(int id, TabElement element, String skin, Integer ping) {
        Binding existing = slots.get(id);
        if (existing != null) {
            existing.replace(element);
        } else {
            slots.put(id, new Binding(id, element, skin, ping));
        }
        dirty = true;
        return this;
    }

    /** Passthrough to TAB's native rotating slot groups (e.g. for a scrolling online-player list). */
    public TabListView group(String groupName, int[] slotIds) {
        groups.put(groupName, slotIds);
        dirty = true;
        return this;
    }

    public TabListView header(TabElement element) {
        this.header = element;
        dirty = true;
        return this;
    }

    public TabListView footer(TabElement element) {
        this.footer = element;
        dirty = true;
        return this;
    }

    public void show() {
        shown = true;
        resend();
        dirty = false;
        ticksSinceResend = 0;
    }

    @Override
    public UUID playerId() {
        return player.getUniqueId();
    }

    @Override
    public void tick() {
        ticksSinceResend++;
        for (Binding binding : slots.values()) {
            int interval = binding.element.refreshTicks();
            if (interval <= 0) continue;
            if (++binding.ticksSinceRefresh >= interval) {
                binding.ticksSinceRefresh = 0;
                dirty = true;
            }
        }
        if (shown && dirty && ticksSinceResend >= RESEND_INTERVAL_TICKS) {
            resend();
            dirty = false;
            ticksSinceResend = 0;
        }
    }

    @Override
    public void destroy() {
        TabEngine.getInstance().unregister(this);
        slots.values().forEach(Binding::unbind);
        if (shown) {
            Objects.requireNonNull(TabAPI.getInstance().getLayoutManager()).resetLayout(tabPlayer);
        }
    }

    private void resend() {
        LayoutManager lm = Objects.requireNonNull(TabAPI.getInstance().getLayoutManager());
        Layout layout = lm.createNewLayout(layoutName);
        for (Binding binding : slots.values()) {
            addFixedSlot(layout, new SlotSnapshot(binding.slotId, binding.element.render(context), binding.skin, binding.ping));
        }
        groups.forEach(layout::addGroup);
        lm.sendLayout(tabPlayer, layout);

        if (header != null) {
            Objects.requireNonNull(TabAPI.getInstance().getHeaderFooterManager()).setHeader(tabPlayer, header.render(context));
        }
        if (footer != null) {
            Objects.requireNonNull(TabAPI.getInstance().getHeaderFooterManager()).setFooter(tabPlayer, footer.render(context));
        }
    }

    private static void addFixedSlot(Layout layout, SlotSnapshot slot) {
        if (slot.skin() != null && !slot.skin().isEmpty() && slot.ping() != null) {
            layout.addFixedSlot(slot.slotId(), slot.text(), slot.skin(), slot.ping());
        } else if (slot.skin() != null && !slot.skin().isEmpty()) {
            layout.addFixedSlot(slot.slotId(), slot.text(), slot.skin());
        } else if (slot.ping() != null) {
            layout.addFixedSlot(slot.slotId(), slot.text(), slot.ping());
        } else {
            layout.addFixedSlot(slot.slotId(), slot.text());
        }
    }
}
