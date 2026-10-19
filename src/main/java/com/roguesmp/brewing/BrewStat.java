package com.roguesmp.brewing;

import com.roguesmp.attribute.Attributes;
import com.roguesmp.codec.Codec;
import com.roguesmp.codec.DataResult;
import com.roguesmp.effect.SmpEffect;
import io.papermc.paper.persistence.PersistentDataContainerView;
import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.Locale;
import java.util.function.BiFunction;

/**
 * Something a brew ingredient can contribute to: either a player attribute, or an {@link SmpEffect}
 * built from the summed value. Ids are listed in {@link BrewStats}.
 */
public sealed interface BrewStat {

    Codec<BrewStat> CODEC = Codec.STRING.comapFlatMap(
            id -> {
                BrewStat stat = BrewStats.get(id.toLowerCase(Locale.ROOT));
                return stat != null ? DataResult.success(stat) : DataResult.error("Unknown brew stat '" + id + "'");
            },
            BrewStat::id
    );

    String id();

    List<Component> display(double value, int durationTicks, PersistentDataContainerView pdc);

    record AttributeStat(Attributes attribute) implements BrewStat {

        @Override
        public String id() {
            return attribute.name().toLowerCase(Locale.ROOT);
        }

        @Override
        public List<Component> display(double value, int durationTicks, PersistentDataContainerView pdc) {
            List<Component> lines = attribute.getAttribute().getDisplayText(value, null, pdc);
            return lines == null ? List.of() : lines;
        }
    }

    record EffectStat(String id, BiFunction<Double, Integer, SmpEffect> factory) implements BrewStat {

        public SmpEffect create(double value, int durationTicks) {
            return factory.apply(value, durationTicks);
        }

        @Override
        public List<Component> display(double value, int durationTicks, PersistentDataContainerView pdc) {
            Component line = create(value, durationTicks).getDisplayComponent();
            return line == null ? List.of() : List.of(line);
        }
    }
}
