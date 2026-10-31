package com.roguesmp.quest.board;

import com.roguesmp.codec.Codec;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;

public record AdventureRank(String name, String icon, String model) {

    public static final AdventureRank NONE = new AdventureRank("", "", "");

    public static final Codec<AdventureRank> CODEC = Codec.composite(
            Codec.STRING.optionalFieldOf("name", "Unranked").forGetter(AdventureRank::name),
            Codec.STRING.optionalFieldOf("icon", "").forGetter(AdventureRank::icon),
            Codec.STRING.optionalFieldOf("model", "").forGetter(AdventureRank::model),
            AdventureRank::new
    );

    public Component getDisplayIcon() {
        return Utils.fromString(icon);
    }
}
