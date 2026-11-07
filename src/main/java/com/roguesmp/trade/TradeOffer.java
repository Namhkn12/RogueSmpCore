package com.roguesmp.trade;

import com.roguesmp.gate.cost.CostPayment;
import com.roguesmp.codec.Codec;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.gate.cost.Cost;
import com.roguesmp.gate.requirement.Requirement;
import com.roguesmp.trade.result.TradeResult;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class TradeOffer {

    public static final Codec<TradeOffer> CODEC = Codec.composite(
            Codec.STRING.optionalFieldOf("name", () -> null).forGetter(TradeOffer::getName),
            Codec.listOf(Codec.STRING).optionalFieldOf("description", () -> null).forGetter(TradeOffer::getDescription),
            Codec.listOf(Cost.CODEC).optionalFieldOf("costs", new ArrayList<>()).forGetter(TradeOffer::getCosts),
            Codec.listOf(Requirement.CODEC).optionalFieldOf("requirements", new ArrayList<>()).forGetter(TradeOffer::getRequirements),
            Codec.listOf(TradeResult.CODEC).fieldOf("results").forGetter(TradeOffer::getResults),
            TradeOffer::new
    );

    private final @Nullable String name;
    private final @Nullable List<String> description;
    private final List<Cost> costs;
    private final List<Requirement> requirements;
    private final List<TradeResult> results;

    public TradeOffer(@Nullable String name, @Nullable List<String> description, List<Cost> costs, List<Requirement> requirements, List<TradeResult> results) {
        this.name = name;
        this.description = description;
        this.costs = costs;
        this.requirements = requirements;
        this.results = results;
    }

    public boolean meetsRequirements(SmpPlayer player) {
        return requirements.stream().allMatch(requirement -> requirement.test(player));
    }

    public boolean canAfford(SmpPlayer player) {
        return CostPayment.canAfford(player, costs);
    }

    public Status trade(SmpPlayer player) {
        Player bukkitPlayer = player.getBukkitPlayer();
        if (bukkitPlayer == null || !meetsRequirements(player) || !canAfford(player)) return Status.NOT_ELIGIBLE;

        ItemStack[] items = results.stream()
                .flatMap(result -> result.createItems(player).stream())
                .toArray(ItemStack[]::new);
        if (slotsNeeded(items) > emptySlots(bukkitPlayer)) return Status.INVENTORY_FULL;
        if (!CostPayment.pay(player, costs)) return Status.NOT_ELIGIBLE;

        bukkitPlayer.getInventory().addItem(items);
        results.forEach(result -> result.give(player));
        return Status.SUCCESS;
    }

    private static int slotsNeeded(ItemStack[] items) {
        int slots = 0;
        for (ItemStack item : items) slots += Math.ceilDiv(item.getAmount(), item.getMaxStackSize());
        return slots;
    }

    private static int emptySlots(Player player) {
        int empty = 0;
        for (ItemStack stack : player.getInventory().getStorageContents()) {
            if (stack == null || stack.isEmpty()) empty++;
        }
        return empty;
    }

    public @Nullable String getName() {
        return name;
    }

    public @Nullable List<String> getDescription() {
        return description;
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
