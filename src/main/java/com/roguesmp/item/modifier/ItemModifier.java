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
 * registration order in {@link com.roguesmp.registry.ModifierRegistry}.
 * </p>
 */
public interface ItemModifier {
    void collectAndApply(SmpItem smpItem, @Nullable SmpPlayer player);
}
