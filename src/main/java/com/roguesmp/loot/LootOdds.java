package com.roguesmp.loot;

import java.util.List;

public record LootOdds(List<PoolOdds> pools) {

    public record PoolOdds(int index, boolean active, List<EntryOdds> entries) {}

    public record EntryOdds(LootEntry entry, double weight, double chance) {}
}
