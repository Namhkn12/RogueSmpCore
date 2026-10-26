package com.roguesmp.quest.board;

import com.roguesmp.codec.Codec;
import com.roguesmp.codec.DataResult;
import com.roguesmp.quest.reward.QuestReward;

import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Exp needed to go from level {@code n} to {@code n + 1} is {@code baseExp + increment * (n - 1)}. {@code acceptLimits}
 * maps a level to the board quests that can be accepted at once from that level on. {@code rewards} are sent to the
 * player's inbox when they reach that level.
 */
public record QuestLevelCurve(long baseExp, long increment, int maxLevel, NavigableMap<Integer, Integer> acceptLimits, NavigableMap<Integer, List<QuestReward>> rewards) {

    private static final Codec<NavigableMap<Integer, Integer>> LIMITS_CODEC = Codec.unboundedMap(Codec.INT).comapFlatMap(raw -> {
        NavigableMap<Integer, Integer> limits = new TreeMap<>();
        for (Map.Entry<String, Integer> entry : raw.entrySet()) {
            try {
                limits.put(Integer.parseInt(entry.getKey()), Math.max(1, entry.getValue()));
            } catch (NumberFormatException e) {
                return DataResult.error("Accept limit key must be a level number: " + entry.getKey());
            }
        }
        return DataResult.success(limits);
    }, limits -> limits.entrySet().stream().collect(Collectors.toMap(entry -> String.valueOf(entry.getKey()), Map.Entry::getValue)));

    private static final Codec<NavigableMap<Integer, List<QuestReward>>> REWARDS_CODEC = Codec.unboundedMap(Codec.lenientListOf(QuestReward.CODEC)).comapFlatMap(raw -> {
        NavigableMap<Integer, List<QuestReward>> rewards = new TreeMap<>();
        for (Map.Entry<String, List<QuestReward>> entry : raw.entrySet()) {
            try {
                rewards.put(Integer.parseInt(entry.getKey()), List.copyOf(entry.getValue()));
            } catch (NumberFormatException e) {
                return DataResult.error("Reward key must be a level number: " + entry.getKey());
            }
        }
        return DataResult.success(rewards);
    }, rewards -> rewards.entrySet().stream().collect(Collectors.toMap(entry -> String.valueOf(entry.getKey()), Map.Entry::getValue)));

    public static final QuestLevelCurve DEFAULT = new QuestLevelCurve(100, 50, 50, new TreeMap<>(Map.of(1, 2, 5, 3, 10, 4, 20, 5)), new TreeMap<>());

    public static final Codec<QuestLevelCurve> CODEC = Codec.composite(
            Codec.LONG.optionalFieldOf("baseExp", DEFAULT.baseExp).forGetter(QuestLevelCurve::baseExp),
            Codec.LONG.optionalFieldOf("increment", DEFAULT.increment).forGetter(QuestLevelCurve::increment),
            Codec.INT.optionalFieldOf("maxLevel", DEFAULT.maxLevel).forGetter(QuestLevelCurve::maxLevel),
            LIMITS_CODEC.optionalFieldOf("acceptLimits", DEFAULT.acceptLimits).forGetter(QuestLevelCurve::acceptLimits),
            REWARDS_CODEC.optionalFieldOf("rewards", DEFAULT.rewards).forGetter(QuestLevelCurve::rewards),
            (baseExp, increment, maxLevel, limits, rewards) -> new QuestLevelCurve(Math.max(1, baseExp), Math.max(0, increment), Math.max(1, maxLevel), limits.isEmpty() ? DEFAULT.acceptLimits : limits, rewards)
    );

    public long expToNext(int level) {
        return baseExp + increment * (level - 1);
    }

    public int levelOf(long totalExp) {
        int level = 1;
        long remaining = totalExp;
        while (level < maxLevel && remaining >= expToNext(level)) {
            remaining -= expToNext(level);
            level++;
        }
        return level;
    }

    public long expIntoLevel(long totalExp) {
        int reached = levelOf(totalExp);
        long remaining = totalExp;
        for (int level = 1; level < reached; level++) remaining -= expToNext(level);
        return remaining;
    }

    public int acceptLimit(int level) {
        Map.Entry<Integer, Integer> entry = acceptLimits.floorEntry(level);
        return entry != null ? entry.getValue() : acceptLimits.firstEntry().getValue();
    }

    public List<QuestReward> rewardsAt(int level) {
        return rewards.getOrDefault(level, List.of());
    }
}
