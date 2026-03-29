package com.roguesmp.dungeon.loot;


import com.roguesmp.dungeon.context.LootContext;
import com.roguesmp.player.SmpPlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Built-in {@link LootRule} implementations.
 *
 * <p>Use these as-is or extend them for custom behavior.
 */
public final class LootRules {

    private LootRules() {}

    // -----------------------------------------------------------------------
    // FixedBonusRule
    // -----------------------------------------------------------------------

    /**
     * Always returns a fixed modifier value.
     * Useful for testing or guaranteeing a flat bonus.
     *
     * <pre>{@code new LootRules.Fixed(1.0) // always adds 1.0 to modifier}</pre>
     */
    public static class Fixed implements LootRule {
        private final double value;

        public Fixed(double value) {
            this.value = value;
        }

        @Override
        public double evaluate(LootContext context) {
            return value;
        }
    }

    // -----------------------------------------------------------------------
    // LootingEnchantRule
    // -----------------------------------------------------------------------

    /**
     * Contributes a modifier based on the Looting enchantment level on a weapon.
     *
     * <p>Default formula: {@code lootingLevel * multiplierPerLevel}
     * e.g. Looting III with multiplier 0.5 → contributes 1.5
     *
     * <pre>{@code
     * ItemStack weapon = player.getInventory().getItemInMainHand();
     * context.addRule(new LootRules.LootingEnchant(weapon, 0.5));
     * }</pre>
     */
    public static class LootingEnchant implements LootRule {
        private final @Nullable ItemStack weapon;
        private final double multiplierPerLevel;

        public LootingEnchant(@Nullable ItemStack weapon, double multiplierPerLevel) {
            this.weapon = weapon;
            this.multiplierPerLevel = multiplierPerLevel;
        }

        public LootingEnchant(@Nullable ItemStack weapon) {
            this(weapon, 0.5);
        }

        @Override
        public double evaluate(LootContext context) {
            if (weapon == null || weapon.getType().isAir()) return 0.0;
            int level = weapon.getEnchantmentLevel(Enchantment.LOOTING);
            return level * multiplierPerLevel;
        }
    }

    // -----------------------------------------------------------------------
    // PlayerLuckRule
    // -----------------------------------------------------------------------

    /**
     * Contributes a modifier based on the player's custom luck stat.
     *
     * <p>Reads a numeric stat from {@link SmpPlayer}. You supply the stat key
     * and a multiplier to convert the raw stat value into a roll modifier.
     *
     * <p>Implement the {@code StatReader} to bridge your player stat system:
     * <pre>{@code
     * new LootRules.PlayerLuck(player, p -> p.getStat("luck") * 0.01)
     * }</pre>
     */
    public static class PlayerLuck implements LootRule {
        private final @Nullable SmpPlayer player;
        private final @NotNull StatReader reader;

        public PlayerLuck(@Nullable SmpPlayer player, @NotNull StatReader reader) {
            this.player = player;
            this.reader = reader;
        }

        @Override
        public double evaluate(LootContext context) {
            if (player == null) return 0.0;
            return reader.read(player);
        }

        @FunctionalInterface
        public interface StatReader {
            double read(@NotNull SmpPlayer player);
        }
    }

    // -----------------------------------------------------------------------
    // DungeonTierRule
    // -----------------------------------------------------------------------

    /**
     * Contributes a flat modifier based on dungeon difficulty tier.
     *
     * <p>Pass a tier integer (1-based) and a modifier per tier:
     * <pre>{@code
     * new LootRules.DungeonTier(dungeon.getTier(), 0.25)
     * // Tier 3 dungeon → contributes 0.75
     * }</pre>
     */
    public static class DungeonTier implements LootRule {
        private final int tier;
        private final double modifierPerTier;

        public DungeonTier(int tier, double modifierPerTier) {
            this.tier = tier;
            this.modifierPerTier = modifierPerTier;
        }

        @Override
        public double evaluate(LootContext context) {
            return Math.max(0, tier) * modifierPerTier;
        }
    }

    public static class DungeonScoreRule implements LootRule {
        private final int score;
        private final double multiplierPerPoint;

        public DungeonScoreRule(int score, double multiplierPerPoint) {
            this.score = score;
            this.multiplierPerPoint = multiplierPerPoint;
        }

        @Override
        public double evaluate(LootContext context) {
            return Math.max(0, score) * multiplierPerPoint;
        }
    }

}
