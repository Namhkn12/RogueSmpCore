package com.roguesmp.gate.cost.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.gate.cost.Cost;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.PlayerUtils;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

public final class ExpCost implements Cost {

    public static final String TYPE_KEY = "exp";

    public static final Codec<ExpCost> CODEC = Codec.INT.fieldOf("level")
            .xmap(ExpCost::new, ExpCost::getLevel).codec();

    private final int level;
    private final int expCost;

    public ExpCost(int level) {
        this.level = level;
        expCost = PlayerUtils.getExpFromLevel(level);
    }

    @Override
    public String getTypeId() {
        return TYPE_KEY;
    }

    public int getLevel() {
        return level;
    }

    @Override
    public boolean canAfford(SmpPlayer player) {
        Player bukkitPlayer = player.getBukkitPlayer();
        return bukkitPlayer != null && PlayerUtils.getExp(bukkitPlayer) >= expCost;
    }

    @Override
    public void take(SmpPlayer player) {
        Player bukkitPlayer = player.getBukkitPlayer();
        if (bukkitPlayer == null) return;
        PlayerUtils.changeExp(bukkitPlayer, -expCost);
    }

    @Override
    public Component getDisplay(SmpPlayer player) {
        Player bukkitPlayer = player.getBukkitPlayer();
        if (bukkitPlayer == null) return Component.text(level + " Exp Level", NamedTextColor.GRAY);

        int currentTotalExp = PlayerUtils.getExp(bukkitPlayer);
        boolean hasEnough = currentTotalExp >= expCost;

        return Component.text(Utils.formatMoney(expCost) + " Exp", NamedTextColor.YELLOW)
                .append(Component.text(" (", NamedTextColor.GRAY))
                .append(Component.text(Utils.formatMoney(currentTotalExp), hasEnough ? NamedTextColor.GREEN : NamedTextColor.RED))
                .append(Component.text("/" + Utils.formatMoney(expCost) + ")", NamedTextColor.GRAY));
    }
}
