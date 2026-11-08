package com.roguesmp.enchant.infusion;

import com.roguesmp.codec.Codec;
import com.roguesmp.gate.cost.Cost;

import java.util.List;

public record InfusionLevel(List<Cost> costs, double successChance) {

    public static final Codec<InfusionLevel> CODEC = Codec.composite(
            Codec.listOf(Cost.CODEC).optionalFieldOf("costs", List.of()).forGetter(InfusionLevel::costs),
            Codec.DOUBLE.optionalFieldOf("success_chance", 1.0).forGetter(InfusionLevel::successChance),
            InfusionLevel::new
    );
}
