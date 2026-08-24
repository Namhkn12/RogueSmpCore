package com.roguesmp.item.component.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.constant.Keys;
import com.roguesmp.context.ItemLoreContext;
import com.roguesmp.item.component.ItemComponent;
import com.roguesmp.item.component.UniqueTrackingComponent;
import com.roguesmp.utils.Utils;
import io.papermc.paper.persistence.PersistentDataContainerView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Control how base attribute scale, from -0.2 to 0 bonus
 */
public class RandomStatComponent implements ItemComponent, UniqueTrackingComponent {

    public static final Codec<RandomStatComponent> CODEC = Codec.BOOLEAN.xmap(RandomStatComponent::new, RandomStatComponent::hasRandomQuality);

    private static final NamespacedKey QUALITY_KEY = Keys.of("quality");
    private final boolean hasQuality;

    private double currentQuality = 0;
    private int currentMagicPower = 0;

    public RandomStatComponent(boolean hasQuality) {
        this.hasQuality = hasQuality;
    }


    @Override
    public @NotNull ItemComponent copy() {
        return new RandomStatComponent(hasQuality);
    }

    @Override
    public void contributeLore(ItemLoreContext context) {
        Component line = Component.empty().decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
        if (currentMagicPower >= 0) {
            Component mpLine = Component.text("Ma lực: ", NamedTextColor.GRAY).append(Component.text(currentMagicPower, NamedTextColor.AQUA)).append(Component.text(" - ", NamedTextColor.DARK_GRAY));
            line = line.append(mpLine);
        }

        if (hasQuality) {
            if (context.player() == null) {
                line = line.append(Component.text("Chất lượng: ", NamedTextColor.GRAY))
                        .append(buildQualityText(1d));
            } else {
                line = line.append(Component.text("Chất lượng: ", NamedTextColor.GRAY))
                        .append(buildQualityText(currentQuality));
            }

            context.builder().putLines(0, List.of(line));
        }


    }

    @Override
    public void load(PersistentDataContainerView pdc) {
        Double currentQuality = pdc.get(QUALITY_KEY, PersistentDataType.DOUBLE);
        if (currentQuality == null) this.currentQuality = Utils.RANDOM.nextDouble();
        else this.currentQuality = currentQuality;
    }

    @Override
    public void save(PersistentDataContainer pdc) {
        pdc.set(QUALITY_KEY, PersistentDataType.DOUBLE, currentQuality);
    }

    private static Component buildQualityText(double quality) {
        double percent = (int) Math.round(quality * 100);

        String qualityName;
        TextColor color;

        if (percent >= 100) {
            qualityName = "Hoàn Mỹ";
            color = NamedTextColor.LIGHT_PURPLE;
        } else if (percent >= 80) {
            qualityName = "Mới Cứng";
            color = NamedTextColor.AQUA;
        } else if (percent >= 60) {
            qualityName = "Tương Đối";
            color = NamedTextColor.GREEN;
        } else if (percent >= 40) {
            qualityName = "Khả Dụng";
            color = NamedTextColor.YELLOW;
        } else if (percent >= 20) {
            qualityName = "Hoen gỉ";
            color = NamedTextColor.RED;
        } else {
            qualityName = "Tàn tạ";
            color = NamedTextColor.DARK_RED;
        }

        return Component.text(qualityName, color)
                .append(Component.space())
                .append(Component.text("(", NamedTextColor.GRAY))
                .append(Component.text(Utils.formatDecimal(quality * 100) + "%", color))
                .append(Component.text(")", NamedTextColor.GRAY));
    }

    public boolean hasRandomQuality() {
        return hasQuality;
    }

    public double getCurrentQuality() {
        return currentQuality;
    }

    public void setCurrentMagicPower(int currentMagicPower) {
        this.currentMagicPower = currentMagicPower;
    }
}
