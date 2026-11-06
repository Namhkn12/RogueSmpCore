package com.roguesmp.block.upgrade;

import com.roguesmp.block.BlockType;

import com.roguesmp.block.upgrade.stat.BlockStat;

import java.util.List;
import java.util.Set;

public interface UpgradeHost {

    BlockType<?, ?> getType();

    String getId();

    UpgradeSet upgrades();

    List<String> upgradeItems();

    Set<BlockStat> stats();

    void onUpgradesChanged();
}
