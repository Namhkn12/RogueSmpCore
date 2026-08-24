package com.roguesmp.item.modifier;

import com.roguesmp.item.SmpItem;
import com.roguesmp.player.SmpPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * Defines logic for cross-component interactions and dynamic scaling.
 * <p>
 * ItemModifiers are executed during the {@link SmpItem#applyModifiers(SmpPlayer)} phase.
 * They are responsible for calculating values that depend on multiple components
 * (e.g., Gems affecting Attributes) or external player data (e.g., Level scaling).
 * </p>
 * <p>
 * <strong>Execution Order:</strong> Modifiers are processed sequentially based on their
 * registration order in {@link ModifierRegistry}.
 * </p>
 */
public interface ItemModifier {
    /**
     * @param isPreview true for a display-only stack (GUI icon/preview) that should show
     *                   best-case values rather than an item-specific roll - not simply "player is
     *                   null", since plenty of real generation (loot drops with no attributable
     *                   killer, recipe-template items, block drops) also has no player and must
     *                   still use the item's actual rolled values.
     */
    void collectAndApply(SmpItem smpItem, @Nullable SmpPlayer player, boolean isPreview);
}
