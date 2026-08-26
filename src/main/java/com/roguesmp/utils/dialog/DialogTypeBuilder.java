package com.roguesmp.utils.dialog;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.action.DialogActionCallback;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import io.papermc.paper.registry.set.RegistrySet;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.event.ClickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A builder class for creating different kinds of dialog layouts.
 */
public class DialogTypeBuilder {

    private static ClickCallback.Options defaultOptions() {
        return ClickCallback.Options.builder()
                .uses(1)
                .lifetime(ClickCallback.DEFAULT_LIFETIME)
                .build();
    }
    
    private static final int DEFAULT_BUTTON_WIDTH = 100;

    // ==========================================
    // 1. NOTICE LAYOUT BUILDER
    // ==========================================

    /**
     * Builder for a notice dialog, which displays a message and an optional button.
     */
    public static class Notice {
        private final DialogBase base;
        private ActionButton button = null;

        public Notice(DialogBase base) { this.base = base; }

        /**
         * Sets the button for the notice dialog with a default width of DEFAULT_BUTTON_WIDTH.
         *
         * @param label   the button text
         * @param handler the action to run when clicked
         * @return this builder
         */
        public Notice button(Component label, Component tooltip, DialogActionCallback handler) {
            return button(label, tooltip, DEFAULT_BUTTON_WIDTH, handler);
        }

        /**
         * Sets the button for the notice dialog with a specified width.
         *
         * @param label   the button text
         * @param width   the width of the button
         * @param handler the action to run when clicked
         * @return this builder
         */
        public Notice button(Component label, Component tooltip, int width, DialogActionCallback handler) {
            DialogAction action = handler != null ? DialogAction.customClick(handler, defaultOptions()) : null;
            this.button = ActionButton.create(label, tooltip, width, action);
            return this;
        }

        /**
         * Creates the notice dialog.
         *
         * @return the dialog instance
         */
        public Dialog build() {
            return Dialog.create(b -> b.empty()
                    .base(base)
                    .type(button == null ? DialogType.notice() : DialogType.notice(button))
            );
        }
    }

    // ==========================================
    // 2. CONFIRMATION LAYOUT BUILDER
    // ==========================================

    /**
     * Builder for a confirmation dialog, which displays two buttons (e.g., Yes and No).
     */
    public static class Confirmation {
        private final DialogBase base;
        private ActionButton confirmBtn;
        private ActionButton cancelBtn;

        public Confirmation(DialogBase base) { this.base = base; }

        /**
         * Sets the confirmation button with a default width of DEFAULT_BUTTON_WIDTH.
         *
         * @param label   the button text
         * @param tooltip the button tooltip, or null if not set
         * @param handler the action to run when clicked
         * @return this builder
         */
        public Confirmation yesButton(Component label, @Nullable Component tooltip, DialogActionCallback handler) {
            return yesButton(label, tooltip, DEFAULT_BUTTON_WIDTH, handler);
        }

        /**
         * Sets the confirmation button with a specified width.
         *
         * @param label   the button text
         * @param tooltip the button tooltip, or null if not set
         * @param width   the width of the button
         * @param handler the action to run when clicked
         * @return this builder
         */
        public Confirmation yesButton(Component label, @Nullable Component tooltip, int width, DialogActionCallback handler) {
            DialogAction action = handler != null ? DialogAction.customClick(handler, defaultOptions()) : null;
            this.confirmBtn = ActionButton.create(label, tooltip, width, action);
            return this;
        }

        /**
         * Sets the cancellation button with a default width of DEFAULT_BUTTON_WIDTH.
         *
         * @param label   the button text
         * @param tooltip the button tooltip, or null if not set
         * @param handler the action to run when clicked
         * @return this builder
         */
        public Confirmation noButton(Component label, @Nullable Component tooltip, DialogActionCallback handler) {
            return noButton(label, tooltip, DEFAULT_BUTTON_WIDTH, handler);
        }

        /**
         * Sets the cancellation button with a specified width.
         *
         * @param label   the button text
         * @param tooltip the button tooltip, or null if not set
         * @param width   the width of the button
         * @param handler the action to run when clicked
         * @return this builder
         */
        public Confirmation noButton(Component label, @Nullable Component tooltip, int width, DialogActionCallback handler) {
            DialogAction action = handler != null ? DialogAction.customClick(handler, defaultOptions()) : null;
            this.cancelBtn = ActionButton.create(label, tooltip, width, action);
            return this;
        }

        /**
         * Creates the confirmation dialog.
         *
         * @return the dialog instance
         */
        public Dialog build() {
            return Dialog.create(b -> b.empty()
                    .base(base)
                    .type(DialogType.confirmation(confirmBtn, cancelBtn))
            );
        }
    }

    // ==========================================
    // 3. MULTI-ACTION LAYOUT BUILDER
    // ==========================================

    /**
     * Builder for a multi-action dialog, which displays a grid of action buttons.
     */
    public static class MultiAction {
        private final DialogBase base;
        private final List<ActionButton> buttons = new ArrayList<>();
        private ActionButton exitButton = null;
        private int columns = 1;

        public MultiAction(DialogBase base) { this.base = base; }

        /**
         * Adds an action button to the dialog grid with a default width of DEFAULT_BUTTON_WIDTH.
         *
         * @param label   the button text
         * @param tooltip the button tooltip, or null if not set
         * @param handler the action to run when clicked
         * @return this builder
         */
        public MultiAction addButton(Component label, @Nullable Component tooltip, DialogActionCallback handler) {
            return addButton(label, tooltip, DEFAULT_BUTTON_WIDTH, handler);
        }

        /**
         * Adds an action button to the dialog grid with a specified width.
         *
         * @param label   the button text
         * @param tooltip the button tooltip, or null if not set
         * @param width   the width of the button
         * @param handler the action to run when clicked
         * @return this builder
         */
        public MultiAction addButton(Component label, @Nullable Component tooltip, int width, DialogActionCallback handler) {
            DialogAction action = handler != null ? DialogAction.customClick(handler, defaultOptions()) : null;
            this.buttons.add(ActionButton.create(label, tooltip, width, action));
            return this;
        }

        /**
         * Sets the number of columns in the grid.
         *
         * @param columns the number of columns
         * @return this builder
         */
        public MultiAction columns(int columns) {
            this.columns = Math.max(1, columns);
            return this;
        }

        /**
         * Sets the exit button for the dialog with a default width of DEFAULT_BUTTON_WIDTH.
         *
         * @param label   the exit button text
         * @param handler the action to run when clicked
         * @return this builder
         */
        public MultiAction exitButton(Component label, DialogActionCallback handler) {
            return exitButton(label, DEFAULT_BUTTON_WIDTH, handler);
        }

        /**
         * Sets the exit button for the dialog with a specified width.
         *
         * @param label   the exit button text
         * @param width   the width of the exit button
         * @param handler the action to run when clicked
         * @return this builder
         */
        public MultiAction exitButton(Component label, int width, DialogActionCallback handler) {
            DialogAction action = handler != null ? DialogAction.customClick(handler, defaultOptions()) : null;
            this.exitButton = ActionButton.create(label, null, width, action);
            return this;
        }

        /**
         * Creates the multi-action dialog.
         *
         * @return the dialog instance
         */
        public Dialog build() {
            return Dialog.create(b -> b.empty()
                    .base(base)
                    .type(DialogType.multiAction(buttons)
                            .columns(columns)
                            .exitAction(exitButton)
                            .build())
            );
        }
    }

    // ==========================================
    // 4. DIALOG LIST LAYOUT BUILDER
    // ==========================================

    /**
     * Builder for a dialog list dialog, which displays a list of sub-dialogs to navigate to.
     */
    public static class DialogList {
        private final DialogBase base;
        private final ArrayList<Dialog> dialogs = new ArrayList<>();
        private ActionButton exitButton = null;
        private int columns = 1;
        private int buttonWidth = DEFAULT_BUTTON_WIDTH;

        public DialogList(DialogBase base) {
            this.base = base;
        }

        /**
         * Sets the number of columns in the layout.
         *
         * @param columns the number of columns
         * @return this builder
         */
        public DialogList columns(int columns) {
            this.columns = Math.max(1, columns);
            return this;
        }

        /**
         * Sets the width of the buttons in the list.
         *
         * @param width the width of the buttons
         * @return this builder
         */
        public DialogList buttonWidth(int width) {
            this.buttonWidth = Math.min(1024, Math.max(1, width));
            return this;
        }

        /**
         * Sets the exit button for the dialog with a default width of DEFAULT_BUTTON_WIDTH.
         *
         * @param label   the exit button text
         * @param handler the action to run when clicked
         * @return this builder
         */
        public DialogList exitButton(Component label, DialogActionCallback handler) {
            return exitButton(label, DEFAULT_BUTTON_WIDTH, handler);
        }

        /**
         * Sets the exit button for the dialog with a specified width.
         *
         * @param label   the exit button text
         * @param width   the width of the exit button
         * @param handler the action to run when clicked
         * @return this builder
         */
        public DialogList exitButton(Component label, int width, DialogActionCallback handler) {
            DialogAction action = handler != null ? DialogAction.customClick(handler, defaultOptions()) : null;
            this.exitButton = ActionButton.create(label, null, width, action);
            return this;
        }

        /**
         * Add a dialog to the end of the list
         *
         * @param dialog Dialog to add
         * @return this builder
         */
        public DialogList addDialog(Dialog dialog) {
            this.dialogs.add(dialog);
            return this;
        }

        /**
         * Creates the dialog list dialog.
         *
         * @return the dialog instance
         */
        public Dialog build() {
            return Dialog.create(b -> b.empty()
                    .base(base)
                    .type(DialogType.dialogList(RegistrySet.valueSet(RegistryKey.DIALOG, dialogs))
                            .columns(columns)
                            .buttonWidth(buttonWidth)
                            .exitAction(exitButton)
                            .build())
            );
        }
    }

    // ==========================================
    // 5. SERVER LINKS LAYOUT BUILDER
    // ==========================================

    /**
     * Builder for a server links dialog, which displays the server's configured links.
     */
    public static class ServerLinks {
        private final DialogBase base;
        private ActionButton exitButton = null;
        private int columns = 1;
        private int buttonWidth = DEFAULT_BUTTON_WIDTH;

        public ServerLinks(DialogBase base) { this.base = base; }

        /**
         * Sets the number of columns in the layout.
         *
         * @param columns the number of columns
         * @return this builder
         */
        public ServerLinks columns(int columns) {
            this.columns = Math.max(1, columns);
            return this;
        }

        /**
         * Sets the width of the link buttons.
         *
         * @param width the width of the buttons
         * @return this builder
         */
        public ServerLinks buttonWidth(int width) {
            this.buttonWidth = Math.min(1024, Math.max(1, width));
            return this;
        }

        /**
         * Sets the exit button for the dialog with a default width of DEFAULT_BUTTON_WIDTH.
         *
         * @param label   the exit button text
         * @param handler the action to run when clicked
         * @return this builder
         */
        public ServerLinks exitButton(Component label, DialogActionCallback handler) {
            return exitButton(label, DEFAULT_BUTTON_WIDTH, handler);
        }

        /**
         * Sets the exit button for the dialog with a specified width.
         *
         * @param label   the exit button text
         * @param width   the width of the exit button
         * @param handler the action to run when clicked
         * @return this builder
         */
        public ServerLinks exitButton(Component label, int width, DialogActionCallback handler) {
            DialogAction action = handler != null ? DialogAction.customClick(handler, defaultOptions()) : null;
            this.exitButton = ActionButton.create(label, null, width, action);
            return this;
        }

        /**
         * Creates the server links dialog.
         *
         * @return the dialog instance
         */
        public Dialog build() {
            return Dialog.create(b -> b.empty()
                    .base(base)
                    .type(DialogType.serverLinks(exitButton, columns, buttonWidth))
            );
        }
    }
}
