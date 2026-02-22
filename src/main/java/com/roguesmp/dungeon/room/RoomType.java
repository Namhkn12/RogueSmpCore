package com.roguesmp.dungeon.room;

public enum RoomType {
    START("start", "Phòng bắt đầu của dungeon"),
    END("end", "Phòng kết thúc, nơi nhận thưởng hoặc thoát dungeon"),
    MONSTER("monster", "Phòng quái cơ bản, chứa mob thường"),
    ELITE("elite", "Phòng quái Elite mạnh hơn bình thường"),
    TREASURE("treasure", "Phòng kho báu, chứa chest hoặc loot hiếm"),
    PUZZLE("puzzle", "Phòng giải đố, cần tương tác để mở tiếp"),
    SHOP("shop", "Phòng cửa hàng, có NPC bán đồ"),
    EMPTY("empty", "Phòng trống, không có gì đặc biệt"),
    EVENT("event", "Phòng sự kiện random như buff/debuff, mini challenge");

    private final String jsonKey;
    private final String description;

    RoomType(String jsonKey, String description) {
        this.jsonKey = jsonKey;
        this.description = description;
    }

    public String getJsonKey() {
        return jsonKey;
    }

    public String getDescription() {
        return description;
    }

    public static RoomType fromJsonKey(String key) {
        for (RoomType type : values()) {
            if (type.jsonKey.equalsIgnoreCase(key)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown RoomType jsonKey: " + key);
    }
}
