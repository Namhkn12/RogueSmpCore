package com.roguesmp.island;

import com.roguesmp.gate.GateInventoryUtils;
import com.roguesmp.gate.cost.Cost;
import com.roguesmp.gate.cost.CostPayment;
import com.roguesmp.gate.cost.impl.ItemCost;
import com.roguesmp.gate.cost.impl.MoneyCost;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.quest.QuestManager;
import com.roguesmp.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class IslandLevelService {

    public enum Result {
        SUCCESS,
        MAX_LEVEL,
        REQUIREMENTS_NOT_MET,
        CONTRIBUTIONS_MISSING,
        CANNOT_AFFORD,
        LEVEL_CHANGED
    }

    private static final String DIVIDER = "<dark_aqua><strikethrough>                                                  </strikethrough>";

    private final IslandDataManager dataManager;

    public IslandLevelService(IslandDataManager dataManager) {
        this.dataManager = dataManager;
    }

    public static Map<String, Integer> contributionGoals(IslandLevel level) {
        Map<String, Integer> goals = new LinkedHashMap<>();
        for (Cost cost : level.costs()) {
            if (cost instanceof ItemCost itemCost) goals.merge(itemCost.getItem(), itemCost.getAmount(), Integer::sum);
        }
        return goals;
    }

    public static long moneyGoal(IslandLevel level) {
        long goal = 0;
        for (Cost cost : level.costs()) {
            if (cost instanceof MoneyCost moneyCost) goal += moneyCost.getAmount();
        }
        return goal;
    }

    public static List<Cost> directCosts(IslandLevel level) {
        return level.costs().stream().filter(cost -> !(cost instanceof ItemCost) && !(cost instanceof MoneyCost)).toList();
    }

    public long contributeMoney(SmpPlayer player, IslandData island, int targetLevel, long maxAmount) {
        Map.Entry<Integer, IslandLevel> next = IslandLevel.next(island.getLevel());
        if (next == null || next.getKey() != targetLevel) return 0;

        long amount = Math.min(maxAmount, Math.min(moneyGoal(next.getValue()) - island.getMoneyContribution(), player.getPlayerData().getMoney()));
        if (amount <= 0 || !player.takeMoney(amount)) return 0;

        island.addMoneyContribution(amount);
        dataManager.saveAsync(island);
        return amount;
    }

    public int contribute(SmpPlayer player, IslandData island, int targetLevel, String itemId, int maxAmount) {
        Player bukkitPlayer = player.getBukkitPlayer();
        Map.Entry<Integer, IslandLevel> next = IslandLevel.next(island.getLevel());
        if (bukkitPlayer == null || next == null || next.getKey() != targetLevel) return 0;

        int goal = contributionGoals(next.getValue()).getOrDefault(itemId, 0);
        int amount = Math.min(maxAmount, Math.min(goal - island.getContribution(itemId), GateInventoryUtils.count(player, itemId)));
        if (amount <= 0) return 0;

        GateInventoryUtils.remove(bukkitPlayer.getInventory(), itemId, amount);
        island.addContribution(itemId, amount);
        dataManager.saveAsync(island);
        return amount;
    }

    public boolean isContributed(IslandData island, IslandLevel level) {
        return island.getMoneyContribution() >= moneyGoal(level)
                && contributionGoals(level).entrySet().stream().allMatch(goal -> island.getContribution(goal.getKey()) >= goal.getValue());
    }

    public Result check(SmpPlayer player, IslandData island) {
        Map.Entry<Integer, IslandLevel> next = IslandLevel.next(island.getLevel());
        if (next == null) return Result.MAX_LEVEL;
        IslandLevel level = next.getValue();
        if (!level.requirements().stream().allMatch(requirement -> requirement.test(player))) return Result.REQUIREMENTS_NOT_MET;
        if (!isContributed(island, level)) return Result.CONTRIBUTIONS_MISSING;
        if (!CostPayment.canAfford(player, directCosts(level))) return Result.CANNOT_AFFORD;
        return Result.SUCCESS;
    }

    public Result upgrade(SmpPlayer player, IslandData island, int targetLevel) {
        Player bukkitPlayer = player.getBukkitPlayer();
        if (bukkitPlayer == null) return Result.CANNOT_AFFORD;
        Map.Entry<Integer, IslandLevel> target = IslandLevel.next(island.getLevel());
        if (target == null || target.getKey() != targetLevel) return Result.LEVEL_CHANGED;

        Result result = check(player, island);
        if (result != Result.SUCCESS) return result;

        Map.Entry<Integer, IslandLevel> next = IslandLevel.next(island.getLevel());
        if (!CostPayment.pay(player, directCosts(next.getValue()))) return Result.CANNOT_AFFORD;

        IslandLevel previous = IslandLevel.of(island.getLevel());
        setLevel(island, next.getKey());
        announceUpgrade(island, bukkitPlayer.getName(), next.getKey(), previous, next.getValue());
        return Result.SUCCESS;
    }

    public void setLevel(IslandData island, int level) {
        island.setLevel(level);
        island.clearContributions();
        dataManager.saveAsync(island);
        QuestManager.getInstance().refresh(island.getMembers());
    }

    private void announceUpgrade(IslandData island, String upgrader, int level, IslandLevel previous, IslandLevel current) {
        List<String> lines = new ArrayList<>(List.of(
                DIVIDER,
                "",
                "  <gold><bold>⬆ ĐẢO ĐÃ LÊN CẤP " + level + "!</bold>",
                "  <gray>Nâng cấp bởi <white>" + upgrader,
                ""
        ));
        List<IslandLevel.Perk> from = previous.perks();
        List<IslandLevel.Perk> to = current.perks();
        for (int i = 0; i < to.size(); i++) {
            if (to.get(i).unlocked()) lines.add(compareLine(from.get(i), to.get(i)));
        }
        lines.add("");
        lines.add(DIVIDER);
        for (UUID memberId : island.getMembers()) {
            Player member = Bukkit.getPlayer(memberId);
            if (member == null) continue;
            lines.forEach(line -> member.sendMessage(Utils.fromString(line)));
            member.playSound(member.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 0.9f);
            member.playSound(member.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.5f, 1.2f);
        }
    }

    private static String compareLine(IslandLevel.Perk from, IslandLevel.Perk to) {
        String label = "<gray>" + to.label() + ": ";
        if (!from.unlocked()) return "   <gold>✦ " + label + "<green><bold>" + to.value() + "</bold> <gold>(Mới mở khoá!)";
        if (from.value().equals(to.value())) return "   <dark_gray>▪ " + label + "<white>" + to.value();
        return "   <green>▪ " + label + "<dark_gray>" + from.value() + " → <green><bold>" + to.value() + "</bold>";
    }
}
