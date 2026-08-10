package com.roguesmp.tab.element;

import com.roguesmp.tab.TabContext;

import java.util.Set;
import java.util.function.Function;

/**
 * A single renderable slot (a scoreboard line or a tablist slot). Mirrors the rest of the codebase's
 * extension-point shape: one required method plus mostly-default no-op hooks over the reactivity surface.
 */
public interface TabElement {

    String render(TabContext context);

    /** Context keys this element depends on. When any of these change on the owning view's {@link TabContext}, this element alone re-renders. */
    default Set<String> dependencies() {
        return Set.of();
    }

    /** Ticks between automatic re-renders, independent of context changes. {@code <= 0} disables periodic refresh. */
    default int refreshTicks() {
        return 0;
    }

    static TabElement of(Function<TabContext, String> renderer) {
        return new FunctionElement(renderer, Set.of(), 0);
    }

    static TabElement of(Function<TabContext, String> renderer, String... dependencies) {
        return new FunctionElement(renderer, Set.of(dependencies), 0);
    }

    static TabElement ticking(int everyTicks, Function<TabContext, String> renderer) {
        return new FunctionElement(renderer, Set.of(), everyTicks);
    }

    static TabElement constant(String text) {
        return new FunctionElement(context -> text, Set.of(), 0);
    }
}
