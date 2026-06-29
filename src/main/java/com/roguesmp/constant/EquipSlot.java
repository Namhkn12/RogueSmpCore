package com.roguesmp.constant;

import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public enum EquipSlot {
    MAINHAND(EquipmentSlot.HAND, "<!i><gray>Khi trang bị tay chính:", "Tay chính","mainhand"),
    OFFHAND(EquipmentSlot.OFF_HAND, "<!i><gray>Khi trang bị tay phụ:", "Tay phụ", "offhand"),
    HEAD(EquipmentSlot.HEAD, "<!i><gray>Khi trang bị ở đầu:", "Đầu", "head"),
    CHEST(EquipmentSlot.CHEST, "<!i><gray>Khi trang bị ở ngực:", "Ngực", "chest"),
    LEGS(EquipmentSlot.LEGS, "<!i><gray>Khi trang bị ở chân:", "Chân", "legs"),
    FEET(EquipmentSlot.FEET, "<!i><gray>Khi trang bị ở bàn chân:", "Bàn chân","feet"),
    PROJECTILE(null, "<!i><gray>Khi bắn:", "Đạn", "projectile");

    private static final Map<EquipmentSlot, EquipSlot> vanillaToSlot = new HashMap<>();

    private final EquipmentSlot vanillaSlot;
    private final String displayString;
    private final String simpleName;
    private final String id;

    EquipSlot(EquipmentSlot vanillaSlot, String displayString, String simpleName, String id) {
        this.vanillaSlot = vanillaSlot;
        this.displayString = displayString;
        this.simpleName = simpleName;
        this.id = id;
    }

    public static EquipSlot fromVanilla(EquipmentSlot slot) {
        return vanillaToSlot.get(slot);
    }

    public @Nullable EquipmentSlot getVanillaSlot() {
        return vanillaSlot;
    }

    public String getDisplayString() {
        return displayString;
    }

    public String getSimpleName() {
        return simpleName;
    }

    public String getId() {
        return id;
    }

    static {
        for (EquipSlot slot : EquipSlot.values()) {
            vanillaToSlot.put(slot.vanillaSlot, slot);
        }
    }
}
