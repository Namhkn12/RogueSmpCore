package com.roguesmp.player.ability.upgrade;

import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.PlayerUtils;
import com.roguesmp.utils.Utils;
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
        PlayerUtils.changeExp(bukkitPlayer, -expCost);
    }

    @Override
    public Component getDisplay(SmpPlayer player) {
        Player bukkitPlayer = player.getBukkitPlayer();
        if (bukkitPlayer == null) return Component.text("- " + level + " Exp Level", NamedTextColor.GRAY);

        int currentTotalExp = PlayerUtils.getExp(bukkitPlayer);
        int currentLevel = bukkitPlayer.getLevel();
        boolean hasEnough = currentTotalExp >= expCost;

        // Build: • 30 Cấp độ Exp (25/30)
        return Component.text(" • ", NamedTextColor.DARK_GRAY)
                .append(Component.text(level + " Cấp Exp", hasEnough ? NamedTextColor.GREEN : NamedTextColor.YELLOW))
                .append(Component.text(" (", NamedTextColor.GRAY))
                .append(Component.text(currentLevel, hasEnough ? NamedTextColor.GREEN : NamedTextColor.RED))
                .append(Component.text("/", NamedTextColor.GRAY))
                .append(Component.text(level, NamedTextColor.GRAY))
                .append(Component.text(")", NamedTextColor.GRAY));
    }

    public int getLevel() {
        return level;
    }

}
