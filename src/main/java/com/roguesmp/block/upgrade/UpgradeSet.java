package com.roguesmp.block.upgrade;

import com.roguesmp.block.StoredItem;
import com.roguesmp.block.upgrade.effect.OutputContext;
import com.roguesmp.block.upgrade.effect.UpgradeBehavior;
import com.roguesmp.block.upgrade.effect.UpgradeEffect;
import com.roguesmp.block.upgrade.stat.StatSheet;
import com.roguesmp.item.component.impl.BlockUpgradeComponent;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;

public final class UpgradeSet {

    private final UpgradeHost host;
    private StatSheet sheet = StatSheet.EMPTY;
    private List<UpgradeBehavior> behaviors = List.of();

    public UpgradeSet(UpgradeHost host) {
        this.host = host;
    }

    public void rebuild() {
        List<String> items = host.upgradeItems();
        List<UpgradeEffect> effects = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            BlockUpgradeComponent upgrade = BlockUpgradeComponent.of(items.get(i));
            if (upgrade == null) continue;

            effects.add(upgrade.effect());
            int self = i;
            upgrade.synergies().stream()
                    .filter(synergy -> hasOther(items, self, synergy))
                    .forEach(synergy -> effects.add(synergy.effect()));
        }

        sheet = new StatSheet(effects.stream().flatMap(effect -> effect.modifiers().stream()).toList());
        behaviors = effects.stream().flatMap(effect -> effect.behaviors().stream()).toList();
        host.onUpgradesChanged();
    }

    private static boolean hasOther(List<String> items, int self, UpgradeSynergy synergy) {
        for (int i = 0; i < items.size(); i++) {
            if (i != self && synergy.appliesTo(items.get(i))) return true;
        }
        return false;
    }

    public UpgradeInstallResult canAdd(String itemId) {
        BlockUpgradeComponent upgrade = BlockUpgradeComponent.of(itemId);
        if (upgrade == null) return UpgradeInstallResult.NOT_AN_UPGRADE;

        int duplicates = 0;
        for (String presentId : host.upgradeItems()) {
            if (upgrade.incompatible().contains(presentId)) return UpgradeInstallResult.INCOMPATIBLE;

            BlockUpgradeComponent other = BlockUpgradeComponent.of(presentId);
            if (other != null && other.incompatible().contains(itemId)) return UpgradeInstallResult.INCOMPATIBLE;
            if (presentId.equals(itemId)) duplicates++;
        }
        if (upgrade.maxPerBlock() > 0 && duplicates >= upgrade.maxPerBlock()) return UpgradeInstallResult.LIMIT_REACHED;
        return UpgradeInstallResult.INSTALLED;
    }

    public List<StoredItem> output(OutputContext context) {
        behaviors.forEach(behavior -> behavior.onOutput(context));
        return context.outputs();
    }

    public StatSheet sheet() {
        return sheet;
    }

    public List<Component> describe() {
        List<Component> lines = new ArrayList<>(sheet.describe());
        behaviors.forEach(behavior -> lines.addAll(behavior.getDisplay()));
        return lines;
    }
}
