package com.roguesmp.block.impl.generator.module;

public enum GeneratorStat {
    PLACE_DELAY("Thời gian đặt", false, " tick", 1),
    BREAK_DELAY("Thời gian khai thác", false, " tick", 1),
    ENERGY_PER_TICK("Tiêu thụ năng lượng", false, "/tick", 1),
    MAX_ENERGY("Năng lượng tối đa", true, "", 1),
    LOOT_CAPACITY("Sức chứa", true, "", 1),
    DROP_AMOUNT("Sản lượng", true, "", 1),
    RARE_DROP_CHANCE("Tỉ lệ vật phẩm hiếm", true, "%", 100),
    COMMON_DROP_AMOUNT("Sản lượng vật phẩm thường", true, "", 1);

    private final String label;
    private final boolean higherIsBetter;
    private final String flatSuffix;
    private final double flatDisplayScale;

    GeneratorStat(String label, boolean higherIsBetter, String flatSuffix, double flatDisplayScale) {
        this.label = label;
        this.higherIsBetter = higherIsBetter;
        this.flatSuffix = flatSuffix;
        this.flatDisplayScale = flatDisplayScale;
    }

    public String label() {
        return label;
    }

    public boolean higherIsBetter() {
        return higherIsBetter;
    }

    public String flatSuffix() {
        return flatSuffix;
    }

    public double flatDisplayScale() {
        return flatDisplayScale;
    }
}
