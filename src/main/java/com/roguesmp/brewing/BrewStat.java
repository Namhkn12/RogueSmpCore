package com.roguesmp.brewing;

import com.roguesmp.attribute.Attributes;
import com.roguesmp.codec.Codec;
import com.roguesmp.codec.DataResult;
import com.roguesmp.effect.SmpEffect;
import com.roguesmp.effect.impl.VanillaPotionEffect;
import com.roguesmp.utils.Utils;
import io.papermc.paper.persistence.PersistentDataContainerView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionEffectTypeCategory;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
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

    PersistentDataContainerView NO_ITEM_DATA = ItemStack.of(Material.PAPER).getPersistentDataContainer();

    static Component indent(Component line) {
        return Component.space().append(line).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    String id();

    List<Component> display(double value, int durationTicks);

    /**
     * Whether a summed value is strong enough to end up on the potion.
     */
    default boolean isActive(double value) {
        return !Utils.isEffectiveZero(value);
    }

    default @Nullable SmpEffect createEffect(double value, int durationTicks) {
        return null;
    }

    /**
     * An ingredient's contribution range, e.g. {@code +2% ~ +4% Melee Damage} - built from
     * {@link #display} by default, sharing the text after the number.
     */
    default List<Component> displayRange(double min, double max) {
        List<Component> minLines = display(min, 0);
        if (min == max) return minLines;

        List<Component> maxLines = display(max, 0);
        List<Component> lines = new ArrayList<>();
        for (int i = 0; i < Math.min(minLines.size(), maxLines.size()); i++) lines.add(rangeLine(minLines.get(i), maxLines.get(i)));
        return lines;
    }

    private static Component rangeLine(Component min, Component max) {
        PlainTextComponentSerializer plain = PlainTextComponentSerializer.plainText();
        String minText = plain.serialize(min);
        String maxText = plain.serialize(max);
        int minSplit = minText.indexOf(' ');
        int maxSplit = maxText.indexOf(' ');
        if (minSplit < 0 || maxSplit < 0 || !minText.substring(minSplit).equals(maxText.substring(maxSplit))) {
            return min.append(Utils.text(" ~ ", NamedTextColor.DARK_GRAY)).append(max);
        }
        return Component.text(minText.substring(0, minSplit) + " ~ " + maxText.substring(0, maxSplit) + maxText.substring(maxSplit), max.color());
    }

    record AttributeStat(Attributes attribute) implements BrewStat {

        @Override
        public String id() {
            return attribute.name().toLowerCase(Locale.ROOT);
        }

        @Override
        public List<Component> display(double value, int durationTicks) {
            List<Component> lines = attribute.getAttribute().getDisplayText(value, null, NO_ITEM_DATA);
            return lines == null ? List.of() : lines;
        }
    }

    record EffectStat(String id, BiFunction<Double, Integer, SmpEffect> factory) implements BrewStat {

        @Override
        public boolean isActive(double value) {
            return value > 0 && !Utils.isEffectiveZero(value);
        }

        @Override
        public SmpEffect createEffect(double value, int durationTicks) {
            return factory.apply(value, durationTicks);
        }

        @Override
        public List<Component> display(double value, int durationTicks) {
            Component line = createEffect(value, durationTicks).getDisplayComponent();
            return line == null ? List.of() : List.of(line);
        }
    }

    /**
     * A vanilla potion effect brewed from points: the summed points, rounded down, are the effect
     * level, so an effect only appears once it reaches 1 point.
     */
    record PotionStat(PotionEffectType type, int maxLevel) implements BrewStat {

        private static final double ROUNDING_SLACK = 1e-6;

        @Override
        public String id() {
            return type.key().value();
        }

        private int level(double value) {
            return Math.min(maxLevel, (int) Math.floor(value + ROUNDING_SLACK));
        }

        @Override
        public boolean isActive(double value) {
            return level(value) >= 1;
        }

        @Override
        public SmpEffect createEffect(double value, int durationTicks) {
            return new VanillaPotionEffect(durationTicks, type, Math.max(1, level(value)) - 1);
        }

        @Override
        public List<Component> display(double value, int durationTicks) {
            Component line = createEffect(value, durationTicks).getDisplayComponent();
            return line == null ? List.of() : List.of(line);
        }

        @Override
        public List<Component> displayRange(double min, double max) {
            String points = min == max ? Utils.formatDecimal(min) : Utils.formatDecimal(min) + " ~ " + Utils.formatDecimal(max);
            NamedTextColor color = type.getCategory() == PotionEffectTypeCategory.HARMFUL ? NamedTextColor.RED : NamedTextColor.AQUA;
            return List.of(Component.text("+" + points + " điểm ", color).append(Component.translatable(type.translationKey(), color)));
        }
    }
}
