package com.roguesmp.item.component.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.constant.Keys;
import com.roguesmp.context.ItemLoreContext;
import com.roguesmp.item.component.ItemComponent;
import com.roguesmp.item.component.UniqueTrackingComponent;
import com.roguesmp.utils.Utils;
import io.papermc.paper.persistence.PersistentDataContainerView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ObjectComponent;
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

    public static final Codec<RandomStatComponent> CODEC = Codec.unit(RandomStatComponent::new);

    private static final NamespacedKey QUALITY_KEY = Keys.of("quality");

    private double currentQuality = 0;
    private int currentMagicPower = -1;
    private boolean freshlyRolled = false;

    public RandomStatComponent() {
    }


    @Override
    public @NotNull ItemComponent copy() {
        return new RandomStatComponent();
    }

    @Override
    public void contributeLore(ItemLoreContext context) {
        Component line = Component.empty().decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
        if (context.player() == null) {
            if (currentMagicPower >= 0) {
                Component mpLine = Component.text("Ma lực: ", NamedTextColor.GRAY).append(Component.text(currentMagicPower, NamedTextColor.AQUA)).append(Component.text(" - ", NamedTextColor.DARK_GRAY));
                line = line.append(mpLine);
            }
        } else if (currentMagicPower >= 0) {
            Component mpLine = Component.text("Ma lực: ", NamedTextColor.GRAY).append(Component.text(currentMagicPower, NamedTextColor.AQUA)).append(Component.text(" - ", NamedTextColor.DARK_GRAY));
            line = line.append(mpLine);
        }

        if (context.player() == null) {
            line = line.append(Component.text("Chất lượng: ", NamedTextColor.GRAY))
                    .append(buildQualityText(-1d)); //-1 for a '???' preview
        } else {
            line = line.append(Component.text("Chất lượng: ", NamedTextColor.GRAY))
                    .append(buildQualityText(currentQuality));
        }
        context.builder().putLines(1, List.of(line));
    }

    @Override
    public void load(PersistentDataContainerView pdc) {
        Double currentQuality = pdc.get(QUALITY_KEY, PersistentDataType.DOUBLE);
        if (currentQuality == null) {
            this.currentQuality = Utils.RANDOM.nextDouble();
            this.freshlyRolled = true;
        } else {
            this.currentQuality = currentQuality;
            this.freshlyRolled = false;
        }
    }

    /**
     * True only right after a brand new quality was just rolled (no {@code QUALITY_KEY} was
     * persisted yet for this item) - the correct one-time signal for "this is this item's actual
     * first-ever load", since a genuinely fresh decode always recomputes it from persisted PDC
     * state. Call {@link #consumeFreshRoll()} once acted on, so re-running one-time setup logic
     * (e.g. a quality-based durability reduction) on a later {@code generateItemStack} call for
     * the same cached instance doesn't repeat it.
     */
    public boolean wasFreshlyRolled() {
        return freshlyRolled;
    }

    public void consumeFreshRoll() {
        freshlyRolled = false;
    }

    @Override
    public void save(PersistentDataContainer pdc) {
        pdc.set(QUALITY_KEY, PersistentDataType.DOUBLE, currentQuality);
    }

    private static Component buildQualityText(double quality) {
        if (quality < 0) {
            return Component.text("???", NamedTextColor.DARK_GRAY, TextDecoration.OBFUSCATED);
        }
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

    public double getCurrentQuality() {
        return currentQuality;
    }

    public void setCurrentMagicPower(int currentMagicPower) {
        this.currentMagicPower = currentMagicPower;
    }
}
