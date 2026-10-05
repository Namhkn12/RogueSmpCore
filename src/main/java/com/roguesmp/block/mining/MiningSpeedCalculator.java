package com.roguesmp.block.mining;

import com.roguesmp.attribute.Attributes;
import com.roguesmp.block.BlockProperties;
import com.roguesmp.item.ItemType;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.registry.Holder;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Set;

/**
 * Custom-block mining speed, following Hypixel
 * SkyBlock's Mining Speed/Block Strength formula:
 * {@code ticks = round(hardness * 30 / miningSpeed)}, floored at 4 ticks, with a full bypass
 * (instant break) once {@code miningSpeed > 30 * hardness}.
 */
public final class MiningSpeedCalculator {

    public static final int CANNOT_BREAK = -1;

    private static final float BASE_MINING_SPEED = 1f;
    private static final float BOOST_PER_LEVEL = 20f;
    // Index 0 = Mining Fatigue level I.
    private static final float[] FATIGUE_PENALTY = {20f, 60f, 200f, 1000f};
    private static final float INSTANT_MINE_RATIO = 30f;
    private static final int MIN_TICKS = 4;

    private MiningSpeedCalculator() {}

    /**
     * Ticks to break a block of this hardness at this mining speed, or 0 for an instant (no delay)
     * break past {@link #INSTANT_MINE_RATIO} times the hardness.
     */
    public static int ticksToBreak(float miningSpeed, int hardness) {
        if (hardness <= 0) return 0;
        if (miningSpeed <= 0f) return CANNOT_BREAK;
        if (miningSpeed > INSTANT_MINE_RATIO * hardness) return 0;

        long ticks = Math.round(INSTANT_MINE_RATIO * hardness / miningSpeed);
        return (int) Math.max(MIN_TICKS, ticks);
    }

    public static float totalMiningSpeed(SmpPlayer smpPlayer, Player player) {
        double gearSpeed = smpPlayer.getActiveAttributes().getOrDefault(Attributes.MINING_SPEED, 0d);
        float total = BASE_MINING_SPEED + (float) gearSpeed;

        int boostLevel = Math.max(effectLevel(player, PotionEffectType.HASTE), effectLevel(player, PotionEffectType.CONDUIT_POWER));
        total += boostLevel * BOOST_PER_LEVEL;

        int fatigueLevel = effectLevel(player, PotionEffectType.MINING_FATIGUE);
        if (fatigueLevel > 0) {
            int index = Math.min(fatigueLevel, FATIGUE_PENALTY.length) - 1;
            total -= FATIGUE_PENALTY[index];
        }

        return Math.max(0f, total);
    }

    /**
     * Ticks to break {@code block} right now, {@code 0} for instant, or {@link #CANNOT_BREAK} if the
     * player is missing a required tool or doesn't meet the block's break strength.
     */
    public static int ticksToBreak(Player player, BlockProperties block) {
        if (!hasRequiredTool(player, block)) return CANNOT_BREAK;

        SmpPlayer smpPlayer = PlayerManager.getInstance().getSmpPlayer(player);
        if (smpPlayer == null) return CANNOT_BREAK;

        if (breakStrength(smpPlayer) < block.breakStrength()) return CANNOT_BREAK;

        return ticksToBreak(totalMiningSpeed(smpPlayer, player), block.hardness());
    }

    private static boolean hasRequiredTool(Player player, BlockProperties block) {
        Set<Holder<ItemType>> required = block.tools();
        if (required.isEmpty()) return true;

        ItemStack tool = player.getInventory().getItemInMainHand();
        for (Holder<ItemType> type : required) {
            if (type.isBound() && type.value().matches(tool)) return true;
        }
        return false;
    }

    private static double breakStrength(SmpPlayer smpPlayer) {
        return smpPlayer.getActiveAttributes().getOrDefault(Attributes.BREAK_STRENGTH, 0d);
    }

    private static int effectLevel(Player player, PotionEffectType type) {
        PotionEffect effect = player.getPotionEffect(type);
        return effect == null ? 0 : effect.getAmplifier() + 1;
    }
}
