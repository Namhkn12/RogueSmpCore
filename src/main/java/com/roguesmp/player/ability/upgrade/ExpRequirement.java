package com.roguesmp.player.ability.upgrade;

import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.PlayerUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.Objects;

public final class ExpRequirement implements UpgradeRequirement {
    private final int level;
    private final int expCost;

    public ExpRequirement(int level) {
        this.level = level;
        expCost = PlayerUtils.getExpFromLevel(level);
    }

    @Override
    public boolean canFulfill(SmpPlayer player) {
        Player bukkitPlayer = player.getBukkitPlayer();
        if (bukkitPlayer == null) return false;
        return PlayerUtils.getExp(bukkitPlayer) >= expCost;
    }

    @Override
    public void consume(SmpPlayer player) {
        Player bukkitPlayer = player.getBukkitPlayer();
        if (bukkitPlayer == null) return;
        PlayerUtils.changeExp(bukkitPlayer, expCost);
    }

    @Override
    public Component getDisplay(SmpPlayer player) {
        return Component.text("- " + level + " Exp level", NamedTextColor.GOLD);
    }

    public int getLevel() {
        return level;
    }

}
