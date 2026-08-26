package com.roguesmp.gui;

import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

import java.util.Objects;

/**
 * A {@link BaseGui} that redraws itself from a single state object {@code S}, for GUIs where players can freely
 * place/drag items directly into GUI slots - e.g. a crafting grid or
 * fusion pedestals. Bukkit's click/drag events is finicky and can fire before the item has actually landed in the slot,
 * so reading it back immediately still sees stale contents, that's why this class re-checks a tick later instead.
 * <p>
 * Flow: any click/drag schedules {@link #syncStateInventory()} for next tick, which calls
 * {@link #computeState()} to derive a fresh {@code S} from the now-settled inventory. That's handed to
 * {@link #updateState(Object)}, which skips the redraw if {@link #isStateEqual(Object, Object)} says
 * nothing changed, otherwise caches it and calls {@link #render(Object)} right away - no further
 * delay, since the settling tick has already passed by this point.
 *
 * @param <S> The state type - a small, value-comparable object (a record is the natural fit).
 */
public abstract class ReactiveGui<S> extends BaseGui {

    private S state;

    public ReactiveGui(Component name, int row) {
        super(name, row);
    }

    protected S getState() {
        return state;
    }

    /**
     * Set the initial state directly, without triggering a redraw - use this from a subclass
     * constructor, before the gui is ever shown, instead of {@link #updateState(Object)}.
     */
    protected void initState(S initialState) {
        this.state = initialState;
    }

    /**
     * Recompute state from the current inventory contents (e.g. re-match a recipe against whatever
     * is currently sitting in the grid slots). Called by {@link #syncStateInventory()}.
     * <p>
     * This may be called every time a raw click/drag happens (on any slot in the inventory view), so if deriving {@code S} is expensive,
     * short-circuit here: keep a cheap fingerprint (e.g. a content hash) on {@code S} itself, compare
     * it against {@link #getState()} first, and only do the expensive work when it actually differs.
     */
    protected abstract S computeState();

    /**
     * (Re)draw the gui's buttons/decorations for the given state. Called on the initial
     * {@link #setup()} and again whenever the state actually changes (per {@link #isStateEqual}).
     */
    protected abstract void render(S state);

    @Override
    public final void setup() {
        render(state);
    }

    /**
     * Whether {@code oldState} and {@code newState} should be treated as the same state, i.e.
     * whether a redraw can be skipped. Defaults to {@link Objects#equals(Object, Object)}
     * (structural equality, which is what a record's generated {@code equals()} gives you).
     * <p>
     * Override this when the default is wrong or wasteful for your {@code S} - e.g. to compare only
     * a subset of fields that actually affect rendering, to fall back to reference equality when a
     * field's {@code equals()} is expensive or absent (see {@code CraftingGui}/{@code FusionGui},
     * whose recipe fields have no {@code equals()} override and so already compare by reference),
     * or to always redraw ({@code return false}) for a state that should never be considered stale.
     */
    protected boolean isStateEqual(S oldState, S newState) {
        return Objects.equals(oldState, newState);
    }

    /**
     * Update the Gui state, redrawing immediately if the newState is not equal to currentState.
     * Only call this from an already-deferred context (see {@link #syncStateInventory()}) - it does
     * not itself defer, since by the time state is being synced the item-not-landed-yet tick has
     * already passed.
     * @param newState
     */
    public void updateState(S newState) {
        if (isStateEqual(state, newState)) return;
        state = newState;
        setup();
    }

    /**
     * Sync the current state with the current inventory contents - recomputes it via
     * {@link #computeState()} and, if that's actually different from the cached state,
     * redraws (see {@link #updateState(Object)}).
     */
    public void syncStateInventory() {
        updateState(computeState());
    }

    @Override
    public void onClickTopInventory(InventoryClickEvent event) {
        super.onClickTopInventory(event);
        Utils.runLater(this::syncStateInventory);
    }

    @Override
    public void onClickBottomInventory(InventoryClickEvent event) {
        Utils.runLater(this::syncStateInventory);
    }

    @Override
    public void onDragInventory(InventoryDragEvent event) {
        Utils.runLater(this::syncStateInventory);
    }
}
