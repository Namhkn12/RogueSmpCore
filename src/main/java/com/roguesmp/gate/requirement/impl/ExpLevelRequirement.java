package com.roguesmp.gate.requirement.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.gate.requirement.Requirement;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public class ExpLevelRequirement implements Requirement {

    public static final String TYPE_KEY = "exp_level";
    public static final Codec<ExpLevelRequirement> CODEC = Codec.INT.fieldOf("min")
            .xmap(ExpLevelRequirement::new, ExpLevelRequirement::getMin).codec();

    private final int min;

    public ExpLevelRequirement(int min) {
        this.min = min;
    }

    @Override
    public String getTypeId() {
        return TYPE_KEY;
    }

    public int getMin() {
        return min;
    }

    @Override
    public boolean test(SmpPlayer player) {
        Player bukkitPlayer = player.getBukkitPlayer();
        return bukkitPlayer != null && bukkitPlayer.getLevel() >= min;
    }

    @Override
    public Component getDisplay(SmpPlayer player) {
        return Utils.fromString("<gray>Cấp kinh nghiệm <white>" + min);
    }
}
