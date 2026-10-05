package com.roguesmp.block.impl.generator.module;

public enum GeneratorStat {
    PLACE_DELAY("Thời gian đặt", false),
    BREAK_DELAY("Thời gian khai thác", false),
    ENERGY_PER_TICK("Tiêu thụ năng lượng", false),
    MAX_ENERGY("Năng lượng tối đa", true),
    LOOT_CAPACITY("Sức chứa", true),
    DROP_AMOUNT("Sản lượng", true),
    RARE_DROP_CHANCE("Tỉ lệ vật phẩm hiếm", true),
    COMMON_DROP_AMOUNT("Sản lượng vật phẩm thường", true);

    private final String label;
    private final boolean higherIsBetter;

    GeneratorStat(String label, boolean higherIsBetter) {
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
