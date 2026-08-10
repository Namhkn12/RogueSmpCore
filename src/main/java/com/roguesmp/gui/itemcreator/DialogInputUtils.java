package com.roguesmp.gui.itemcreator;

import org.jetbrains.annotations.Nullable;

/**
 * Shared parsing for the item creator's numeric text inputs (used instead of slider inputs so
 * players can type an exact value). Clamps to the field's old slider bounds so behavior matches
 * what the slider used to enforce.
 */
final class DialogInputUtils {

    private DialogInputUtils() {
    }

    static @Nullable Float parseFloat(@Nullable String text, float min, float max) {
        if (text == null || text.isBlank()) return null;
        try {
            float value = Float.parseFloat(text.trim());
            return Math.max(min, Math.min(max, value));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
