package com.roguesmp.block.impl.generator.behavior;

import com.roguesmp.block.BlockDrop;
import com.roguesmp.block.StoredItem;
import com.roguesmp.block.impl.generator.ResourceGeneratorBlock;
import com.roguesmp.codec.Codec;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.List;

public record AddDropsBehavior(List<BlockDrop> drops) implements GeneratorBehavior {

    public static final String TYPE_KEY = "add_drops";

    public static final Codec<AddDropsBehavior> CODEC = Codec.composite(
            Codec.listOf(BlockDrop.CODEC).fieldOf("drops").forGetter(AddDropsBehavior::drops),
            AddDropsBehavior::new
    );

    @Override
    public String getTypeId() {
        return TYPE_KEY;
    }

    @Override
    public List<Component> getDisplay() {
        List<Component> lines = new ArrayList<>();
        for (BlockDrop drop : drops) {
            String amount = drop.minAmount() == drop.maxAmount() ? "×" + drop.minAmount() : "×" + drop.minAmount() + "-" + drop.maxAmount();
            String chance = drop.chance() < 1.0 ? " (" + Utils.formatDecimal(drop.chance() * 100) + "%)" : "";
            lines.add(Utils.text("Drop bổ sung: ", NamedTextColor.GOLD)
                    .append(new StoredItem(drop.item(), 1).displayName())
                    .append(Component.text(" " + amount + chance, NamedTextColor.GRAY)));
        }
        return lines;
    }

    @Override
    public void onHarvest(ResourceGeneratorBlock generator, List<StoredItem> harvested) {
        for (BlockDrop drop : drops) {
            StoredItem rolled = generator.stats().rollDrop(drop);
            if (rolled != null) harvested.add(rolled);
        }
    }
}
