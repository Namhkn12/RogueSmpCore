package com.roguesmp.tab.element;

import com.roguesmp.tab.TabContext;

import java.util.Collections;
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
    private final Set<String> cachedDependencies;
    private final int cachedRefreshTicks;

    JoinedElement(String separator, List<TabElement> parts) {
        this.separator = separator;
        this.parts = List.copyOf(parts);

        // Pre-compute dependencies
        Set<String> deps = new HashSet<>();
        for (TabElement part : this.parts) {
            deps.addAll(part.dependencies());
        }
        this.cachedDependencies = Collections.unmodifiableSet(deps);

        // Pre-compute tightest refresh interval
        int tightest = 0;
        for (TabElement part : this.parts) {
            int ticks = part.refreshTicks();
            if (ticks > 0 && (tightest == 0 || ticks < tightest)) {
                tightest = ticks;
            }
        }
        this.cachedRefreshTicks = tightest;
    }

    @Override
    public String render(TabContext context) {
        if (parts.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(parts.getFirst().render(context));
        for (int i = 1; i < parts.size(); i++) {
            sb.append(separator).append(parts.get(i).render(context));
        }
        return sb.toString();
    }

    @Override
    public Set<String> dependencies() {
        return cachedDependencies;
    }

    @Override
    public int refreshTicks() {
        return cachedRefreshTicks;
    }
}
