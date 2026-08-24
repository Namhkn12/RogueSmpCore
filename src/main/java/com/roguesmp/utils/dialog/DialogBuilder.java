package com.roguesmp.utils.dialog;

import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.body.ItemDialogBody;
import io.papermc.paper.registry.data.dialog.body.PlainMessageDialogBody;
import io.papermc.paper.registry.data.dialog.input.*;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A builder class providing a fluent API to configure base dialog settings, content bodies, and inputs.
 * <p>
 *     Default DialogAfterAction is initially NONE, and pause = false so that mouse don't flicker between dialogs.
 * </p>
 */
public class DialogBuilder {
    private final Component title;
    private Component externalTitle = null;
    private DialogBase.DialogAfterAction afterAction = DialogBase.DialogAfterAction.NONE;
    private boolean closeWithEscape = true;

    private final List<DialogBody> bodies = new ArrayList<>();
    private final List<DialogInput> inputs = new ArrayList<>();

    private DialogBuilder(Component title) {
        this.title = title;
    }

    /**
     * Creates a new dialog builder instance.
     *
     * @param title the primary title of the dialog
     * @return a new builder
     */
    public static DialogBuilder create(Component title) {
        return new DialogBuilder(title);
    }

    /**
     * Sets whether the dialog can be closed with the Escape key.
     *
     * @param allow true if it can be closed, false otherwise
     * @return this builder
     */
    public DialogBuilder canCloseWithEscape(boolean allow) {
        this.closeWithEscape = allow;
        return this;
    }

    /**
     * Sets the external title of the dialog, used when this dialog is opened from another button.
     *
     * @param externalTitle the external title
     * @return this builder
     */
    public DialogBuilder externalTitle(Component externalTitle) {
        this.externalTitle = externalTitle;
        return this;
    }

    /**
     * Sets the action to run after the dialog is closed.
     *
     * @param action the action to run
     * @return this builder
     */
    public DialogBuilder afterAction(DialogBase.DialogAfterAction action) {
        this.afterAction = action;
        return this;
    }

    // ==========================================
    // BODY ELEMENT APPENDERS
    // ==========================================

    /**
     * Adds a plain message body to the dialog.
     *
     * @param text the contents of the message
     * @return this builder
     */
    public DialogBuilder addTextBody(Component text) {
        this.bodies.add(DialogBody.plainMessage(text));
        return this;
    }

    /**
     * Adds a plain message body with a specific width to the dialog.
     *
     * @param text  the contents of the message
     * @param width the width of the message body
     * @return this builder
     */
    public DialogBuilder addTextBody(Component text, int width) {
        this.bodies.add(DialogBody.plainMessage(text, width));
        return this;
    }

    /**
     * Adds a basic item body to the dialog.
     *
     * @param item the item to display
     * @return this builder
     */
    public DialogBuilder addItemBody(ItemStack item) {
        this.bodies.add(DialogBody.item(item).build());
        return this;
    }

    /**
     * Adds a fully configured item body to the dialog.
     *
     * @param item            the item to display in the dialog
     * @param description       the description of the body, or null if not set
     * @param showDecorations   whether to show decorations around the item
     * @param showTooltip       whether to show a tooltip for the item
     * @param width             the width of the item body
     * @param height            the height of the item body
     * @return this builder
     */
    public DialogBuilder addItemBody(ItemStack item, @Nullable PlainMessageDialogBody description, boolean showDecorations, boolean showTooltip, int width, int height) {
        this.bodies.add(DialogBody.item(item, description, showDecorations, showTooltip, width, height));
        return this;
    }

    /**
     * Adds an item body configured via a builder lambda.
     *
     * @param item        the item to display
     * @param configurator the builder consumer
     * @return this builder
     */
    public DialogBuilder addItemBody(ItemStack item, Consumer<ItemDialogBody.Builder> configurator) {
        ItemDialogBody.Builder builder = DialogBody.item(item);
        configurator.accept(builder);
        this.bodies.add(builder.build());
        return this;
    }

    // ==========================================
    // INPUT COMPONENT INJECTORS
    // ==========================================

    /**
     * Adds a standard text input field to the dialog.
     *
     * @param id    the key identifier for the input
     * @param label the label for the input
     * @return this builder
     */
    public DialogBuilder addTextInput(String id, Component label) {
        this.inputs.add(DialogInput.text(id, label).build());
        return this;
    }

    /**
     * Adds a fully configured text input field to the dialog.
     *
     * @param id               the key identifier for the input
     * @param label            the label for the input
     * @param width            the width of the input
     * @param labelVisible     whether the label should be visible
     * @param initial          the initial value of the input
     * @param maxLength        the maximum length of the input
     * @param multilineOptions the multiline options, or null if not set
     * @return this builder
     */
    public DialogBuilder addTextInput(String id, Component label, int width, boolean labelVisible, String initial, int maxLength, TextDialogInput.@Nullable MultilineOptions multilineOptions) {
        this.inputs.add(DialogInput.text(id, width, label, labelVisible, initial, maxLength, multilineOptions));
        return this;
    }

    /**
     * Adds a text input field configured via its internal builder lambda.
     *
     * @param id           the key identifier for the input
     * @param label        the label for the input
     * @param configurator the builder consumer
     * @return this builder
     */
    public DialogBuilder addTextInput(String id, Component label, Consumer<TextDialogInput.Builder> configurator) {
        TextDialogInput.Builder builder = DialogInput.text(id, label);
        configurator.accept(builder);
        this.inputs.add(builder.build());
        return this;
    }

    /**
     * Adds a basic boolean checkbox input to the dialog.
     *
     * @param id    the key identifier for the input
     * @param label the label for the input
     * @return this builder
     */
    public DialogBuilder addCheckboxInput(String id, Component label) {
        this.inputs.add(DialogInput.bool(id, label).build());
        return this;
    }

    /**
     * Adds a fully configured boolean checkbox input to the dialog.
     *
     * @param id      the key identifier for the input
     * @param label   the label for the input
     * @param initial the initial value of the input
     * @param onTrue  the input's value in a template when the value is true
     * @param onFalse the input's value in a template when the value is false
     * @return this builder
     */
    public DialogBuilder addCheckboxInput(String id, Component label, boolean initial, String onTrue, String onFalse) {
        this.inputs.add(DialogInput.bool(id, label, initial, onTrue, onFalse));
        return this;
    }

    /**
     * Adds a boolean checkbox input configured via its internal builder lambda.
     *
     * @param id           the key identifier for the input
     * @param label        the label for the input
     * @param configurator the builder consumer
     * @return this builder
     */
    public DialogBuilder addCheckboxInput(String id, Component label, Consumer<BooleanDialogInput.Builder> configurator) {
        BooleanDialogInput.Builder builder = DialogInput.bool(id, label);
        configurator.accept(builder);
        this.inputs.add(builder.build());
        return this;
    }

    /**
     * Adds a standard slider number range input to the dialog.
     *
     * @param id      the key identifier for the input
     * @param label   the label for the input
     * @param min     the start of the range
     * @param max     the end of the range
     * @param step    the step size
     * @param initial the initial value
     * @param width   the width of the input
     * @return this builder
     */
    public DialogBuilder addSliderInput(String id, Component label, float min, float max, float step, float initial, int width) {
        this.inputs.add(DialogInput.numberRange(id, label, min, max).step(step).initial(initial).width(width).build());
        return this;
    }

    /**
     * Adds a fully configured slider number range input to the dialog.
     *
     * @param id          the key identifier for the input
     * @param width       the width of the input
     * @param label       the label for the input
     * @param labelFormat the format for the label (a translation key or format string)
     * @param start       the start of the range
     * @param end         the end of the range
     * @param initial     the initial value, or null if not set
     * @param step        the step size, or null if not set
     * @return this builder
     */
    public DialogBuilder addSliderInput(String id, int width, Component label, String labelFormat, float start, float end, @Nullable Float initial, @Nullable Float step) {
        this.inputs.add(DialogInput.numberRange(id, width, label, labelFormat, start, end, initial, step));
        return this;
    }

    /**
     * Adds a slider number range input configured via its internal builder lambda.
     *
     * @param id           the key identifier for the input
     * @param label        the label for the input
     * @param start        the start of the range
     * @param end          the end of the range
     * @param configurator the builder consumer
     * @return this builder
     */
    public DialogBuilder addSliderInput(String id, Component label, float start, float end, Consumer<NumberRangeDialogInput.Builder> configurator) {
        NumberRangeDialogInput.Builder builder = DialogInput.numberRange(id, label, start, end);
        configurator.accept(builder);
        this.inputs.add(builder.build());
        return this;
    }

    /**
     * Adds a radio (single option) input configured via its internal builder lambda.
     *
     * @param id           the key identifier for the input
     * @param label        the label for the input
     * @param entries      the list of options for the input
     * @param configurator the builder consumer
     * @return this builder
     */
    public DialogBuilder addSingleOptionInput(String id, Component label, List<SingleOptionDialogInput.OptionEntry> entries, Consumer<SingleOptionDialogInput.Builder> configurator) {
        SingleOptionDialogInput.Builder builder = DialogInput.singleOption(id, label, entries);
        configurator.accept(builder);
        this.inputs.add(builder.build());
        return this;
    }

    // ==========================================
    // DECOUPLING HANDOFF LAYOUT TARGETS
    // ==========================================

    public DialogTypeBuilder.Notice notice() {
        return new DialogTypeBuilder.Notice(this.buildBase());
    }

    public DialogTypeBuilder.Confirmation confirmation() {
        return new DialogTypeBuilder.Confirmation(this.buildBase());
    }

    public DialogTypeBuilder.MultiAction multiAction() {
        return new DialogTypeBuilder.MultiAction(this.buildBase());
    }

    public DialogTypeBuilder.DialogList dialogList() {
        return new DialogTypeBuilder.DialogList(this.buildBase());
    }

    public DialogTypeBuilder.ServerLinks serverLinks() {
        return new DialogTypeBuilder.ServerLinks(this.buildBase());
    }

    private DialogBase buildBase() {
        DialogBase.Builder baseBuilder = DialogBase.builder(title)
                .pause(false)
                .canCloseWithEscape(closeWithEscape)
                .body(bodies)
                .inputs(inputs);
        if (externalTitle != null) baseBuilder.externalTitle(externalTitle);
        baseBuilder.afterAction(afterAction);
        return baseBuilder.build();
    }
}
