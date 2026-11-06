package com.roguesmp.item.component.impl;

import com.roguesmp.block.BlockData;
import com.roguesmp.block.BlockType;
import com.roguesmp.block.StoredItem;
import com.roguesmp.block.upgrade.UpgradeHost;
import com.roguesmp.block.upgrade.UpgradeInstallResult;
import com.roguesmp.block.upgrade.UpgradeSynergy;
import com.roguesmp.block.upgrade.effect.UpgradeEffect;
import com.roguesmp.block.upgrade.DurationUnit;
import com.roguesmp.block.upgrade.UpgradeType;
import com.roguesmp.codec.Codec;
import com.roguesmp.context.ItemLoreContext;
import com.roguesmp.item.component.ItemComponent;
import com.roguesmp.item.component.ItemComponentKeys;
import com.roguesmp.registry.Holder;
import com.roguesmp.registry.Registries;
import com.roguesmp.registry.Registry;
import com.roguesmp.utils.ItemRef;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record BlockUpgradeComponent(
        UpgradeType type,
        List<Holder<BlockType<?, ?>>> blockTypes,
        List<Holder<BlockData>> blocks,
        UpgradeEffect effect,
        int duration,
        DurationUnit unit,
        List<String> incompatible,
        int maxPerBlock,
        List<UpgradeSynergy> synergies
) implements ItemComponent {

    private static final int LORE_PRIORITY = 80;

    public static final Codec<BlockUpgradeComponent> CODEC = Codec.composite(
            UpgradeType.CODEC.fieldOf("type").forGetter(BlockUpgradeComponent::type),
            Codec.listOrSingle(Registry.referenceCodec(() -> Registries.BLOCK_TYPE)).optionalFieldOf("block_types", List.of()).forGetter(BlockUpgradeComponent::blockTypes),
            Codec.listOrSingle(Registry.referenceCodec(() -> Registries.BLOCK)).optionalFieldOf("blocks", List.of()).forGetter(BlockUpgradeComponent::blocks),
            UpgradeEffect.CODEC.forGetter(BlockUpgradeComponent::effect),
            Codec.INT.optionalFieldOf("duration", 0).forGetter(BlockUpgradeComponent::duration),
            Codec.enumOf(DurationUnit.class).optionalFieldOf("unit", DurationUnit.TICKS).forGetter(BlockUpgradeComponent::unit),
            Codec.listOf(Codec.STRING).optionalFieldOf("incompatible", List.of()).forGetter(BlockUpgradeComponent::incompatible),
            Codec.INT.optionalFieldOf("max_per_block", 0).forGetter(BlockUpgradeComponent::maxPerBlock),
            Codec.listOf(UpgradeSynergy.CODEC).optionalFieldOf("synergies", List.of()).forGetter(BlockUpgradeComponent::synergies),
            BlockUpgradeComponent::new
    );

    public static @Nullable BlockUpgradeComponent of(String itemId) {
        return ItemRef.component(itemId, ItemComponentKeys.BLOCK_UPGRADE);
    }

    public static UpgradeInstallResult check(String itemId, UpgradeHost host, UpgradeType type) {
        BlockUpgradeComponent upgrade = of(itemId);
        if (upgrade == null) return UpgradeInstallResult.NOT_AN_UPGRADE;
        if (!upgrade.eligibleFor(host)) return UpgradeInstallResult.NOT_ELIGIBLE;
        if (upgrade.type() != type) return UpgradeInstallResult.WRONG_TYPE;
        return UpgradeInstallResult.INSTALLED;
    }

    public boolean eligibleFor(UpgradeHost host) {
        return blockTypes.stream().anyMatch(holder -> holder.isBound() && holder.value() == host.getType())
                || blocks.stream().anyMatch(holder -> holder.getId().equals(host.getId()));
    }

    @Override
    public @NotNull ItemComponent copy() {
        return this;
    }

    @Override
    public void contributeLore(ItemLoreContext context) {
        List<Component> lines = new ArrayList<>(effect.describe());
        synergies.forEach(synergy -> lines.addAll(synergy.describe()));
        if (duration > 0) lines.add(Utils.text("Kéo dài: " + unit.format(duration), NamedTextColor.GRAY));
        if (maxPerBlock > 0) lines.add(Utils.text("Tối đa mỗi máy: " + maxPerBlock, NamedTextColor.GRAY));
        if (!incompatible.isEmpty()) lines.add(incompatibleLine());
        lines.add(Utils.text("Dùng cho: ", NamedTextColor.DARK_AQUA).append(Utils.fromString(blockNames())));
        context.builder().putLines(LORE_PRIORITY, lines);
    }

    private String blockNames() {
        Stream<String> typeNames = blockTypes.stream().map(holder -> holder.isBound() ? holder.value().displayName() : holder.getId());
        Stream<String> blockNames = blocks.stream().map(holder -> holder.isBound() ? holder.value().properties().displayName() : holder.getId());
        return Stream.concat(typeNames, blockNames).collect(Collectors.joining("<dark_aqua>, "));
    }

    private Component incompatibleLine() {
        Component line = Utils.text("Xung đột: ", NamedTextColor.DARK_RED);
        for (int i = 0; i < incompatible.size(); i++) {
            if (i > 0) line = line.append(Component.text(", ", NamedTextColor.DARK_RED));
            line = line.append(new StoredItem(incompatible.get(i), 1).displayName());
        }
        return line;
    }
}
