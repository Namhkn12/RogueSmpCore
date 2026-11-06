package com.roguesmp.block.upgrade.stat;

import com.roguesmp.codec.Codec;

public enum BlockStat {
    ENERGY_PER_TICK("Tiêu thụ năng lượng", false),
    MAX_ENERGY("Năng lượng tối đa", true),

    GENERATOR_PLACE_DELAY("Thời gian đặt", false),
    GENERATOR_BREAK_DELAY("Thời gian khai thác", false),
    GENERATOR_LOOT_CAPACITY("Sức chứa", true),
    GENERATOR_DROP_AMOUNT("Sản lượng", true),
    GENERATOR_RARE_DROP_CHANCE("Tỉ lệ vật phẩm hiếm", true),
    GENERATOR_COMMON_DROP_AMOUNT("Sản lượng vật phẩm thường", true);

    public static final Codec<BlockStat> CODEC = Codec.enumOf(BlockStat.class);

    private final String label;
    private final boolean higherIsBetter;

    BlockStat(String label, boolean higherIsBetter) {
        this.label = label;
        this.higherIsBetter = higherIsBetter;
    }

    public String label() {
        return label;
    }

    public boolean higherIsBetter() {
        return higherIsBetter;
    }
}
