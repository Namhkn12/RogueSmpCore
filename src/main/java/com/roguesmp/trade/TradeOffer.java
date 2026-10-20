package com.roguesmp.trade;

import com.roguesmp.codec.Codec;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.gate.cost.Cost;
import com.roguesmp.gate.requirement.Requirement;
import com.roguesmp.trade.result.TradeResult;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class TradeOffer {

    public static final Codec<TradeOffer> CODEC = Codec.composite(
            Codec.STRING.optionalFieldOf("name", () -> null).forGetter(TradeOffer::getName),
            Codec.listOf(Cost.CODEC).optionalFieldOf("costs", new ArrayList<>()).forGetter(TradeOffer::getCosts),
            Codec.listOf(Requirement.CODEC).optionalFieldOf("requirements", new ArrayList<>()).forGetter(TradeOffer::getRequirements),
            Codec.listOf(TradeResult.CODEC).fieldOf("results").forGetter(TradeOffer::getResults),
            TradeOffer::new
    );

    private final @Nullable String name;
    private final List<Cost> costs;
    private final List<Requirement> requirements;
    private final List<TradeResult> results;

    public TradeOffer(@Nullable String name, List<Cost> costs, List<Requirement> requirements, List<TradeResult> results) {
        this.name = name;
        this.costs = costs;
        this.requirements = requirements;
        this.results = results;
    }

    public boolean meetsRequirements(SmpPlayer player) {
        return requirements.stream().allMatch(requirement -> requirement.test(player));
    }

    public boolean canAfford(SmpPlayer player) {
        return costs.stream().allMatch(cost -> cost.canAfford(player));
    }

    public Status trade(SmpPlayer player) {
        Player bukkitPlayer = player.getBukkitPlayer();
        if (bukkitPlayer == null || !meetsRequirements(player) || !canAfford(player)) return Status.NOT_ELIGIBLE;

        PlayerInventory inventory = bukkitPlayer.getInventory();
        ItemStack[] snapshot = inventory.getStorageContents();
        costs.forEach(cost -> cost.takeFrom(inventory));
        ItemStack[] items = results.stream()
                .flatMap(result -> result.createItems(player).stream())
                .toArray(ItemStack[]::new);
        if (items.length > 0 && !inventory.addItem(items).isEmpty()) {
            inventory.setStorageContents(snapshot);
            return Status.INVENTORY_FULL;
        }

        costs.forEach(cost -> cost.take(player));
        results.forEach(result -> result.give(player));
        return Status.SUCCESS;
    }

    public @Nullable String getName() {
        return name;
    }

    public List<Cost> getCosts() {
        return costs;
    }

    public List<Requirement> getRequirements() {
        return requirements;
    }

    public List<TradeResult> getResults() {
        return results;
    }

    public enum Status {
        SUCCESS,
        NOT_ELIGIBLE,
        INVENTORY_FULL
    }
}
