package com.roguesmp.tab.element;

import com.roguesmp.tab.TabContext;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Renders several elements as one, joined by a separator. Reacts to every dependency any of them declare,
 * and refreshes on a timer at whichever of their {@code refreshTicks()} is smallest. Produced by {@link TabElement#join}.
 */
final class JoinedElement implements TabElement {

    private final String separator;
    private final List<TabElement> parts;

    JoinedElement(String separator, List<TabElement> parts) {
        this.separator = separator;
        this.parts = parts;
    }

    @Override
    public String render(TabContext context) {
        return parts.stream().map(part -> part.render(context)).collect(Collectors.joining(separator));
    }

    @Override
    public Set<String> dependencies() {
        Set<String> dependencies = new HashSet<>();
        for (TabElement part : parts) {
            dependencies.addAll(part.dependencies());
        }
        return dependencies;
    }

    @Override
    public int refreshTicks() {
        int tightest = 0;
        for (TabElement part : parts) {
            int ticks = part.refreshTicks();
            if (ticks > 0 && (tightest == 0 || ticks < tightest)) {
                tightest = ticks;
            }
        }
        return tightest;
    }
}
