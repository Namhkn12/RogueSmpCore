package com.roguesmp.brewing;

import com.roguesmp.codec.Codec;

import java.util.List;

/**
 * What a brewed potion is made from. Stats are re-derived from the current ingredient definitions
 * every time, so balance changes apply to already-brewed potions; {@code seed} keeps each
 * ingredient's rolls stable between evaluations.
 */
public record BrewRecipe(List<String> ingredients, long seed) {

    public static final Codec<BrewRecipe> CODEC = Codec.composite(
            Codec.listOf(Codec.STRING).fieldOf("ingredients").forGetter(BrewRecipe::ingredients),
            Codec.STRING.xmap(Long::parseLong, String::valueOf).fieldOf("seed").forGetter(BrewRecipe::seed),
            BrewRecipe::new
    );

    public BrewRecipe {
        ingredients = List.copyOf(ingredients);
    }
}
