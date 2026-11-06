package com.roguesmp.block.upgrade;

import com.roguesmp.block.BlockType;

import java.util.List;

public interface UpgradeHost {

    BlockType<?, ?> getType();

    String getId();

    UpgradeSet upgrades();

    List<String> upgradeItems();

    void onUpgradesChanged();
}
