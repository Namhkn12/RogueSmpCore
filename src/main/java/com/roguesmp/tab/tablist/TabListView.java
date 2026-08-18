package com.roguesmp.tab.tablist;

import com.roguesmp.tab.TabContext;
import com.roguesmp.tab.TabEngine;
import com.roguesmp.tab.TabView;
import com.roguesmp.tab.element.TabElement;
import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.TabPlayer;
import me.neznamy.tab.api.placeholder.PlayerPlaceholder;
import me.neznamy.tab.api.tablist.layout.Layout;
import me.neznamy.tab.api.tablist.layout.LayoutManager;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * A per-player custom TAB tablist layout, made of numbered slots plus an optional header and footer.
 * <p>
 * TAB's {@link Layout} can only be built once and sent — there's no way to change one slot's text after
 * it's been sent, short of resending the whole layout, which redraws every entry on the client and looks
 * bad if done often. To get around that, every slot number shares one process-lifetime TAB placeholder
 * (registered the first time that slot is used, by any view). The placeholder looks up whichever
 * {@link TabListView} is currently active for the viewer and renders that view's element for that slot.
 * Updating a slot's text then just pushes a new placeholder value, without touching the layout at all.
 * A full resend ({@link #sendFullLayout()}) only happens for structural changes: {@link #show()} itself,
 * adding a slot/group id that wasn't part of the layout yet, or a slot's skin/ping changing (those are
 * baked into the layout and can't be pushed like text can).
 */
public final class TabListView implements TabView {

    private static final Map<Integer, PlayerPlaceholder> SLOT_PLACEHOLDERS = new ConcurrentHashMap<>();
    private static final Map<UUID, TabListView> ACTIVE = new ConcurrentHashMap<>();

    /** Gets or creates the shared placeholder for a slot number. Every view using that slot reuses the same one. */
    private static PlayerPlaceholder slotPlaceholder(int slotId) {
        return SLOT_PLACEHOLDERS.computeIfAbsent(slotId, id ->
                // -1 = manual refresh only.
                Objects.requireNonNull(TabAPI.getInstance().getPlaceholderManager())
                        .registerPlayerPlaceholder("%roguesmp_tabslot_" + id + "%", -1, tp -> resolveSlot(tp, id)));
    }

    /** Renders slot {@code slotId} for whichever view is currently showing for {@code viewer}. Runs whenever TAB needs the placeholder's value. */
    private static String resolveSlot(TabPlayer viewer, int slotId) {
        TabListView view = ACTIVE.get(viewer.getUniqueId());
        if (view == null) return "";
        Binding binding = view.slots.get(slotId);
        return binding == null ? "" : binding.element.render(view.context);
    }

    /** Ties one {@link TabElement} to a slot number, its shared placeholder, and the context listeners that keep it updated. */
    private final class Binding {
        final int slotId;
        TabElement element;
        final PlayerPlaceholder placeholder;
        // What was actually baked into the last sent Layout — unlike text, skin/ping can't be pushed
        // incrementally, so push() compares against these to know whether a full resend is required.
        String lastSkin;
        Integer lastPing;
        int ticksSinceRefresh;
        final List<Runnable> unbindTasks = new ArrayList<>();

        Binding(int slotId, TabElement element) {
            this.slotId = slotId;
            this.element = element;
            this.placeholder = slotPlaceholder(slotId);
            bind();
        }

        void bind() {
            for (String dep : element.dependencies()) {
                Consumer<TabContext> listener = ctx -> push(this);
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

    /** Same reactive wiring as {@link Binding}, minus the slot/placeholder machinery — used for header and footer. */
    private final class TextBinding {
        TabElement element;
        final Consumer<String> push;
        int ticksSinceRefresh;
        final List<Runnable> unbindTasks = new ArrayList<>();

        TextBinding(TabElement element, Consumer<String> push) {
            this.element = element;
            this.push = push;
            bind();
        }

        void bind() {
            for (String dep : element.dependencies()) {
                Consumer<TabContext> listener = ctx -> pushNow();
                context.onChange(dep, listener);
                unbindTasks.add(() -> context.removeListener(dep, listener));
            }
        }

        void pushNow() {
            push.accept(element.render(context));
            ticksSinceRefresh = 0;
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
    private TextBinding headerBinding;
    private TextBinding footerBinding;
    private boolean shown;

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

    /** Creates a tablist view for {@code player}. Throws if TAB hasn't loaded the player yet — check {@link TabEngine#isReady} first. */
    public static TabListView create(Player player, String name, TabContext context) {
        return new TabListView(player, name, context);
    }

    /** Sets the element for slot {@code id}, with no custom skin or ping. */
    public TabListView slot(int id, TabElement element) {
        Binding existing = slots.get(id);
        if (existing != null) {
            existing.replace(element);
            if (shown) push(existing);
            return this;
        }
        slots.put(id, new Binding(id, element));
        // A new slot id changes the layout's structure, so (unlike a plain content update) this needs
        // one full resend to actually add the slot — rare/one-off, so no need to worry about flicker here.
        if (shown) sendFullLayout();
        return this;
    }

    /** Reserves a range of slots for TAB's own player group (e.g. a scrolling online-player list). TAB fills it automatically. */
    public TabListView group(String groupCondition, int[] slotIds) {
        groups.put(groupCondition, slotIds);
        if (shown) sendFullLayout();
        return this;
    }

    /** Sets the header element. Can be swapped later by calling this again. */
    public TabListView header(TabElement element) {
        Consumer<String> push = text -> Objects.requireNonNull(TabAPI.getInstance().getHeaderFooterManager()).setHeader(tabPlayer, text);
        if (headerBinding == null) {
            headerBinding = new TextBinding(element, push);
        } else {
            headerBinding.replace(element);
        }
        if (shown) headerBinding.pushNow();
        return this;
    }

    /** Sets the footer element. Can be swapped later by calling this again. */
    public TabListView footer(TabElement element) {
        Consumer<String> push = text -> Objects.requireNonNull(TabAPI.getInstance().getHeaderFooterManager()).setFooter(tabPlayer, text);
        if (footerBinding == null) {
            footerBinding = new TextBinding(element, push);
        } else {
            footerBinding.replace(element);
        }
        if (shown) footerBinding.pushNow();
        return this;
    }

    /** Sends the layout to the player for the first time. Call this once, after adding slots/groups/header/footer. */
    public void show() {
        ACTIVE.put(player.getUniqueId(), this);
        shown = true;
        sendFullLayout();

        if (headerBinding != null) headerBinding.pushNow();
        if (footerBinding != null) footerBinding.pushNow();
    }

    private void push(Binding binding) {
        String skin = binding.element.skin(context);
        Integer ping = binding.element.ping(context);
        if (!Objects.equals(skin, binding.lastSkin) || !Objects.equals(ping, binding.lastPing)) {
            // Skin/ping are baked into the Layout at send time, not substitutable through the placeholder
            // like text is — the only way to change either is a full resend. Rare in practice (only when
            // what a slot represents actually changes, e.g. a different player now occupying it).
            sendFullLayout();
            return;
        }
        binding.placeholder.updateValue(tabPlayer, binding.element.render(context));
        binding.ticksSinceRefresh = 0;
    }

    private void sendFullLayout() {
        LayoutManager lm = Objects.requireNonNull(TabAPI.getInstance().getLayoutManager());
        Layout layout = lm.createNewLayout(layoutName);
        for (Binding binding : slots.values()) {
            addFixedSlot(layout, binding);
        }
        groups.forEach(layout::addGroup);
        lm.sendLayout(tabPlayer, layout);
    }

    private void addFixedSlot(Layout layout, Binding binding) {
        String text = "%roguesmp_tabslot_" + binding.slotId + "%";
        String skin = binding.element.skin(context);
        Integer ping = binding.element.ping(context);
        binding.lastSkin = skin;
        binding.lastPing = ping;

        if (skin != null && !skin.isEmpty() && ping != null) {
            layout.addFixedSlot(binding.slotId, text, skin, ping);
        } else if (skin != null && !skin.isEmpty()) {
            layout.addFixedSlot(binding.slotId, text, skin);
        } else if (ping != null) {
            layout.addFixedSlot(binding.slotId, text, ping);
        } else {
            layout.addFixedSlot(binding.slotId, text);
        }
    }

    @Override
    public UUID playerId() {
        return player.getUniqueId();
    }

    @Override
    public void tick() {
        for (Binding binding : slots.values()) {
            int interval = binding.element.refreshTicks();
            if (interval <= 0) continue;
            if (++binding.ticksSinceRefresh >= interval) {
                push(binding);
            }
        }
        tickText(headerBinding);
        tickText(footerBinding);
    }

    private void tickText(TextBinding binding) {
        if (binding == null) return;
        int interval = binding.element.refreshTicks();
        if (interval <= 0) return;
        if (++binding.ticksSinceRefresh >= interval) {
            binding.pushNow();
        }
    }

    /** Stops updating this view and, if it was shown, resets the layout and restores the library default header/footer. */
    @Override
    public void destroy() {
        TabEngine.getInstance().unregister(this);
        ACTIVE.remove(player.getUniqueId(), this);
        slots.values().forEach(Binding::unbind);
        if (headerBinding != null) headerBinding.unbind();
        if (footerBinding != null) footerBinding.unbind();
        if (shown) {
            Objects.requireNonNull(TabAPI.getInstance().getLayoutManager()).resetLayout(tabPlayer);
            // TAB has no "unset header/footer" call — only restore what this view actually touched, so we
            // don't stomp on header/footer some other system might be managing independently of this view.
            if (headerBinding != null) {
                Objects.requireNonNull(TabAPI.getInstance().getHeaderFooterManager()).setHeader(tabPlayer, TabEngine.defaultHeader(player));
            }
            if (footerBinding != null) {
                Objects.requireNonNull(TabAPI.getInstance().getHeaderFooterManager()).setFooter(tabPlayer, TabEngine.defaultFooter(player));
            }
        }
    }
}
