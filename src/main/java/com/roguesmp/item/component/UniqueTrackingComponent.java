package com.roguesmp.item.component;

/**
 * Marker for {@link ItemComponent}s that carry meaningful per-instance runtime state (loaded
 * from/saved to an ItemStack's PDC, e.g. current durability, applied gems, player-added enchants).
 * <p>
 * A {@link com.roguesmp.item.BaseItem} holding at least one component implementing this is
 * automatically treated as unique: each generated {@link com.roguesmp.item.SmpItem} is assigned a
 * persistent UUID identity and tracked/cached by {@link com.roguesmp.item.ItemManager}, instead of
 * every item author having to declare uniqueness by hand.
 */
public interface UniqueTrackingComponent extends ItemComponent {
}
