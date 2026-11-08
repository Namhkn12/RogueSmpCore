package com.roguesmp.block.upgrade.effect;

import com.roguesmp.block.StoredItem;
import com.roguesmp.codec.Codec;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record ConvertDropsBehavior(Map<String, String> conversions) implements UpgradeBehavior {

    public static final String TYPE_KEY = "convert_drops";

    public static final Codec<ConvertDropsBehavior> CODEC = Codec.composite(
            Codec.unboundedMap(Codec.STRING, Codec.STRING).fieldOf("conversions").forGetter(ConvertDropsBehavior::conversions),
            ConvertDropsBehavior::new
    );

    @Override
    public String getTypeId() {
        return TYPE_KEY;
    }

    @Override
    public List<Component> getDisplay() {
        List<Component> lines = new ArrayList<>();
        conversions.forEach((from, to) -> lines.add(Utils.text("Chuyển đổi: ", NamedTextColor.GOLD)
                .append(new StoredItem(from, 1).displayName())
                .append(Component.text(" → ", NamedTextColor.GOLD))
                .append(new StoredItem(to, 1).displayName())));
        return lines;
    }

    @Override
    public void onOutput(OutputContext context) {
        context.outputs().replaceAll(drop -> {
            String converted = conversions.get(drop.item());
            return converted == null ? drop : new StoredItem(converted, drop.amount());
        });
    }
}
