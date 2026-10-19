package com.roguesmp.item.component.impl;

import com.roguesmp.utils.ItemRef;
import com.roguesmp.codec.Codec;
import com.roguesmp.context.ItemLoreContext;
import com.roguesmp.item.component.ItemComponent;
import com.roguesmp.item.component.ItemComponentKeys;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.server.MinecraftServer;
import org.bukkit.Material;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record EnergySourceComponent(int energyPerTick, int burnTicks) implements ItemComponent {

    private static final int LORE_PRIORITY = 80;
    private static final int VANILLA_ENERGY_PER_TICK = 10;
    private static final int VANILLA_BURN_DIVISOR = 25;
    private static final Map<Material, Optional<EnergySourceComponent>> VANILLA_CACHE = new EnumMap<>(Material.class);

    public static final Codec<EnergySourceComponent> CODEC = Codec.composite(
            Codec.INT.fieldOf("energy_per_tick").forGetter(EnergySourceComponent::energyPerTick),
            Codec.INT.fieldOf("burn_ticks").forGetter(EnergySourceComponent::burnTicks),
            EnergySourceComponent::new
    );

    public static @Nullable EnergySourceComponent of(String itemId) {
        if (!ItemRef.isVanilla(itemId)) return ItemRef.component(itemId, ItemComponentKeys.ENERGY_SOURCE);

        Material material = ItemRef.vanillaMaterial(itemId);
        if (material == null || material == Material.LAVA_BUCKET || !material.isFuel()) return null;
        return VANILLA_CACHE.computeIfAbsent(material, EnergySourceComponent::fromVanillaFuel).orElse(null);
    }

    private static Optional<EnergySourceComponent> fromVanillaFuel(Material material) {
        int burnTime = MinecraftServer.getServer().fuelValues().burnDuration(CraftItemStack.asNMSCopy(ItemStack.of(material)));
        int burnTicks = burnTime / VANILLA_BURN_DIVISOR;
        return burnTicks > 0 ? Optional.of(new EnergySourceComponent(VANILLA_ENERGY_PER_TICK, burnTicks)) : Optional.empty();
    }

    public int totalEnergy() {
        return energyPerTick * burnTicks;
    }

    @Override
    public @NotNull ItemComponent copy() {
        return this;
    }

    @Override
    public void contributeLore(ItemLoreContext context) {
        context.builder().putLines(LORE_PRIORITY, List.of(
                Utils.text("Năng lượng: " + energyPerTick + "/tick trong " + Utils.formatDecimal(burnTicks / 20d) + "s", NamedTextColor.YELLOW),
                Utils.text("Tổng: " + Utils.formatMoney(totalEnergy()), NamedTextColor.GRAY)
        ));
    }
}
