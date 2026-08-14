package com.roguesmp.gui.entitycreator;

import org.jetbrains.annotations.Nullable;

/**
 * Shared parsing for the entity creator's numeric text inputs (used instead of slider inputs so
 * players can type an exact value). Mirrors {@code gui.itemcreator.DialogInputUtils}.
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
