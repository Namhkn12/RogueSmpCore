package com.roguesmp.tab.element;

import com.roguesmp.tab.TabContext;

import java.util.Set;
import java.util.function.Function;

/** A {@link TabElement} backed by a plain function. Produced by {@link TabElement#of}, {@link TabElement#ticking}, and {@link TabElement#constant}. */
final class FunctionElement implements TabElement {

    private final Function<TabContext, String> renderer;
    private final Set<String> dependencies;
    private final int refreshTicks;

    FunctionElement(Function<TabContext, String> renderer, Set<String> dependencies, int refreshTicks) {
        this.renderer = renderer;
        this.dependencies = dependencies;
        this.refreshTicks = refreshTicks;
    }

    @Override
    public String render(TabContext context) {
        return renderer.apply(context);
    }

    @Override
    public Set<String> dependencies() {
        return dependencies;
    }

    @Override
    public int refreshTicks() {
        return refreshTicks;
    }
}
