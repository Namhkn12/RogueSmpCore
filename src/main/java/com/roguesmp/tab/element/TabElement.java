package com.roguesmp.tab.element;

import com.roguesmp.tab.TabContext;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.function.Function;

/**
 * Defines how one scoreboard line or tablist slot renders. {@link #render} is the only required method.
 * Everything else is an optional hook, used when a line needs more than fixed text: reacting to
 * {@link TabContext} state, refreshing on a timer, or (tablist only) showing a custom head/ping.
 */
public interface TabElement {

    /** Returns the text to show for this element right now. */
    String render(TabContext context);

    /** Context keys this element reads. When one of these changes on the view's {@link TabContext}, only this element re-renders. */
    default Set<String> dependencies() {
        return Set.of();
    }

    /** How many server ticks between automatic re-renders, on top of context changes. {@code <= 0} means no automatic refresh. */
    default int refreshTicks() {
        return 0;
    }

    /**
     * Tablist only: the head/skin to show for this slot (e.g. {@code "player:Notch"}, {@code "mineskin:1234"},
     * {@code "texture:<base64>"}). {@code null} means no custom skin. A changed skin can't be pushed the same
     * cheap way text can — {@link com.roguesmp.tab.tablist.TabListView} has to resend the whole layout to
     * change it — so only make this return a different value when the head actually needs to change.
     */
    default @Nullable String skin(TabContext context) {
        return null;
    }

    /** Tablist only: the fake ping to show for this slot. {@code null} leaves it unset. Same resend cost as {@link #skin}. */
    default @Nullable Integer ping(TabContext context) {
        return null;
    }

    /** A plain element with no {@link TabContext} dependencies and no automatic refresh. */
    static TabElement of(Function<TabContext, String> renderer) {
        return new FunctionElement(renderer, Set.of(), 0);
    }

    /** Like {@link #of(Function)}, but re-renders whenever any of the given context keys change. */
    static TabElement of(Function<TabContext, String> renderer, String... dependencies) {
        return new FunctionElement(renderer, Set.of(dependencies), 0);
    }

    /** An element that re-renders automatically every {@code everyTicks} server ticks. */
    static TabElement ticking(int everyTicks, Function<TabContext, String> renderer) {
        return new FunctionElement(renderer, Set.of(), everyTicks);
    }

    /** An element that always renders the same fixed text. */
    static TabElement constant(String text) {
        return new FunctionElement(context -> text, Set.of(), 0);
    }

    /** Combines several elements into one, joining their rendered text with {@code separator}. Useful for a multi-line header/footer. */
    static TabElement join(String separator, TabElement... parts) {
        return new JoinedElement(separator, java.util.List.of(parts));
    }

    /** Same as {@link #join}, but joins with a newline — TAB shows {@code \n} in a header/footer as a line break. */
    static TabElement lines(TabElement... parts) {
        return join("\n", parts);
    }
}
