package com.roguesmp.tab;

import com.roguesmp.tab.element.TabElement;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Ties one {@link TabElement} to wherever it is displayed. {@code push} writes the element's rendered text
 * to that place. It runs when a dependency key changes on the {@link TabContext}, on the element's refresh
 * interval, and whenever {@link #refresh()} is called.
 */
public final class ElementBinding {

    private final TabContext context;
    private final Consumer<ElementBinding> push;
    private final List<Runnable> unbindTasks = new ArrayList<>();
    private TabElement element;
    private int ticksSinceRefresh;

    public ElementBinding(TabContext context, TabElement element, Consumer<ElementBinding> push) {
        this.context = context;
        this.element = element;
        this.push = push;
        bind();
    }

    public TabElement element() {
        return element;
    }

    public String render() {
        return element.render(context);
    }

    public void replace(TabElement newElement) {
        unbind();
        this.element = newElement;
        bind();
    }

    public void refresh() {
        push.accept(this);
        ticksSinceRefresh = 0;
    }

    public void tick() {
        int interval = element.refreshTicks();
        if (interval <= 0) return;
        if (++ticksSinceRefresh >= interval) refresh();
    }

    public void unbind() {
        unbindTasks.forEach(Runnable::run);
        unbindTasks.clear();
    }

    private void bind() {
        for (String dependency : element.dependencies()) {
            Consumer<TabContext> listener = ctx -> refresh();
            context.onChange(dependency, listener);
            unbindTasks.add(() -> context.removeListener(dependency, listener));
        }
    }
}
